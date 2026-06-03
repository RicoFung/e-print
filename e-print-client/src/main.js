'use strict';

const path = require('node:path');
const { app, BrowserWindow, ipcMain } = require('electron');
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

app.whenReady().then(() => {
  currentConfig = loadConfig();
  saveConfig(currentConfig);
  registerIpcHandlers();
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
    width: 820,
    height: 680,
    minWidth: 680,
    minHeight: 560,
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

}

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

