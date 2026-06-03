'use strict';

const path = require('node:path');
const { app, BrowserWindow, ipcMain, Menu } = require('electron');
const { deriveTemplateBaseUrl, loadConfig, saveConfig, resolveConfigPath } = require('./lib/config');
const { startPrintClient } = require('./lib/ws-client');
const { createElectronPrinter } = require('./printer/electron-printer');

const TEST_PAGE_HTML = `<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <title>e-print test page</title>
    <style>
      body {
        margin: 0;
        padding: 24px;
        font-family: Arial, sans-serif;
        color: #111827;
      }
      .page {
        border: 2px solid #111827;
        padding: 18px;
      }
      h1 {
        margin: 0 0 12px;
        font-size: 22px;
      }
      p {
        margin: 8px 0;
        font-size: 13px;
      }
      .mark {
        margin-top: 18px;
        height: 36px;
        background: repeating-linear-gradient(90deg, #111827 0 8px, #fff 8px 14px);
      }
    </style>
  </head>
  <body>
    <section class="page">
      <h1>e-print test page</h1>
      <p>If this page prints correctly, the selected printer is available.</p>
      <p>Printed at: ${new Date().toISOString()}</p>
      <div class="mark"></div>
    </section>
  </body>
</html>`;

let client;
let mainWindow;
let currentConfig;
let currentLanguage = 'en';

app.whenReady().then(() => {
  currentConfig = loadConfig();
  currentLanguage = detectLanguage();
  saveConfig(currentConfig);
  registerIpcHandlers();
  applyApplicationMenu(currentLanguage);
  createMainWindow();
  restartClient(currentConfig);
});

app.on('before-quit', () => {
  if (client && typeof client.stop === 'function') {
    client.stop();
  }
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (!mainWindow) {
    createMainWindow();
  }
});

function createMainWindow() {
  mainWindow = new BrowserWindow({
    width: 680,
    height: 410,
    minWidth: 680,
    minHeight: 410,
    title: 'E-PRINT-CLIENT',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false
    }
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  mainWindow.loadFile(path.join(__dirname, 'renderer', 'index.html'));
}

function registerIpcHandlers() {
  ipcMain.handle('config:get', () => ({
    config: currentConfig,
    configPath: resolveConfigPath()
  }));

  ipcMain.handle('config:save', (_event, nextConfig) => {
    currentConfig = normalizeUserConfig(nextConfig);
    saveConfig(currentConfig);
    restartClient(currentConfig);
    return {
      config: currentConfig,
      configPath: resolveConfigPath(),
      status: getClientStatus()
    };
  });

  ipcMain.handle('connection:get-status', () => getClientStatus());

  ipcMain.handle('connection:reconnect', () => {
    restartClient(currentConfig);
    return getClientStatus();
  });

  ipcMain.handle('printers:list', () => listPrinters());

  ipcMain.handle('printer:test', async (_event, options) => {
    const nextOptions = options || {};
    const printerName = nextOptions.printerName === undefined
      ? currentConfig.printerName
      : nextOptions.printerName;
    const silent = nextOptions.silent === undefined
      ? currentConfig.silent !== false
      : nextOptions.silent !== false;

    await createElectronPrinter().print(TEST_PAGE_HTML, {
      printerName,
      silent,
      copies: 1
    });

    return {
      ok: true,
      printerName: printerName || '',
      silent
    };
  });

  ipcMain.handle('app:set-language', (_event, language) => {
    currentLanguage = normalizeLanguage(language);
    applyApplicationMenu(currentLanguage);
    return currentLanguage;
  });
}

function applyApplicationMenu(language) {
  const labels = menuLabels[normalizeLanguage(language)];
  const template = [
    {
      label: labels.file,
      submenu: [
        {
          label: labels.quit,
          role: 'quit'
        }
      ]
    },
    {
      label: labels.edit,
      submenu: [
        { label: labels.undo, role: 'undo' },
        { label: labels.redo, role: 'redo' },
        { type: 'separator' },
        { label: labels.cut, role: 'cut' },
        { label: labels.copy, role: 'copy' },
        { label: labels.paste, role: 'paste' },
        { label: labels.selectAll, role: 'selectAll' }
      ]
    },
    {
      label: labels.view,
      submenu: [
        { label: labels.reload, role: 'reload' },
        { label: labels.forceReload, role: 'forceReload' },
        { label: labels.toggleDevTools, role: 'toggleDevTools' },
        { type: 'separator' },
        { label: labels.resetZoom, role: 'resetZoom' },
        { label: labels.zoomIn, role: 'zoomIn' },
        { label: labels.zoomOut, role: 'zoomOut' },
        { type: 'separator' },
        { label: labels.toggleFullScreen, role: 'togglefullscreen' }
      ]
    },
    {
      label: labels.window,
      submenu: [
        { label: labels.minimize, role: 'minimize' },
        { label: labels.close, role: 'close' }
      ]
    },
    {
      label: labels.help,
      submenu: [
        {
          label: labels.about,
          click: () => {
            if (mainWindow && mainWindow.webContents) {
              mainWindow.webContents.send('app:about');
            }
          }
        }
      ]
    }
  ];

  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}

function detectLanguage() {
  const locale = typeof app.getLocale === 'function' ? app.getLocale() : '';
  return normalizeLanguage(locale);
}

function normalizeLanguage(language) {
  return String(language || '').toLowerCase().startsWith('zh') ? 'zh-CN' : 'en';
}

const menuLabels = {
  en: {
    file: 'File',
    quit: 'Quit',
    edit: 'Edit',
    undo: 'Undo',
    redo: 'Redo',
    cut: 'Cut',
    copy: 'Copy',
    paste: 'Paste',
    selectAll: 'Select All',
    view: 'View',
    reload: 'Reload',
    forceReload: 'Force Reload',
    toggleDevTools: 'Toggle Developer Tools',
    resetZoom: 'Actual Size',
    zoomIn: 'Zoom In',
    zoomOut: 'Zoom Out',
    toggleFullScreen: 'Toggle Full Screen',
    window: 'Window',
    minimize: 'Minimize',
    close: 'Close',
    help: 'Help',
    about: 'About E-PRINT-CLIENT'
  },
  'zh-CN': {
    file: '\u6587\u4ef6',
    quit: '\u9000\u51fa',
    edit: '\u7f16\u8f91',
    undo: '\u64a4\u9500',
    redo: '\u91cd\u505a',
    cut: '\u526a\u5207',
    copy: '\u590d\u5236',
    paste: '\u7c98\u8d34',
    selectAll: '\u5168\u9009',
    view: '\u89c6\u56fe',
    reload: '\u91cd\u65b0\u52a0\u8f7d',
    forceReload: '\u5f3a\u5236\u91cd\u65b0\u52a0\u8f7d',
    toggleDevTools: '\u5f00\u53d1\u8005\u5de5\u5177',
    resetZoom: '\u5b9e\u9645\u5927\u5c0f',
    zoomIn: '\u653e\u5927',
    zoomOut: '\u7f29\u5c0f',
    toggleFullScreen: '\u5207\u6362\u5168\u5c4f',
    window: '\u7a97\u53e3',
    minimize: '\u6700\u5c0f\u5316',
    close: '\u5173\u95ed',
    help: '\u5e2e\u52a9',
    about: '\u5173\u4e8e E-PRINT-CLIENT'
  }
};

async function listPrinters() {
  if (!mainWindow || !mainWindow.webContents || typeof mainWindow.webContents.getPrintersAsync !== 'function') {
    return [];
  }

  const printers = await mainWindow.webContents.getPrintersAsync();
  return printers.map((printer) => ({
    name: printer.name,
    displayName: printer.displayName || printer.name,
    description: printer.description || '',
    status: printer.status,
    isDefault: Boolean(printer.isDefault)
  }));
}

function restartClient(config) {
  if (client && typeof client.stop === 'function') {
    client.stop();
  }

  client = startPrintClient(config, {
    printer: createElectronPrinter(),
    onStatusChange: sendStatus
  });
  sendStatus(client.getStatus());
}

function sendStatus(status) {
  if (mainWindow && mainWindow.webContents) {
    mainWindow.webContents.send('connection:status', status);
  }
}

function getClientStatus() {
  return client && typeof client.getStatus === 'function'
    ? client.getStatus()
    : {
        state: 'idle',
        serverUrl: currentConfig ? currentConfig.serverUrl : '',
        message: 'Not connected',
        updatedAt: new Date().toISOString()
      };
}

function normalizeUserConfig(input) {
  const nextConfig = {
    ...currentConfig,
    ...input
  };
  validateWebSocketUrl(nextConfig.serverUrl);
  nextConfig.templateBaseUrl = deriveTemplateBaseUrl(nextConfig.serverUrl);
  nextConfig.printerName = typeof nextConfig.printerName === 'string'
    ? nextConfig.printerName
    : '';
  nextConfig.silent = nextConfig.silent !== false;
  return nextConfig;
}

function validateWebSocketUrl(value) {
  const url = new URL(value);
  if (url.protocol !== 'ws:' && url.protocol !== 'wss:') {
    throw new Error('WebSocket URL must start with ws:// or wss://');
  }
}

