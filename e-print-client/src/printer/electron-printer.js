'use strict';

const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { BrowserWindow } = require('electron');

const PRINT_TEMP_DIR = path.join(os.tmpdir(), 'e-print-client');
const LAST_PRINT_FILE = path.join(PRINT_TEMP_DIR, 'last-print.html');

function createElectronPrinter() {
  return {
    print(html, options) {
      return printHtml(html, options);
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
            reject(new Error(failureReason || 'print failed'));
          }
        });
      })
      .catch((error) => {
        win.close();
        reject(error);
      });
  });
}

async function writePrintHtml(html) {
  await fs.mkdir(PRINT_TEMP_DIR, { recursive: true });
  await fs.writeFile(LAST_PRINT_FILE, String(html || ''), 'utf8');
  return LAST_PRINT_FILE;
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

module.exports = {
  createElectronPrinter,
  LAST_PRINT_FILE,
  printHtml,
  waitForImages,
  waitForPaint,
  writePrintHtml
};
