'use strict';

const api = window.ePrintClient;

const form = document.getElementById('configForm');
const serverUrlInput = document.getElementById('serverUrl');
const clientIdInput = document.getElementById('clientId');
const printerNameSelect = document.getElementById('printerName');
const silentSelect = document.getElementById('silent');
const templateBaseUrl = document.getElementById('templateBaseUrl');
const statusBadge = document.getElementById('statusBadge');
const statusText = document.getElementById('statusText');
const reconnectButton = document.getElementById('reconnectButton');
const refreshPrintersButton = document.getElementById('refreshPrintersButton');
const testPrintButton = document.getElementById('testPrintButton');
const languageButton = document.getElementById('languageButton');
const languageMenu = document.getElementById('languageMenu');
const languageText = document.getElementById('languageText');
const languageOptions = Array.from(document.querySelectorAll('[data-language-value]'));
const themeButton = document.getElementById('themeButton');
const themeMenu = document.getElementById('themeMenu');
const themeSwatch = document.getElementById('themeSwatch');
const themeOptions = Array.from(document.querySelectorAll('[data-theme-value]'));

let currentConfig;
let currentPrinters = [];
let currentStatus;
let currentLanguage = detectLanguage();
let currentTheme = loadTheme();

const messages = {
  en: {
    language: 'Language',
    theme: 'Theme',
    appSubtitle: 'Local print bridge',
    connection: 'Connection',
    connectionHint: 'Bind this device to the print server.',
    reconnect: 'Reconnect',
    websocketUrl: 'WebSocket URL',
    saveConnect: 'Save and connect',
    clientId: 'Client ID',
    templateApi: 'Template API',
    printer: 'Printer',
    device: 'Device',
    choosePrinter: 'Choose printer',
    printerHint: 'Choose output and test local printing.',
    refresh: 'Refresh',
    printTest: 'Print test page',
    silentPrinting: 'Silent printing',
    silentEnabled: 'Enabled',
    silentDisabled: 'Disabled',
    connectionStatus: 'Status',
    statusHint: 'Live connection diagnostics.',
    url: 'URL',
    message: 'Message',
    updated: 'Updated',
    idle: 'Idle',
    connecting: 'Connecting',
    connected: 'Connected',
    disconnected: 'Disconnected',
    error: 'Connection failed',
    stopped: 'Stopped',
    systemDefaultPrinter: 'System default printer',
    defaultSuffix: 'default',
    notFoundSuffix: 'not found',
    savingConfig: 'Saving configuration...',
    configSaved: 'Configuration saved. Connecting with current settings.',
    reconnecting: 'Reconnecting...',
    reconnectRequested: 'Reconnect requested.',
    reconnectFailed: 'Reconnect failed.',
    printingTest: 'Printing test page...',
    testPageSent: 'Test page sent to {printer}.',
    testPrintFailed: 'Test print failed.',
    loadingPrinters: 'Loading printers...',
    printerListUpdated: 'Printer list updated.',
    noPrintersFound: 'No printers found.',
    loadPrintersFailed: 'Failed to load printers.'
  },
  'zh-CN': {
    language: '\u8bed\u8a00',
    theme: '\u4e3b\u9898',
    appSubtitle: '\u672c\u5730\u6253\u5370\u6865\u63a5\u5668',
    connection: '\u8fde\u63a5',
    connectionHint: '\u5c06\u6b64\u8bbe\u5907\u7ed1\u5b9a\u5230\u6253\u5370\u670d\u52a1\u3002',
    reconnect: '\u91cd\u65b0\u8fde\u63a5',
    websocketUrl: 'WebSocket \u5730\u5740',
    saveConnect: '\u4fdd\u5b58\u5e76\u8fde\u63a5',
    clientId: '\u5ba2\u6237\u7aef ID',
    templateApi: '\u6a21\u677f\u63a5\u53e3',
    printer: '\u6253\u5370\u673a',
    device: '\u8bbe\u5907',
    choosePrinter: '\u9009\u62e9\u6253\u5370\u673a',
    printerHint: '\u9009\u62e9\u8f93\u51fa\u8bbe\u5907\u5e76\u6d4b\u8bd5\u672c\u5730\u6253\u5370\u3002',
    refresh: '\u5237\u65b0',
    printTest: '\u6253\u5370\u6d4b\u8bd5\u9875',
    silentPrinting: '\u9759\u9ed8\u6253\u5370',
    silentEnabled: '\u5f00\u542f',
    silentDisabled: '\u5173\u95ed',
    connectionStatus: '\u72b6\u6001',
    statusHint: '\u5b9e\u65f6\u8fde\u63a5\u8bca\u65ad\u3002',
    url: '\u5730\u5740',
    message: '\u6d88\u606f',
    updated: '\u66f4\u65b0\u65f6\u95f4',
    idle: '\u7a7a\u95f2',
    connecting: '\u8fde\u63a5\u4e2d',
    connected: '\u5df2\u8fde\u63a5',
    disconnected: '\u5df2\u65ad\u5f00',
    error: '\u8fde\u63a5\u5931\u8d25',
    stopped: '\u5df2\u505c\u6b62',
    systemDefaultPrinter: '\u7cfb\u7edf\u9ed8\u8ba4\u6253\u5370\u673a',
    defaultSuffix: '\u9ed8\u8ba4',
    notFoundSuffix: '\u672a\u627e\u5230',
    savingConfig: '\u6b63\u5728\u4fdd\u5b58\u914d\u7f6e...',
    configSaved: '\u914d\u7f6e\u5df2\u4fdd\u5b58\uff0c\u6b63\u5728\u4f7f\u7528\u5f53\u524d\u8bbe\u7f6e\u8fde\u63a5\u3002',
    reconnecting: '\u6b63\u5728\u91cd\u65b0\u8fde\u63a5...',
    reconnectRequested: '\u5df2\u8bf7\u6c42\u91cd\u65b0\u8fde\u63a5\u3002',
    reconnectFailed: '\u91cd\u65b0\u8fde\u63a5\u5931\u8d25\u3002',
    printingTest: '\u6b63\u5728\u6253\u5370\u6d4b\u8bd5\u9875...',
    testPageSent: '\u6d4b\u8bd5\u9875\u5df2\u53d1\u9001\u5230 {printer}\u3002',
    testPrintFailed: '\u6d4b\u8bd5\u6253\u5370\u5931\u8d25\u3002',
    loadingPrinters: '\u6b63\u5728\u52a0\u8f7d\u6253\u5370\u673a...',
    printerListUpdated: '\u6253\u5370\u673a\u5217\u8868\u5df2\u66f4\u65b0\u3002',
    noPrintersFound: '\u672a\u627e\u5230\u6253\u5370\u673a\u3002',
    loadPrintersFailed: '\u52a0\u8f7d\u6253\u5370\u673a\u5931\u8d25\u3002'
  }
};

const themeLabels = {
  ocean: 'Ocean Depths',
  sunset: 'Sunset Boulevard',
  forest: 'Forest Canopy',
  minimalist: 'Modern Minimalist',
  golden: 'Golden Hour',
  arctic: 'Arctic Frost',
  desert: 'Desert Rose',
  tech: 'Tech Innovation',
  botanical: 'Botanical Garden',
  midnight: 'Midnight Galaxy'
};

const legacyThemeMap = {
  mint: 'botanical',
  sky: 'arctic',
  peach: 'sunset'
};

init();

async function init() {
  applyTheme(currentTheme);
  applyLanguage(currentLanguage);
  api.onStatusChange(renderStatus);

  const result = await api.getConfig();
  currentConfig = result.config;
  renderConfig(result);
  await loadPrinters();
  renderStatus(await api.getStatus());
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  await saveConfig(t('savingConfig'), t('configSaved'));
});

reconnectButton.addEventListener('click', async () => {
  setFeedback(t('reconnecting'));
  try {
    renderStatus(await api.reconnect());
    setFeedback(t('reconnectRequested'));
  } catch (error) {
    setFeedback(error.message || t('reconnectFailed'), true);
  }
});

refreshPrintersButton.addEventListener('click', async () => {
  await loadPrinters();
});

testPrintButton.addEventListener('click', async () => {
  setFeedback(t('printingTest'));

  try {
    await saveConfig('', '');
    const result = await api.printTestPage({
      printerName: printerNameSelect.value,
      silent: isSilentSelected()
    });
    const printerLabel = result.printerName || t('systemDefaultPrinter');
    setFeedback(t('testPageSent', { printer: printerLabel }));
  } catch (error) {
    setFeedback(error.message || t('testPrintFailed'), true);
  }
});

languageButton.addEventListener('click', () => {
  toggleLanguageMenu(!isLanguageMenuOpen());
});

for (const option of languageOptions) {
  option.addEventListener('click', () => {
    applyLanguage(option.dataset.languageValue);
    toggleLanguageMenu(false);
  });
}

document.addEventListener('click', (event) => {
  if (!languageButton.contains(event.target) && !languageMenu.contains(event.target)) {
    toggleLanguageMenu(false);
  }
});

themeButton.addEventListener('click', () => {
  toggleThemeMenu(!isThemeMenuOpen());
});

for (const option of themeOptions) {
  option.addEventListener('click', () => {
    applyTheme(option.dataset.themeValue);
    toggleThemeMenu(false);
  });
}

document.addEventListener('click', (event) => {
  if (!themeButton.contains(event.target) && !themeMenu.contains(event.target)) {
    toggleThemeMenu(false);
  }
});

serverUrlInput.addEventListener('input', () => {
  try {
    templateBaseUrl.textContent = deriveTemplateBaseUrl(serverUrlInput.value.trim());
  } catch {
    templateBaseUrl.textContent = '-';
  }
});

printerNameSelect.addEventListener('change', () => {
  currentConfig = {
    ...currentConfig,
    printerName: printerNameSelect.value
  };
});

silentSelect.addEventListener('change', () => {
  currentConfig = {
    ...currentConfig,
    silent: isSilentSelected()
  };
});

async function saveConfig(startMessage, successMessage) {
  if (startMessage) {
    setFeedback(startMessage);
  }

  const result = await api.saveConfig({
    ...currentConfig,
    serverUrl: serverUrlInput.value.trim(),
    clientId: clientIdInput.value.trim(),
    printerName: printerNameSelect.value,
    silent: isSilentSelected()
  });

  currentConfig = result.config;
  renderConfig(result);
  renderStatus(result.status);

  if (successMessage) {
    setFeedback(successMessage);
  }

  return result;
}

async function loadPrinters() {
  setFeedback(t('loadingPrinters'));
  try {
    currentPrinters = await api.listPrinters();
    renderPrinters();
    setFeedback(currentPrinters.length ? t('printerListUpdated') : t('noPrintersFound'));
  } catch (error) {
    setFeedback(error.message || t('loadPrintersFailed'), true);
  }
}

function renderConfig(result) {
  serverUrlInput.value = result.config.serverUrl || '';
  clientIdInput.value = result.config.clientId || '';
  silentSelect.value = result.config.silent === false ? 'false' : 'true';
  templateBaseUrl.textContent = result.config.templateBaseUrl || '';
  renderPrinters();
}

function isSilentSelected() {
  return silentSelect.value !== 'false';
}

function renderPrinters() {
  const selectedPrinter = currentConfig && currentConfig.printerName
    ? currentConfig.printerName
    : '';

  printerNameSelect.replaceChildren(createPrinterOption('', t('systemDefaultPrinter')));

  for (const printer of currentPrinters) {
    const suffix = printer.isDefault ? ` (${t('defaultSuffix')})` : '';
    printerNameSelect.appendChild(createPrinterOption(printer.name, `${printer.displayName}${suffix}`));
  }

  const hasSelectedPrinter = !selectedPrinter || currentPrinters.some((printer) => printer.name === selectedPrinter);
  if (selectedPrinter && !hasSelectedPrinter) {
    printerNameSelect.appendChild(createPrinterOption(selectedPrinter, `${selectedPrinter} (${t('notFoundSuffix')})`));
  }

  printerNameSelect.value = selectedPrinter;
}

function createPrinterOption(value, label) {
  const option = document.createElement('option');
  option.value = value;
  option.textContent = label;
  return option;
}

function renderStatus(status) {
  currentStatus = status;
  const state = status && status.state ? status.state : 'idle';
  statusBadge.className = `status ${state}`;
  statusText.textContent = statusLabel(state);
  statusBadge.title = createStatusTitle(status);
}

function statusLabel(state) {
  return t(state) || state;
}

function setFeedback() {
  // The UI intentionally only shows the connection state color box.
}

function createStatusTitle(status) {
  if (!status) {
    return '';
  }

  const details = [];
  if (status.serverUrl) {
    details.push(`${t('url')}: ${status.serverUrl}`);
  }
  if (status.message) {
    details.push(`${t('message')}: ${status.message}`);
  }
  if (status.updatedAt) {
    details.push(`${t('updated')}: ${new Date(status.updatedAt).toLocaleString()}`);
  }
  return details.join('\n');
}

function deriveTemplateBaseUrl(serverUrl) {
  const url = new URL(serverUrl);
  url.protocol = url.protocol === 'wss:' ? 'https:' : 'http:';
  url.pathname = '/api/templates';
  url.search = '';
  url.hash = '';
  return url.toString().replace(/\/$/, '');
}

function applyLanguage(language) {
  currentLanguage = messages[language] ? language : 'en';
  if (typeof api.setLanguage === 'function') {
    api.setLanguage(currentLanguage).catch(() => {});
  }
  document.documentElement.lang = currentLanguage;
  languageText.textContent = currentLanguage === 'zh-CN' ? '\u4e2d\u6587' : 'EN';

  for (const option of languageOptions) {
    const isSelected = option.dataset.languageValue === currentLanguage;
    option.classList.toggle('active', isSelected);
    option.setAttribute('aria-selected', String(isSelected));
  }

  document.querySelectorAll('[data-i18n]').forEach((element) => {
    element.textContent = t(element.dataset.i18n);
  });

  if (currentConfig) {
    renderConfig({
      config: currentConfig
    });
  }

  if (currentStatus) {
    renderStatus(currentStatus);
  }
}

function detectLanguage() {
  const language = (navigator.language || '').toLowerCase();
  return language.startsWith('zh') ? 'zh-CN' : 'en';
}

function loadTheme() {
  return localStorage.getItem('ePrintTheme') || 'minimalist';
}

function applyTheme(theme) {
  const availableThemes = Object.keys(themeLabels);
  const normalizedTheme = legacyThemeMap[theme] || theme;
  currentTheme = availableThemes.includes(normalizedTheme) ? normalizedTheme : 'minimalist';
  document.documentElement.dataset.theme = currentTheme;
  localStorage.setItem('ePrintTheme', currentTheme);
  themeButton.setAttribute('aria-label', themeLabels[currentTheme]);
  themeSwatch.className = `themeSwatch ${themeClassName(currentTheme)}`;

  for (const option of themeOptions) {
    const isSelected = option.dataset.themeValue === currentTheme;
    option.classList.toggle('active', isSelected);
    option.setAttribute('aria-selected', String(isSelected));
  }
}

function isThemeMenuOpen() {
  return !themeMenu.hidden;
}

function toggleThemeMenu(isOpen) {
  themeMenu.hidden = !isOpen;
  themeButton.setAttribute('aria-expanded', String(isOpen));
}

function isLanguageMenuOpen() {
  return !languageMenu.hidden;
}

function toggleLanguageMenu(isOpen) {
  languageMenu.hidden = !isOpen;
  languageButton.setAttribute('aria-expanded', String(isOpen));
}

function capitalize(value) {
  return value ? value.charAt(0).toUpperCase() + value.slice(1) : '';
}

function themeClassName(theme) {
  return `theme${capitalize(theme)}`;
}

function t(key, params) {
  const dictionary = messages[currentLanguage] || messages.en;
  let message = dictionary[key] || messages.en[key] || key;
  for (const [name, value] of Object.entries(params || {})) {
    message = message.replace(`{${name}}`, value);
  }
  return message;
}


