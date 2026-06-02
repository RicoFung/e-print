'use strict';

async function createQrDataUrl(text, options) {
  const QRCode = require('qrcode');
  return QRCode.toDataURL(String(text || ''), {
    errorCorrectionLevel: 'M',
    margin: 1,
    width: 180,
    ...(options || {})
  });
}

async function createBarcodeDataUrl(text, options) {
  const bwipjs = require('bwip-js');
  const buffer = await bwipjs.toBuffer({
    bcid: 'code128',
    text: String(text || ''),
    scale: 2,
    height: 10,
    includetext: false,
    ...(options || {})
  });

  return `data:image/png;base64,${buffer.toString('base64')}`;
}

module.exports = {
  createBarcodeDataUrl,
  createQrDataUrl
};
