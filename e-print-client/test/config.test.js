'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const test = require('node:test');
const {
  configureUserConfigPath,
  deriveServerBaseUrl,
  deriveTemplateBaseUrl,
  deriveWebSocketUrl,
  loadConfig,
  resolveConfigPath,
  saveConfig
} = require('../src/lib/config');

test('migrates legacy default server URLs to current context path', () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-'));
  const configPath = path.join(configDir, 'config.json');

  fs.writeFileSync(configPath, JSON.stringify({
    serverUrl: 'ws://localhost:8080/ws/print',
    templateBaseUrl: 'http://localhost:8080/template'
  }), 'utf8');

  const config = loadConfig(configPath);

  assert.equal(config.serverUrl, 'ws://localhost:8080/e-print-server/ws/print');
  assert.equal(config.templateSource, 'qiniu');
  assert.equal(config.templateBaseUrl, 'http://localhost:8080/e-print-server/qiniu/template');
  assert.equal(config.basicUsername, '');
  assert.equal(config.basicPassword, undefined);
});

test('migrates original 9090 server URLs to current port and context path', () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-'));
  const configPath = path.join(configDir, 'config.json');

  fs.writeFileSync(configPath, JSON.stringify({
    serverUrl: 'ws://localhost:9090/ws/print',
    templateBaseUrl: 'http://localhost:9090/template'
  }), 'utf8');

  const config = loadConfig(configPath);

  assert.equal(config.serverUrl, 'ws://localhost:8080/e-print-server/ws/print');
  assert.equal(config.templateBaseUrl, 'http://localhost:8080/e-print-server/qiniu/template');
});

test('keeps custom configured server URLs', () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-'));
  const configPath = path.join(configDir, 'config.json');

  fs.writeFileSync(configPath, JSON.stringify({
    serverUrl: 'ws://192.168.1.20:8080/ws/print',
    templateBaseUrl: 'http://192.168.1.20:8080/template'
  }), 'utf8');

  const config = loadConfig(configPath);

  assert.equal(config.serverUrl, 'ws://192.168.1.20:8080/ws/print');
  assert.equal(config.templateBaseUrl, 'http://192.168.1.20:8080/qiniu/template');
});

test('uses basic auth from selected environment', () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-'));
  const configPath = path.join(configDir, 'config.json');

  fs.writeFileSync(configPath, JSON.stringify({
    env: 'uat',
    environments: {
      loc: {
        basicUsername: 'loc-user',
        basicPassword: 'loc-password'
      },
      uat: {
        serverUrl: 'wss://uat-print.example.com/e-print-server/ws/print',
        basic: {
          username: 'uat-user',
          password: 'uat-password'
        }
      }
    }
  }), 'utf8');

  const config = loadConfig(configPath);

  assert.equal(config.env, 'uat');
  assert.equal(config.serverUrl, 'wss://uat-print.example.com/e-print-server/ws/print');
  assert.equal(config.templateBaseUrl, 'https://uat-print.example.com/e-print-server/qiniu/template');
  assert.equal(config.basicUsername, 'uat-user');
  assert.equal(config.basicPassword, 'uat-password');
});

test('environment variables override selected environment basic auth', () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-'));
  const configPath = path.join(configDir, 'config.json');
  const previousEnv = process.env.E_PRINT_ENV;
  const previousUsername = process.env.E_PRINT_BASIC_USERNAME;
  const previousPassword = process.env.E_PRINT_BASIC_PASSWORD;

  fs.writeFileSync(configPath, JSON.stringify({
    env: 'loc',
    environments: {
      loc: {
        basicUsername: 'loc-user',
        basicPassword: 'loc-password'
      },
      prod: {
        basicUsername: 'prod-user',
        basicPassword: 'prod-password'
      }
    }
  }), 'utf8');

  process.env.E_PRINT_ENV = 'prod';
  process.env.E_PRINT_BASIC_USERNAME = 'override-user';
  process.env.E_PRINT_BASIC_PASSWORD = 'override-password';

  try {
    const config = loadConfig(configPath);

    assert.equal(config.env, 'prod');
    assert.equal(config.basicUsername, 'override-user');
    assert.equal(config.basicPassword, 'override-password');
  } finally {
    restoreEnv('E_PRINT_ENV', previousEnv);
    restoreEnv('E_PRINT_BASIC_USERNAME', previousUsername);
    restoreEnv('E_PRINT_BASIC_PASSWORD', previousPassword);
  }
});

test('derives template API URL when server URL is overridden by environment variable', () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-'));
  const configPath = path.join(configDir, 'config.json');
  const previousServerUrl = process.env.E_PRINT_SERVER_URL;
  const previousTemplateBaseUrl = process.env.E_PRINT_TEMPLATE_BASE_URL;

  fs.writeFileSync(configPath, JSON.stringify({}), 'utf8');

  process.env.E_PRINT_SERVER_URL = 'wss://print.example.com/e-print-server/ws/print';
  delete process.env.E_PRINT_TEMPLATE_BASE_URL;

  try {
    const config = loadConfig(configPath);

    assert.equal(config.serverUrl, 'wss://print.example.com/e-print-server/ws/print');
    assert.equal(config.templateBaseUrl, 'https://print.example.com/e-print-server/qiniu/template');
  } finally {
    restoreEnv('E_PRINT_SERVER_URL', previousServerUrl);
    restoreEnv('E_PRINT_TEMPLATE_BASE_URL', previousTemplateBaseUrl);
  }
});

test('uses project config by default', () => {
  assert.match(resolveConfigPath(), /e-print-client[\\/]config\.json$/);
});

test('uses configured user data path for runtime config', () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-user-data-'));
  const configPath = configureUserConfigPath(configDir);

  try {
    assert.equal(resolveConfigPath(), path.join(configDir, 'config.json'));
    assert.equal(configPath, path.join(configDir, 'config.json'));
  } finally {
    configureUserConfigPath(path.resolve(__dirname, '..'));
  }
});

test('environment config path overrides configured user data path', () => {
  const userDataDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-user-data-'));
  const envConfigDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-env-config-'));
  const envConfigPath = path.join(envConfigDir, 'custom-config.json');
  const previousConfigPath = process.env.E_PRINT_CONFIG_PATH;

  configureUserConfigPath(userDataDir);
  process.env.E_PRINT_CONFIG_PATH = envConfigPath;

  try {
    assert.equal(resolveConfigPath(), envConfigPath);
  } finally {
    restoreEnv('E_PRINT_CONFIG_PATH', previousConfigPath);
    configureUserConfigPath(path.resolve(__dirname, '..'));
  }
});

test('loads bundled project config as initial user config template', () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-user-data-'));
  const configPath = configureUserConfigPath(configDir);

  try {
    const config = loadConfig();

    assert.equal(configPath, path.join(configDir, 'config.json'));
    assert.equal(config.clientId, 'CLIENT-001');
    assert.equal(config.basicUsername, 'eprint');
    assert.equal(config.basicPassword, 'eprint123');
    assert.equal(config.templateCacheDir, path.join(configDir, 'templates'));
  } finally {
    configureUserConfigPath(path.resolve(__dirname, '..'));
  }
});

test('migrates a legacy user-specific template cache path to runtime user data', () => {
  const userDataDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-user-data-'));
  const configPath = configureUserConfigPath(userDataDir);

  fs.writeFileSync(configPath, JSON.stringify({
    templateCacheDir: 'C:\\Users\\developer\\.e-print-client\\templates'
  }), 'utf8');

  try {
    const config = loadConfig();

    assert.equal(config.templateCacheDir, path.join(userDataDir, 'templates'));
  } finally {
    configureUserConfigPath(path.resolve(__dirname, '..'));
  }
});

test('keeps an explicitly configured custom template cache path', () => {
  const userDataDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-user-data-'));
  const customCacheDir = path.join(os.tmpdir(), 'shared-e-print-templates');
  const configPath = configureUserConfigPath(userDataDir);

  fs.writeFileSync(configPath, JSON.stringify({
    templateCacheDir: customCacheDir
  }), 'utf8');

  try {
    const config = loadConfig();

    assert.equal(config.templateCacheDir, customCacheDir);
  } finally {
    configureUserConfigPath(path.resolve(__dirname, '..'));
  }
});

test('derives template API URL from websocket URL', () => {
  assert.equal(
    deriveTemplateBaseUrl('wss://print.example.com/e-print-server/ws/print'),
    'https://print.example.com/e-print-server/qiniu/template'
  );
  assert.equal(
    deriveTemplateBaseUrl('wss://print.example.com/e-print-server/ws/print', 'minio'),
    'https://print.example.com/e-print-server/minio/template'
  );
});

test('derives websocket and API URLs from a server base address', () => {
  const serverBaseUrl = 'wss://apiuat.moco.com/eprint/v1';

  assert.equal(
    deriveWebSocketUrl(serverBaseUrl),
    'wss://apiuat.moco.com/eprint/v1/ws/print'
  );
  assert.equal(
    deriveServerBaseUrl('wss://apiuat.moco.com/eprint/v1/ws/print'),
    serverBaseUrl
  );
  assert.equal(
    deriveTemplateBaseUrl(deriveWebSocketUrl(serverBaseUrl), 'qiniu'),
    'https://apiuat.moco.com/eprint/v1/qiniu/template'
  );
});

test('allows an empty first-run server configuration', () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-'));
  const configPath = path.join(configDir, 'config.json');
  fs.writeFileSync(configPath, '{}', 'utf8');

  const config = loadConfig(configPath);

  assert.equal(config.serverUrl, '');
  assert.equal(config.templateBaseUrl, '');
  assert.equal(config.basicUsername, '');
  assert.equal(config.basicPassword, undefined);
  assert.equal(config.silent, false);
});

test('migrates the legacy silent-print default to preview mode', () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-'));
  const configPath = path.join(configDir, 'config.json');
  fs.writeFileSync(configPath, JSON.stringify({ silent: true }), 'utf8');

  const config = loadConfig(configPath);

  assert.equal(config.configVersion, 2);
  assert.equal(config.silent, false);
});

test('keeps an explicit print mode after the preview-default migration', () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-'));
  const configPath = path.join(configDir, 'config.json');
  fs.writeFileSync(configPath, JSON.stringify({
    configVersion: 2,
    silent: true
  }), 'utf8');

  const config = loadConfig(configPath);

  assert.equal(config.silent, true);
});

test('uses template source from selected environment', () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-'));
  const configPath = path.join(configDir, 'config.json');

  fs.writeFileSync(configPath, JSON.stringify({
    env: 'uat',
    environments: {
      uat: {
        serverUrl: 'wss://uat-print.example.com/e-print-server/ws/print',
        templateSource: 'minio'
      }
    }
  }), 'utf8');

  const config = loadConfig(configPath);

  assert.equal(config.templateSource, 'minio');
  assert.equal(config.templateBaseUrl, 'https://uat-print.example.com/e-print-server/minio/template');
});

test('saves printer configuration', () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-'));
  const configPath = path.join(configDir, 'config.json');

  saveConfig({
    serverUrl: 'ws://localhost:8080/e-print-server/ws/print',
    printerName: 'Zebra ZD230',
    silent: false
  }, configPath);

  const saved = JSON.parse(fs.readFileSync(configPath, 'utf8'));

  assert.equal(saved.printerName, 'Zebra ZD230');
  assert.equal(saved.silent, false);
  assert.equal(saved.templateSource, 'qiniu');
  assert.equal(saved.templateBaseUrl, 'http://localhost:8080/e-print-server/qiniu/template');
});

function restoreEnv(name, value) {
  if (value === undefined) {
    delete process.env[name];
    return;
  }

  process.env[name] = value;
}
