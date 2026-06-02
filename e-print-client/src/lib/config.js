'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const PROJECT_CONFIG_PATH = path.resolve(__dirname, '..', '..', 'config.json');

const DEFAULT_CONFIG = {
  clientId: 'CLIENT-001',
  serverUrl: 'ws://localhost:9090/ws/print',
  templateBaseUrl: 'http://localhost:9090/api/templates',
  printerName: '',
  silent: true,
  templateCacheDir: path.join(os.homedir(), '.e-print-client', 'templates')
};

const LEGACY_DEFAULT_CONFIG = {
  serverUrl: 'ws://localhost:8080/ws/print',
  templateBaseUrl: 'http://localhost:8080/api/templates'
};

function loadConfig(configPath) {
  const resolvedPath = resolveConfigPath(configPath);
  const fileConfig = fs.existsSync(resolvedPath)
    ? JSON.parse(fs.readFileSync(resolvedPath, 'utf8'))
    : {};

  return applyEnv(migrateLegacyDefaults({
    ...DEFAULT_CONFIG,
    ...fileConfig
  }));
}

function saveConfig(config, configPath) {
  const resolvedPath = resolveConfigPath(configPath);
  const nextConfig = normalizeConfig(config);
  fs.mkdirSync(path.dirname(resolvedPath), { recursive: true });
  fs.writeFileSync(resolvedPath, `${JSON.stringify(nextConfig, null, 2)}\n`, 'utf8');
  return resolvedPath;
}

function resolveConfigPath(configPath) {
  return configPath || PROJECT_CONFIG_PATH;
}

function applyEnv(config) {
  return {
    ...config,
    clientId: process.env.E_PRINT_CLIENT_ID || config.clientId,
    serverUrl: process.env.E_PRINT_SERVER_URL || config.serverUrl,
    templateBaseUrl: process.env.E_PRINT_TEMPLATE_BASE_URL || config.templateBaseUrl,
    printerName: process.env.E_PRINT_PRINTER_NAME || config.printerName
  };
}

function migrateLegacyDefaults(config) {
  return {
    ...config,
    serverUrl: config.serverUrl === LEGACY_DEFAULT_CONFIG.serverUrl
      ? DEFAULT_CONFIG.serverUrl
      : config.serverUrl,
    templateBaseUrl: config.templateBaseUrl === LEGACY_DEFAULT_CONFIG.templateBaseUrl
      ? DEFAULT_CONFIG.templateBaseUrl
      : config.templateBaseUrl
  };
}

function normalizeConfig(config) {
  const serverUrl = config.serverUrl || DEFAULT_CONFIG.serverUrl;
  return {
    ...DEFAULT_CONFIG,
    ...config,
    serverUrl,
    templateBaseUrl: config.templateBaseUrl || deriveTemplateBaseUrl(serverUrl)
  };
}

function deriveTemplateBaseUrl(serverUrl) {
  const url = new URL(serverUrl || DEFAULT_CONFIG.serverUrl);
  url.protocol = url.protocol === 'wss:' ? 'https:' : 'http:';
  url.pathname = '/api/templates';
  url.search = '';
  url.hash = '';
  return url.toString().replace(/\/$/, '');
}

module.exports = {
  DEFAULT_CONFIG,
  deriveTemplateBaseUrl,
  loadConfig,
  saveConfig,
  resolveConfigPath
};
