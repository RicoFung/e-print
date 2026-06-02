'use strict';

const { BrowserWindow } = require('electron');

function createElectronPrinter() {
  return {
    print(html, options) {
      return printHtml(html, options);
    }
  };
}

function printHtml(html, options) {
  return new Promise((resolve, reject) => {
    const opts = options || {};
    const win = new BrowserWindow({
      show: opts.silent === false,
      webPreferences: {
        sandbox: true,
        contextIsolation: true,
        nodeIntegration: false
      }
    });

    win.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(html)}`)
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

module.exports = {
  createElectronPrinter,
  printHtml
};
