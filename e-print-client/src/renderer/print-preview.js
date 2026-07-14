'use strict';

const previewApi = window.printPreview;
const params = new URLSearchParams(window.location.search);
const frame = document.getElementById('printFrame');
const printButton = document.getElementById('printButton');
const cancelButton = document.getElementById('cancelButton');
const statusText = document.getElementById('previewStatus');
const metaText = document.getElementById('previewMeta');

const messages = {
  en: {
    title: 'Print preview',
    print: 'Print',
    cancel: 'Cancel',
    ready: 'Ready',
    loading: 'Loading...',
    printing: 'Printing...',
    failed: 'Print failed',
    copies: '{copies} copies',
    printer: '{printer}'
  },
  'zh-CN': {
    title: '\u6253\u5370\u9884\u89c8',
    print: '\u6253\u5370',
    cancel: '\u53d6\u6d88',
    ready: '\u5c31\u7eea',
    loading: '\u6b63\u5728\u52a0\u8f7d...',
    printing: '\u6b63\u5728\u6253\u5370...',
    failed: '\u6253\u5370\u5931\u8d25',
    copies: '{copies} \u4efd',
    printer: '{printer}'
  }
};

const language = detectLanguage();

applyLanguage();
loadPreview();

printButton.addEventListener('click', () => {
  setBusy(true);
  setStatus(t('printing'));
  previewApi.print();
});

cancelButton.addEventListener('click', () => {
  previewApi.cancel();
});

previewApi.onStateChange((state) => {
  if (!state || !state.state) {
    return;
  }

  if (state.state === 'printing') {
    setBusy(true);
    setStatus(t('printing'));
    return;
  }

  if (state.state === 'failed') {
    setBusy(false);
    setStatus(state.message ? `${t('failed')}: ${state.message}` : t('failed'), true);
  }
});

function loadPreview() {
  const src = params.get('src');
  if (!src) {
    setStatus(t('failed'), true);
    setBusy(true);
    return;
  }

  setStatus(t('loading'));
  frame.addEventListener('load', () => setStatus(t('ready')), { once: true });
  frame.src = src;
  renderMeta();
}

function renderMeta() {
  const details = [];
  const printerName = params.get('printerName');
  const copies = params.get('copies') || '1';

  if (printerName) {
    details.push(t('printer', { printer: printerName }));
  }
  details.push(t('copies', { copies }));
  metaText.textContent = details.join(' / ');
}

function setBusy(isBusy) {
  printButton.disabled = isBusy;
  cancelButton.disabled = false;
}

function setStatus(message, isError) {
  statusText.textContent = message;
  statusText.classList.toggle('error', Boolean(isError));
}

function applyLanguage() {
  document.documentElement.lang = language;
  document.querySelectorAll('[data-i18n]').forEach((element) => {
    element.textContent = t(element.dataset.i18n);
  });
}

function detectLanguage() {
  return (navigator.language || '').toLowerCase().startsWith('zh') ? 'zh-CN' : 'en';
}

function t(key, params) {
  const dictionary = messages[language] || messages.en;
  let message = dictionary[key] || messages.en[key] || key;
  for (const [name, value] of Object.entries(params || {})) {
    message = message.replace(`{${name}}`, value);
  }
  return message;
}
