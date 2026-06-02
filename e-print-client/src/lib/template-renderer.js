'use strict';

const assets = require('./assets');

async function renderTemplate(templateHtml, data, options) {
  const opts = options || {};
  const renderData = {
    ...(data || {}),
    qr: {},
    barcode: {}
  };

  const qrFields = opts.qrFields || findAssetFields(data, ['qrText', 'qrCode', 'qrcode']);
  const barcodeFields = opts.barcodeFields || findAssetFields(data, ['barcodeText', 'barCode', 'sku']);

  for (const field of qrFields) {
    renderData.qr[field] = await (opts.createQrDataUrl || assets.createQrDataUrl)(data[field]);
  }

  for (const field of barcodeFields) {
    renderData.barcode[field] = await (opts.createBarcodeDataUrl || assets.createBarcodeDataUrl)(data[field]);
  }

  const compile = opts.compile || defaultCompile;
  return compile(String(templateHtml || ''))(renderData);
}

function defaultCompile(templateHtml) {
  const Handlebars = require('handlebars');
  return Handlebars.compile(templateHtml, {
    noEscape: false,
    strict: false
  });
}

function findAssetFields(data, candidates) {
  if (!data || typeof data !== 'object') {
    return [];
  }

  return candidates.filter((field) => data[field] !== undefined && data[field] !== null && data[field] !== '');
}

module.exports = {
  findAssetFields,
  renderTemplate
};
