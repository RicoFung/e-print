'use strict';

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('printPreview', {
  print: () => ipcRenderer.send('print-preview:print'),
  cancel: () => ipcRenderer.send('print-preview:cancel'),
  onStateChange: (callback) => {
    const listener = (_event, state) => callback(state);
    ipcRenderer.on('print-preview:state', listener);
    return () => ipcRenderer.removeListener('print-preview:state', listener);
  }
});
