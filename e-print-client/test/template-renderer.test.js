'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const { findAssetFields, renderTemplate } = require('../src/lib/template-renderer');

test('detects QR and barcode candidate fields', () => {
  assert.deepEqual(findAssetFields({ qrText: 'x', sku: 'S1' }, ['qrText', 'sku']), ['qrText', 'sku']);
  assert.deepEqual(findAssetFields({ qrText: '' }, ['qrText']), []);
});

test('renders template with injected compiler and asset generators', async () => {
  const html = await renderTemplate('<div>{{productName}}</div>', {
    productName: 'MacBook Pro',
    qrText: 'https://example.com/p/MBP-001',
    sku: 'MBP-001'
  }, {
    compile: () => (data) => `${data.productName}|${data.qr.qrText}|${data.barcode.sku}`,
    createQrDataUrl: async (text) => `qr:${text}`,
    createBarcodeDataUrl: async (text) => `bar:${text}`
  });

  assert.equal(html, 'MacBook Pro|qr:https://example.com/p/MBP-001|bar:MBP-001');
});
