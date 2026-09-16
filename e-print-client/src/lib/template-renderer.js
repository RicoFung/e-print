'use strict';

const assets = require('./assets');

async function renderTemplate(templateHtml, data, options) {
  const opts = options || {};
  const sourceData = data && typeof data === 'object' && !Array.isArray(data) ? data : {};
  const renderData = await resolveCodeAssets(sourceData, opts);
  const compile = opts.compile || defaultCompile;
  return compile(String(templateHtml || ''))(renderData);
}

async function resolveCodeAssets(value, options, path) {
  const opts = options || {};
  const currentPath = path || 'data';

  if (Array.isArray(value)) {
    return Promise.all(value.map((item, index) => resolveCodeAssets(item, opts, `${currentPath}[${index}]`)));
  }

  if (!value || typeof value !== 'object') {
    return value;
  }

  const resolved = {};
  for (const [key, child] of Object.entries(value)) {
    resolved[key] = await resolveCodeAssets(child, opts, `${currentPath}.${key}`);
  }

  if (!Object.prototype.hasOwnProperty.call(value, 'codeType')) {
    return resolved;
  }

  const codeType = value.codeType;
  if (codeType !== 'barcode' && codeType !== 'qr') {
    throw new Error(`unsupported codeType at ${currentPath}: ${String(codeType)}`);
  }
  if (typeof value.value !== 'string' || value.value.trim() === '') {
    throw new Error(`code value must be a non-empty string at ${currentPath}`);
  }

  const generator = codeType === 'barcode'
    ? opts.createBarcodeDataUrl || assets.createBarcodeDataUrl
    : opts.createQrDataUrl || assets.createQrDataUrl;
  resolved.dataUrl = await generator(value.value);
  return resolved;
}

function defaultCompile(templateHtml) {
  const Handlebars = require('handlebars');
  return Handlebars.compile(templateHtml, {
    noEscape: false,
    strict: false
  });
}

module.exports = {
  renderTemplate,
  resolveCodeAssets
};
