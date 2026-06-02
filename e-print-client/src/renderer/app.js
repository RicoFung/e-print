'use strict';

const api = window.ePrintClient;

const form = document.getElementById('configForm');
const serverUrlInput = document.getElementById('serverUrl');
const clientIdInput = document.getElementById('clientId');
const printerNameSelect = document.getElementById('printerName');
const silentInput = document.getElementById('silent');
const templateBaseUrl = document.getElementById('templateBaseUrl');
const configPath = document.getElementById('configPath');
const statusBadge = document.getElementById('statusBadge');
const statusText = document.getElementById('statusText');
const statusUrl = document.getElementById('statusUrl');
const statusMessage = document.getElementById('statusMessage');
const statusUpdatedAt = document.getElementById('statusUpdatedAt');
const feedback = document.getElementById('feedback');
const reconnectButton = document.getElementById('reconnectButton');
const refreshPrintersButton = document.getElementById('refreshPrintersButton');
const testPrintButton = document.getElementById('testPrintButton');

let currentConfig;
let currentPrinters = [];

init();

async function init() {
  api.onStatusChange(renderStatus);

  const result = await api.getConfig();
  currentConfig = result.config;
  renderConfig(result);
  await loadPrinters();
  renderStatus(await api.getStatus());
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  await saveConfig('Saving configuration...', 'Configuration saved. Connecting with current settings.');
});

reconnectButton.addEventListener('click', async () => {
  setFeedback('Reconnecting...');
  try {
    renderStatus(await api.reconnect());
    setFeedback('Reconnect requested.');
  } catch (error) {
    setFeedback(error.message || 'Reconnect failed.', true);
  }
});

refreshPrintersButton.addEventListener('click', async () => {
  await loadPrinters();
});

testPrintButton.addEventListener('click', async () => {
  setFeedback('Printing test page...');

  try {
    await saveConfig('', '');
    const result = await api.printTestPage({
      printerName: printerNameSelect.value,
      silent: silentInput.checked
    });
    const printerLabel = result.printerName || 'system default printer';
    setFeedback(`Test page sent to ${printerLabel}.`);
  } catch (error) {
    setFeedback(error.message || 'Test print failed.', true);
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

silentInput.addEventListener('change', () => {
  currentConfig = {
    ...currentConfig,
    silent: silentInput.checked
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
    silent: silentInput.checked
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
  setFeedback('Loading printers...');
  try {
    currentPrinters = await api.listPrinters();
    renderPrinters();
    setFeedback(currentPrinters.length ? 'Printer list updated.' : 'No printers found.');
  } catch (error) {
    setFeedback(error.message || 'Failed to load printers.', true);
  }
}

function renderConfig(result) {
  serverUrlInput.value = result.config.serverUrl || '';
  clientIdInput.value = result.config.clientId || '';
  silentInput.checked = result.config.silent !== false;
  templateBaseUrl.textContent = result.config.templateBaseUrl || '';
  configPath.textContent = result.configPath ? `Config file: ${result.configPath}` : '';
  renderPrinters();
}

function renderPrinters() {
  const selectedPrinter = currentConfig && currentConfig.printerName
    ? currentConfig.printerName
    : '';

  printerNameSelect.replaceChildren(createPrinterOption('', 'System default printer'));

  for (const printer of currentPrinters) {
    const suffix = printer.isDefault ? ' (default)' : '';
    printerNameSelect.appendChild(createPrinterOption(printer.name, `${printer.displayName}${suffix}`));
  }

  const hasSelectedPrinter = !selectedPrinter || currentPrinters.some((printer) => printer.name === selectedPrinter);
  if (selectedPrinter && !hasSelectedPrinter) {
    printerNameSelect.appendChild(createPrinterOption(selectedPrinter, `${selectedPrinter} (not found)`));
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
  const state = status && status.state ? status.state : 'idle';
  statusBadge.className = `status ${state}`;
  statusText.textContent = statusLabel(state);
  statusUrl.textContent = status && status.serverUrl ? status.serverUrl : '-';
  statusMessage.textContent = status && status.message ? status.message : '-';
  statusUpdatedAt.textContent = status && status.updatedAt
    ? new Date(status.updatedAt).toLocaleString()
    : '-';
}

function statusLabel(state) {
  const labels = {
    idle: 'Idle',
    connecting: 'Connecting',
    connected: 'Connected',
    disconnected: 'Disconnected',
    error: 'Connection failed',
    stopped: 'Stopped'
  };
  return labels[state] || state;
}

function setFeedback(message, isError) {
  feedback.textContent = message;
  feedback.className = isError ? 'feedback error' : 'feedback';
}

function deriveTemplateBaseUrl(serverUrl) {
  const url = new URL(serverUrl);
  url.protocol = url.protocol === 'wss:' ? 'https:' : 'http:';
  url.pathname = '/api/templates';
  url.search = '';
  url.hash = '';
  return url.toString().replace(/\/$/, '');
}
