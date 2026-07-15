'use strict';

const previewApi = window.printPreview;
const params = new URLSearchParams(window.location.search);
const frame = document.getElementById('printFrame');
const printButton = document.getElementById('printButton');
const cancelButton = document.getElementById('cancelButton');
const statusBadge = document.getElementById('previewStatus');
const statusText = document.getElementById('previewStatusText');
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
    failedWithDetail: 'Print failed: {detail}',
    userCancelled: 'user cancelled operation',
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
    failedWithDetail: '\u6253\u5370\u5931\u8d25\uff1a{detail}',
    userCancelled: '\u7528\u6237\u53d6\u6d88\u64cd\u4f5c',
    copies: '{copies} \u4efd',
    printer: '{printer}'
  }
};

const language = normalizeLanguage(params.get('language') || detectLanguage());
const theme = normalizeTheme(params.get('theme'));

applyTheme();
applyLanguage();
loadPreview();

printButton.addEventListener('click', () => {
  setBusy(true);
  setStatus(t('printing'), 'printing');
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
    setStatus(t('printing'), 'printing');
    return;
  }

  if (state.state === 'failed') {
    setBusy(false);
    setStatus(formatPrintFailureMessage(state), 'error');
  }
});

function loadPreview() {
  const src = params.get('src');
  if (!src) {
    setStatus(t('failed'), 'error');
    setBusy(true);
    return;
  }

  setStatus(t('loading'), 'loading');
  frame.addEventListener('load', () => setStatus(t('ready'), 'ready'), { once: true });
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

function setStatus(message, state) {
  const nextState = state || 'ready';
  statusText.textContent = message;
  statusBadge.className = `previewStatus ${nextState}`;
}

function formatPrintFailureMessage(state) {
  const message = state && state.message ? state.message : '';
  if (!message) {
    return t('failed');
  }

  const detail = isUserCancelFailure(state) ? t('userCancelled') : message;
  return t('failedWithDetail', { detail });
}

function isUserCancelFailure(state) {
  return state && (
    state.code === 'PRINT_CANCELLED' ||
    /\bcancell?ed\b|\bcancel\b|\u53d6\u6d88/i.test(String(state.message || ''))
  );
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

function normalizeLanguage(value) {
  return value === 'zh-CN' ? 'zh-CN' : 'en';
}

function applyTheme() {
  document.documentElement.dataset.theme = theme;
}

function normalizeTheme(value) {
  return value === 'sky' ? 'sky' : 'black';
}

function t(key, params) {
  const dictionary = messages[language] || messages.en;
  let message = dictionary[key] || messages.en[key] || key;
  for (const [name, value] of Object.entries(params || {})) {
    message = message.replace(`{${name}}`, value);
  }
  return message;
}
