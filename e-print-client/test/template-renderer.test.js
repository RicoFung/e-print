'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const { renderTemplate, resolveCodeAssets } = require('../src/lib/template-renderer');

const generators = {
  createQrDataUrl: async (text) => `qr:${text}`,
  createBarcodeDataUrl: async (text) => `bar:${text}`
};

test('renders business-named code assets at independent template positions', async () => {
  const html = await renderTemplate(`
    <header><img src="{{codes.receiptBarcode.dataUrl}}"></header>
    <main><img src="{{codes.memberQr.dataUrl}}"></main>
    <footer><img src="{{codes.afterSalesQr.dataUrl}}"></footer>
  `, {
    codes: {
      receiptBarcode: {
        codeType: 'barcode',
        value: 'SALE202609160001'
      },
      memberQr: {
        codeType: 'qr',
        value: 'https://example.com/member/001'
      },
      afterSalesQr: {
        codeType: 'qr',
        value: 'https://example.com/service'
      }
    }
  }, generators);

  assert.match(html, /<header><img src="bar:SALE202609160001"><\/header>/);
  assert.match(html, /<main><img src="qr:https:\/\/example.com\/member\/001"><\/main>/);
  assert.match(html, /<footer><img src="qr:https:\/\/example.com\/service"><\/footer>/);
});

test('recursively resolves code assets in arrays without modifying request data', async () => {
  const data = {
    items: [
      {
        name: 'MacBook Pro',
        productBarcode: {
          codeType: 'barcode',
          value: 'SKU-MBP-001'
        }
      },
      {
        name: 'USB-C Adapter',
        productQr: {
          codeType: 'qr',
          value: 'https://example.com/products/adapter'
        }
      }
    ]
  };
  const original = JSON.parse(JSON.stringify(data));

  const resolved = await resolveCodeAssets(data, generators);

  assert.equal(resolved.items[0].productBarcode.dataUrl, 'bar:SKU-MBP-001');
  assert.equal(resolved.items[1].productQr.dataUrl, 'qr:https://example.com/products/adapter');
  assert.deepEqual(data, original);
  assert.notEqual(resolved, data);
  assert.notEqual(resolved.items, data.items);
});

test('does not support legacy barcodeText and qrText fields', async () => {
  let generated = false;
  const html = await renderTemplate(
    '<img src="{{barcode.barcodeText}}"><img src="{{qr.qrText}}">',
    {
      barcode: { barcodeText: 'SALE202609160001' },
      qr: { qrText: 'https://example.com/receipt/SALE202609160001' }
    },
    {
      createQrDataUrl: async () => {
        generated = true;
        return 'unexpected';
      },
      createBarcodeDataUrl: async () => {
        generated = true;
        return 'unexpected';
      }
    }
  );

  assert.equal(html, '<img src="SALE202609160001"><img src="https://example.com/receipt/SALE202609160001">');
  assert.equal(generated, false);
});

test('rejects unsupported code types', async () => {
  await assert.rejects(
    () => resolveCodeAssets({ codes: { receiptCode: { codeType: 'pdf417', value: 'SALE-001' } } }, generators),
    /unsupported codeType at data\.codes\.receiptCode: pdf417/
  );
});

test('rejects empty code values', async () => {
  await assert.rejects(
    () => resolveCodeAssets({ codes: { receiptCode: { codeType: 'qr', value: '  ' } } }, generators),
    /code value must be a non-empty string at data\.codes\.receiptCode/
  );
});
