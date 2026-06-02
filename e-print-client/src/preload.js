'use strict';

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('ePrintClient', {
  getConfig: () => ipcRenderer.invoke('config:get'),
  saveConfig: (config) => ipcRenderer.invoke('config:save', config),
  getStatus: () => ipcRenderer.invoke('connection:get-status'),
  reconnect: () => ipcRenderer.invoke('connection:reconnect'),
  listPrinters: () => ipcRenderer.invoke('printers:list'),
  printTestPage: (options) => ipcRenderer.invoke('printer:test', options),
  onStatusChange: (callback) => {
    const listener = (_event, status) => callback(status);
    ipcRenderer.on('connection:status', listener);
    return () => ipcRenderer.removeListener('connection:status', listener);
  }
});
