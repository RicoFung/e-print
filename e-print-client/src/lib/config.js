'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const PROJECT_CONFIG_PATH = path.resolve(__dirname, '..', '..', 'config.json');

const DEFAULT_CONFIG = {
  env: 'loc',
  clientId: 'CLIENT-001',
  serverUrl: 'ws://localhost:9090/ws/print',
  templateBaseUrl: 'http://localhost:9090/template',
  basicUsername: 'eprint',
  basicPassword: 'eprint123',
  printerName: '',
  silent: true,
  templateCacheDir: path.join(os.homedir(), '.e-print-client', 'templates')
};

const LEGACY_DEFAULT_CONFIG = {
  serverUrl: 'ws://localhost:8080/ws/print',
  templateBaseUrl: 'http://localhost:8080/template'
};

function loadConfig(configPath) {
  const resolvedPath = resolveConfigPath(configPath);
  const fileConfig = fs.existsSync(resolvedPath)
    ? JSON.parse(fs.readFileSync(resolvedPath, 'utf8'))
    : {};

  return applyEnvOverrides(applyEnvironmentConfig(migrateLegacyDefaults({
    ...DEFAULT_CONFIG,
    ...fileConfig
  })));
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

function applyEnvironmentConfig(config) {
  const env = resolveEnv(config);
  const envConfig = normalizeEnvironmentConfig(config.environments && config.environments[env]);
  const nextConfig = {
    ...config,
    ...envConfig,
    env
  };

  if (envConfig.serverUrl && !envConfig.templateBaseUrl) {
    nextConfig.templateBaseUrl = deriveTemplateBaseUrl(envConfig.serverUrl);
  }

  return nextConfig;
}

function resolveEnv(config) {
  return process.env.E_PRINT_ENV || process.env.NODE_ENV || config.env || DEFAULT_CONFIG.env;
}

function normalizeEnvironmentConfig(envConfig) {
  if (!envConfig) {
    return {};
  }

  const basic = envConfig.basic || {};
  const normalizedConfig = {
    ...envConfig,
  };

  const basicUsername = envConfig.basicUsername || basic.username;
  const basicPassword = envConfig.basicPassword || basic.password;

  if (basicUsername) {
    normalizedConfig.basicUsername = basicUsername;
  }

  if (basicPassword) {
    normalizedConfig.basicPassword = basicPassword;
  }

  return normalizedConfig;
}

function applyEnvOverrides(config) {
  const serverUrl = process.env.E_PRINT_SERVER_URL || config.serverUrl;
  const templateBaseUrl = process.env.E_PRINT_TEMPLATE_BASE_URL
    || (process.env.E_PRINT_SERVER_URL ? deriveTemplateBaseUrl(serverUrl) : config.templateBaseUrl);

  return {
    ...config,
    env: process.env.E_PRINT_ENV || process.env.NODE_ENV || config.env,
    clientId: process.env.E_PRINT_CLIENT_ID || config.clientId,
    serverUrl,
    templateBaseUrl,
    basicUsername: process.env.E_PRINT_BASIC_USERNAME || config.basicUsername,
    basicPassword: process.env.E_PRINT_BASIC_PASSWORD || config.basicPassword,
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
  url.pathname = '/template';
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
