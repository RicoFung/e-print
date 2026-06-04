'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const test = require('node:test');
const { deriveTemplateBaseUrl, loadConfig, resolveConfigPath, saveConfig } = require('../src/lib/config');

test('migrates legacy default server URLs to current port', () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-'));
  const configPath = path.join(configDir, 'config.json');

  fs.writeFileSync(configPath, JSON.stringify({
    serverUrl: 'ws://localhost:8080/ws/print',
    templateBaseUrl: 'http://localhost:8080/template'
  }), 'utf8');

  const config = loadConfig(configPath);

  assert.equal(config.serverUrl, 'ws://localhost:9090/ws/print');
  assert.equal(config.templateBaseUrl, 'http://localhost:9090/template');
  assert.equal(config.basicUsername, 'eprint');
  assert.equal(config.basicPassword, 'eprint123');
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
  assert.equal(config.templateBaseUrl, 'http://192.168.1.20:8080/template');
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
        serverUrl: 'wss://uat-print.example.com/ws/print',
        basic: {
          username: 'uat-user',
          password: 'uat-password'
        }
      }
    }
  }), 'utf8');

  const config = loadConfig(configPath);

  assert.equal(config.env, 'uat');
  assert.equal(config.serverUrl, 'wss://uat-print.example.com/ws/print');
  assert.equal(config.templateBaseUrl, 'https://uat-print.example.com/template');
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

  process.env.E_PRINT_SERVER_URL = 'wss://print.example.com/ws/print';
  delete process.env.E_PRINT_TEMPLATE_BASE_URL;

  try {
    const config = loadConfig(configPath);

    assert.equal(config.serverUrl, 'wss://print.example.com/ws/print');
    assert.equal(config.templateBaseUrl, 'https://print.example.com/template');
  } finally {
    restoreEnv('E_PRINT_SERVER_URL', previousServerUrl);
    restoreEnv('E_PRINT_TEMPLATE_BASE_URL', previousTemplateBaseUrl);
  }
});

test('uses project config by default', () => {
  assert.match(resolveConfigPath(), /e-print-client[\\/]config\.json$/);
});

test('derives template API URL from websocket URL', () => {
  assert.equal(
    deriveTemplateBaseUrl('wss://print.example.com/ws/print'),
    'https://print.example.com/template'
  );
});

test('saves printer configuration', () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-'));
  const configPath = path.join(configDir, 'config.json');

  saveConfig({
    serverUrl: 'ws://localhost:9090/ws/print',
    printerName: 'Zebra ZD230',
    silent: false
  }, configPath);

  const saved = JSON.parse(fs.readFileSync(configPath, 'utf8'));

  assert.equal(saved.printerName, 'Zebra ZD230');
  assert.equal(saved.silent, false);
});

function restoreEnv(name, value) {
  if (value === undefined) {
    delete process.env[name];
    return;
  }

  process.env[name] = value;
}
