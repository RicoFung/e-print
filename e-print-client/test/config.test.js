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
    templateBaseUrl: 'http://localhost:8080/api/templates'
  }), 'utf8');

  const config = loadConfig(configPath);

  assert.equal(config.serverUrl, 'ws://localhost:9090/ws/print');
  assert.equal(config.templateBaseUrl, 'http://localhost:9090/api/templates');
});

test('keeps custom configured server URLs', () => {
  const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'e-print-client-'));
  const configPath = path.join(configDir, 'config.json');

  fs.writeFileSync(configPath, JSON.stringify({
    serverUrl: 'ws://192.168.1.20:8080/ws/print',
    templateBaseUrl: 'http://192.168.1.20:8080/api/templates'
  }), 'utf8');

  const config = loadConfig(configPath);

  assert.equal(config.serverUrl, 'ws://192.168.1.20:8080/ws/print');
  assert.equal(config.templateBaseUrl, 'http://192.168.1.20:8080/api/templates');
});

test('uses project config by default', () => {
  assert.match(resolveConfigPath(), /e-print-client[\\/]config\.json$/);
});

test('derives template API URL from websocket URL', () => {
  assert.equal(
    deriveTemplateBaseUrl('wss://print.example.com/ws/print'),
    'https://print.example.com/api/templates'
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
