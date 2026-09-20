'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const CONFIG_FILE_NAME = 'config.json';
const PROJECT_CONFIG_PATH = path.resolve(__dirname, '..', '..', 'config.json');
const DEFAULT_TEMPLATE_SOURCE = 'qiniu';
const TEMPLATE_SOURCES = new Set(['qiniu', 'minio']);
let userConfigPath;
let runtimeTemplateCacheDir = path.join(os.homedir(), '.e-print-client', 'templates');

const DEFAULT_CONFIG = {
  configVersion: 2,
  env: 'loc',
  clientId: 'CLIENT-001',
  serverUrl: '',
  templateSource: DEFAULT_TEMPLATE_SOURCE,
  templateBaseUrl: '',
  basicUsername: '',
  printerName: '',
  silent: false,
  templateCacheDir: runtimeTemplateCacheDir
};

const LEGACY_DEFAULT_CONFIGS = [
  {
    serverUrl: 'ws://localhost:9090/ws/print',
    templateBaseUrl: 'http://localhost:9090/template'
  },
  {
    serverUrl: 'ws://localhost:8080/ws/print',
    templateBaseUrl: 'http://localhost:8080/template'
  }
];
const LEGACY_MIGRATION_TARGET = {
  serverUrl: 'ws://localhost:8080/e-print-server/ws/print',
  templateBaseUrl: 'http://localhost:8080/e-print-server/qiniu/template'
};

function loadConfig(configPath) {
  const resolvedPath = resolveConfigPath(configPath);
  const fileConfig = migrateConfig(readConfig(resolvedPath) || readInitialConfig(resolvedPath));

  return normalizeConfig(applyEnvOverrides(applyEnvironmentConfig(migrateLegacyDefaults({
    ...DEFAULT_CONFIG,
    ...fileConfig
  }))));
}

function saveConfig(config, configPath) {
  const resolvedPath = resolveConfigPath(configPath);
  const nextConfig = normalizeConfig(config);
  fs.mkdirSync(path.dirname(resolvedPath), { recursive: true });
  fs.writeFileSync(resolvedPath, `${JSON.stringify(nextConfig, null, 2)}\n`, 'utf8');
  return resolvedPath;
}

function resolveConfigPath(configPath) {
  return configPath || process.env.E_PRINT_CONFIG_PATH || userConfigPath || PROJECT_CONFIG_PATH;
}

function configureUserConfigPath(userDataPath) {
  userConfigPath = path.join(userDataPath, CONFIG_FILE_NAME);
  runtimeTemplateCacheDir = path.join(userDataPath, 'templates');
  return userConfigPath;
}

function readConfig(configPath) {
  return fs.existsSync(configPath)
    ? JSON.parse(fs.readFileSync(configPath, 'utf8'))
    : null;
}

function readInitialConfig(resolvedPath) {
  if (resolvedPath === PROJECT_CONFIG_PATH) {
    return {};
  }

  return readConfig(PROJECT_CONFIG_PATH) || {};
}

function applyEnvironmentConfig(config) {
  const env = resolveEnv(config);
  const envConfig = normalizeEnvironmentConfig(config.environments && config.environments[env]);
  const templateSource = normalizeTemplateSource(envConfig.templateSource || config.templateSource);
  const nextConfig = {
    ...config,
    ...envConfig,
    templateSource,
    env
  };

  if (envConfig.serverUrl && !envConfig.templateBaseUrl) {
    nextConfig.templateBaseUrl = deriveTemplateBaseUrl(envConfig.serverUrl, templateSource);
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
  const templateSource = normalizeTemplateSource(process.env.E_PRINT_TEMPLATE_SOURCE || config.templateSource);
  const templateBaseUrl = process.env.E_PRINT_TEMPLATE_BASE_URL
    || (process.env.E_PRINT_SERVER_URL
      ? deriveTemplateBaseUrl(serverUrl, templateSource)
      : config.templateBaseUrl);

  return {
    ...config,
    env: process.env.E_PRINT_ENV || process.env.NODE_ENV || config.env,
    clientId: process.env.E_PRINT_CLIENT_ID || config.clientId,
    serverUrl,
    templateSource,
    templateBaseUrl,
    basicUsername: process.env.E_PRINT_BASIC_USERNAME || config.basicUsername,
    basicPassword: process.env.E_PRINT_BASIC_PASSWORD || config.basicPassword,
    printerName: process.env.E_PRINT_PRINTER_NAME || config.printerName
  };
}

function migrateLegacyDefaults(config) {
  return {
    ...config,
    serverUrl: LEGACY_DEFAULT_CONFIGS.some(({ serverUrl }) => config.serverUrl === serverUrl)
      ? LEGACY_MIGRATION_TARGET.serverUrl
      : config.serverUrl,
    templateBaseUrl: LEGACY_DEFAULT_CONFIGS.some(({ templateBaseUrl }) => config.templateBaseUrl === templateBaseUrl)
      ? LEGACY_MIGRATION_TARGET.templateBaseUrl
      : config.templateBaseUrl
  };
}

function normalizeConfig(config) {
  const serverUrl = deriveWebSocketUrl(config.serverUrl || DEFAULT_CONFIG.serverUrl);
  const templateSource = normalizeTemplateSource(config.templateSource);
  return {
    ...DEFAULT_CONFIG,
    ...config,
    serverUrl,
    templateSource,
    templateBaseUrl: normalizeTemplateBaseUrl(config.templateBaseUrl, serverUrl, templateSource),
    templateCacheDir: normalizeTemplateCacheDir(config.templateCacheDir)
  };
}

function migrateConfig(config) {
  const migrated = { ...config };
  if (!migrated.configVersion || migrated.configVersion < 2) {
    migrated.silent = false;
    migrated.configVersion = 2;
  }
  return migrated;
}

function deriveWebSocketUrl(serverAddress) {
  if (!serverAddress) {
    return '';
  }

  const url = new URL(serverAddress);
  if (url.protocol !== 'ws:' && url.protocol !== 'wss:') {
    throw new Error('Server address must start with ws:// or wss://');
  }

  url.pathname = `${url.pathname.replace(/\/ws\/print\/?$/, '').replace(/\/$/, '')}/ws/print`;
  url.search = '';
  url.hash = '';
  return url.toString().replace(/\/$/, '');
}

function deriveServerBaseUrl(serverUrl) {
  if (!serverUrl) {
    return '';
  }

  const url = new URL(serverUrl);
  url.pathname = url.pathname.replace(/\/ws\/print\/?$/, '').replace(/\/$/, '');
  url.search = '';
  url.hash = '';
  return url.toString().replace(/\/$/, '');
}

function normalizeTemplateCacheDir(templateCacheDir) {
  if (!templateCacheDir || isLegacyDefaultTemplateCacheDir(templateCacheDir)) {
    return runtimeTemplateCacheDir;
  }

  return templateCacheDir;
}

function isLegacyDefaultTemplateCacheDir(templateCacheDir) {
  const normalizedPath = path.normalize(templateCacheDir);
  return path.basename(normalizedPath).toLowerCase() === 'templates'
    && path.basename(path.dirname(normalizedPath)).toLowerCase() === '.e-print-client';
}

function deriveTemplateBaseUrl(serverUrl, templateSource = DEFAULT_TEMPLATE_SOURCE) {
  if (!serverUrl) {
    return '';
  }

  const url = new URL(serverUrl);
  url.protocol = url.protocol === 'wss:' ? 'https:' : 'http:';
  const contextPath = url.pathname
    .replace(/\/ws\/print\/?$/, '')
    .replace(/\/$/, '');
  url.pathname = `${contextPath}/${normalizeTemplateSource(templateSource)}/template`;
  url.search = '';
  url.hash = '';
  return url.toString().replace(/\/$/, '');
}

function normalizeTemplateBaseUrl(templateBaseUrl, serverUrl, templateSource) {
  if (!serverUrl) {
    return '';
  }

  if (!templateBaseUrl) {
    return deriveTemplateBaseUrl(serverUrl, templateSource);
  }

  const url = new URL(templateBaseUrl);
  const templatePathPattern = /\/(?:(?:qiniu|minio)\/)?template\/?$/;

  if (!templatePathPattern.test(url.pathname)) {
    return deriveTemplateBaseUrl(serverUrl, templateSource);
  }

  url.pathname = url.pathname.replace(templatePathPattern, `/${templateSource}/template`);
  url.search = '';
  url.hash = '';
  return url.toString().replace(/\/$/, '');
}

function normalizeTemplateSource(templateSource) {
  return TEMPLATE_SOURCES.has(templateSource) ? templateSource : DEFAULT_TEMPLATE_SOURCE;
}

module.exports = {
  DEFAULT_CONFIG,
  configureUserConfigPath,
  deriveServerBaseUrl,
  deriveTemplateBaseUrl,
  deriveWebSocketUrl,
  loadConfig,
  saveConfig,
  resolveConfigPath
};
