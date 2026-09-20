'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const {
  discoverPrinters,
  listWindowsPrinters
} = require('../src/lib/printer-discovery');

const silentLogger = {
  warn: () => {}
};

test('uses Electron printer discovery when printers are available', async () => {
  const printers = await discoverPrinters({
    getPrintersAsync: async () => [{
      name: 'Electron Printer',
      displayName: 'Electron Printer',
      isDefault: true
    }]
  }, {
    logger: silentLogger,
    execFile: () => assert.fail('Windows fallback should not run')
  });

  assert.equal(printers.length, 1);
  assert.equal(printers[0].name, 'Electron Printer');
  assert.equal(printers[0].isDefault, true);
});

test('falls back to Windows discovery when Electron returns no printers', async () => {
  const printers = await discoverPrinters({
    getPrintersAsync: async () => []
  }, {
    logger: silentLogger,
    execFile: createExecFileMock(JSON.stringify([{
      name: 'Microsoft Print to PDF',
      displayName: 'Microsoft Print to PDF',
      description: 'Microsoft Print To PDF',
      status: 3,
      isDefault: true
    }]))
  });

  assert.equal(printers.length, 1);
  assert.equal(printers[0].name, 'Microsoft Print to PDF');
  assert.equal(printers[0].isDefault, true);
});

test('parses a single Windows printer response', async () => {
  const printers = await listWindowsPrinters(createExecFileMock(JSON.stringify({
    name: 'Label Printer',
    displayName: 'Label Printer',
    isDefault: false
  })));

  assert.equal(printers.length, 1);
  assert.equal(printers[0].name, 'Label Printer');
});

function createExecFileMock(stdout) {
  return (_file, _args, _options, callback) => callback(null, stdout, '');
}
