'use strict';

const { execFile } = require('node:child_process');

const POWERSHELL_PRINTER_SCRIPT = [
  '[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)',
  '$printers = @(Get-CimInstance Win32_Printer | ForEach-Object {',
  '  [pscustomobject]@{',
  '    name = $_.Name',
  '    displayName = $_.Name',
  '    description = $_.DriverName',
  '    status = [int]$_.PrinterStatus',
  '    isDefault = [bool]$_.Default',
  '  }',
  '})',
  'ConvertTo-Json -InputObject $printers -Compress'
].join('\n');

async function discoverPrinters(webContents, options = {}) {
  const logger = options.logger || console;

  if (webContents && typeof webContents.getPrintersAsync === 'function') {
    try {
      const printers = normalizePrinters(await webContents.getPrintersAsync());
      if (printers.length) {
        return printers;
      }
      logger.warn('Electron returned no printers; trying Windows printer discovery');
    } catch (error) {
      logger.warn(`Electron printer discovery failed: ${error.message}`);
    }
  }

  if (process.platform !== 'win32' && !options.execFile) {
    return [];
  }

  return listWindowsPrinters(options.execFile || execFile);
}

function listWindowsPrinters(execFileImpl = execFile) {
  return new Promise((resolve, reject) => {
    execFileImpl(
      'powershell.exe',
      ['-NoProfile', '-NonInteractive', '-Command', POWERSHELL_PRINTER_SCRIPT],
      {
        encoding: 'utf8',
        maxBuffer: 1024 * 1024,
        windowsHide: true
      },
      (error, stdout, stderr) => {
        if (error) {
          reject(new Error(`Windows printer discovery failed: ${(stderr || error.message).trim()}`));
          return;
        }

        try {
          const output = String(stdout || '').replace(/^\uFEFF/, '').trim();
          resolve(normalizePrinters(output ? JSON.parse(output) : []));
        } catch (parseError) {
          reject(new Error(`Invalid Windows printer response: ${parseError.message}`));
        }
      }
    );
  });
}

function normalizePrinters(printers) {
  const entries = Array.isArray(printers) ? printers : printers ? [printers] : [];
  return entries
    .filter((printer) => printer && printer.name)
    .map((printer) => ({
      name: String(printer.name),
      displayName: String(printer.displayName || printer.name),
      description: String(printer.description || ''),
      status: printer.status,
      isDefault: Boolean(printer.isDefault)
    }));
}

module.exports = {
  discoverPrinters,
  listWindowsPrinters,
  normalizePrinters
};
