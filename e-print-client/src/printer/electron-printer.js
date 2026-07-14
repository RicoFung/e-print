'use strict';

const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { BrowserWindow, ipcMain } = require('electron');

const PRINT_TEMP_DIR = path.join(os.tmpdir(), 'e-print-client');
const LAST_PRINT_FILE = path.join(PRINT_TEMP_DIR, 'last-print.html');
const APP_ICON_FILE = path.join(__dirname, '..', '..', 'assets', 'e-print-icon.png');
const PREVIEW_SHELL_FILE = path.join(__dirname, '..', 'renderer', 'print-preview.html');
const PREVIEW_PRELOAD_FILE = path.join(__dirname, 'print-preview-preload.js');
const PRINT_CANCELLED_CODE = 'PRINT_CANCELLED';
const PRINT_FAILED_CODE = 'PRINT_FAILED';

function createElectronPrinter(options) {
  const printerOptions = options || {};
  return {
    print(html, options) {
      const opts = options || {};
      if (opts.silent === false) {
        return previewAndPrintHtml(html, {
          ...opts,
          previewSettings: typeof printerOptions.getPreviewSettings === 'function'
            ? printerOptions.getPreviewSettings()
            : {}
        });
      }
      return printHtml(html, opts);
    }
  };
}

function printHtml(html, options) {
  return new Promise(async (resolve, reject) => {
    const opts = options || {};
    const win = new BrowserWindow({
      show: opts.silent === false,
      webPreferences: {
        sandbox: true,
        contextIsolation: true,
        nodeIntegration: false
      }
    });

    try {
      await writePrintHtml(html);
    } catch (error) {
      win.close();
      reject(error);
      return;
    }

    win.loadFile(LAST_PRINT_FILE)
      .then(() => waitForImages(win))
      .then(() => waitForPaint(win))
      .then(() => {
        win.webContents.print({
          silent: opts.silent !== false,
          printBackground: true,
          deviceName: opts.printerName || undefined,
          copies: opts.copies || 1
        }, (success, failureReason) => {
          win.close();
          if (success) {
            resolve();
          } else {
            reject(createPrintFailureError(failureReason));
          }
        });
      })
      .catch((error) => {
        win.close();
        reject(error);
      });
  });
}

function previewAndPrintHtml(html, options) {
  return new Promise(async (resolve, reject) => {
    const opts = options || {};
    const previewSettings = opts.previewSettings || {};
    const previewFile = createPreviewFilePath();
    let settled = false;
    let printing = false;

    const win = new BrowserWindow({
      width: 960,
      height: 720,
      minWidth: 680,
      minHeight: 520,
      title: 'E-PRINT Preview',
      icon: APP_ICON_FILE,
      backgroundColor: previewSettings.backgroundColor || '#f1f2f3',
      show: true,
      webPreferences: {
        preload: PREVIEW_PRELOAD_FILE,
        contextIsolation: true,
        nodeIntegration: false,
        sandbox: false
      }
    });

    const cleanup = () => {
      ipcMain.removeListener('print-preview:print', handlePrint);
      ipcMain.removeListener('print-preview:cancel', handleCancel);
      fs.unlink(previewFile).catch(() => {});
    };

    const finish = (callback) => {
      if (settled) {
        return;
      }
      settled = true;
      cleanup();
      if (!win.isDestroyed()) {
        win.close();
      }
      callback();
    };

    const sendState = (state) => {
      if (!win.isDestroyed()) {
        win.webContents.send('print-preview:state', state);
      }
    };

    async function handlePrint(event) {
      if (event.sender.id !== win.webContents.id || printing) {
        return;
      }

      printing = true;
      sendState({ state: 'printing' });
      try {
        await printHtml(html, {
          ...opts,
          silent: true
        });
        finish(resolve);
      } catch (error) {
        printing = false;
        sendState({
          state: 'failed',
          code: error && error.code ? error.code : PRINT_FAILED_CODE,
          message: error && error.message ? error.message : String(error)
        });
      }
    }

    function handleCancel(event) {
      if (event.sender.id !== win.webContents.id) {
        return;
      }
      finish(() => reject(createPrintFailureError('print cancelled')));
    }

    ipcMain.on('print-preview:print', handlePrint);
    ipcMain.on('print-preview:cancel', handleCancel);

    win.on('closed', () => {
      if (!settled) {
        settled = true;
        cleanup();
        reject(new Error('print preview closed'));
      }
    });

    try {
      await writePrintHtml(html, previewFile);
      await win.loadFile(PREVIEW_SHELL_FILE, {
        query: {
          src: pathToFileURL(previewFile).toString(),
          printerName: opts.printerName || '',
          copies: String(opts.copies || 1),
          language: previewSettings.language || '',
          theme: previewSettings.theme || ''
        }
      });
    } catch (error) {
      finish(() => reject(error));
    }
  });
}

function createPreviewFilePath() {
  const suffix = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return path.join(PRINT_TEMP_DIR, `preview-${suffix}.html`);
}

async function writePrintHtml(html, filePath) {
  await fs.mkdir(PRINT_TEMP_DIR, { recursive: true });
  const targetFile = filePath || LAST_PRINT_FILE;
  await fs.writeFile(targetFile, String(html || ''), 'utf8');
  return targetFile;
}

function waitForImages(win) {
  return win.webContents.executeJavaScript(`
    Promise.all(Array.from(document.images).map((img) => {
      if (img.complete && img.naturalWidth > 0) {
        return Promise.resolve();
      }
      if (typeof img.decode === 'function') {
        return img.decode().catch(() => undefined);
      }
      return new Promise((resolve) => {
        img.addEventListener('load', resolve, { once: true });
        img.addEventListener('error', resolve, { once: true });
      });
    }))
  `);
}

function waitForPaint(win) {
  return win.webContents.executeJavaScript(`
    new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)))
  `);
}

function createPrintFailureError(failureReason) {
  const isCancelled = isPrintCancelledReason(failureReason);
  const error = new Error(isCancelled ? 'print cancelled' : (failureReason || 'print failed'));
  error.code = isCancelled ? PRINT_CANCELLED_CODE : PRINT_FAILED_CODE;
  error.failureReason = failureReason || '';
  return error;
}

function isPrintCancelledReason(failureReason) {
  return /\bcancell?ed\b|\bcancel\b|\u53d6\u6d88/i.test(String(failureReason || ''));
}

module.exports = {
  createPrintFailureError,
  createElectronPrinter,
  isPrintCancelledReason,
  LAST_PRINT_FILE,
  PRINT_CANCELLED_CODE,
  PRINT_FAILED_CODE,
  previewAndPrintHtml,
  printHtml,
  waitForImages,
  waitForPaint,
  writePrintHtml
};
