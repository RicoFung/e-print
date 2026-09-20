'use strict';

const assert = require('node:assert/strict');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const test = require('node:test');

const projectDir = path.resolve(__dirname, '..');
const inspectScript = [
  "const config = require('./electron-builder.config');",
  'process.stdout.write(JSON.stringify(config.extraMetadata));'
].join(' ');

test('embeds generic Basic credentials for a single-environment build', () => {
  const result = loadBuildConfig('uat', {
    E_PRINT_BASIC_USERNAME: 'single-user',
    E_PRINT_BASIC_PASSWORD: 'single-password'
  });

  assert.equal(result.status, 0, result.stderr);
  assert.deepEqual(JSON.parse(result.stdout), {
    ePrintEnvironment: 'uat',
    ePrintBasicUsername: 'single-user',
    ePrintBasicPassword: 'single-password'
  });
  assert.equal(readBuildProperty('uat', 'nsis.include'), 'build/installer-uat.nsh');
});

test('selects environment-specific Basic credentials', () => {
  const commonEnvironment = {
    E_PRINT_UAT_BASIC_USERNAME: 'uat-user',
    E_PRINT_UAT_BASIC_PASSWORD: 'uat-password',
    E_PRINT_PROD_BASIC_USERNAME: 'prod-user',
    E_PRINT_PROD_BASIC_PASSWORD: 'prod-password'
  };

  const uat = loadBuildConfig('uat', commonEnvironment);
  const prod = loadBuildConfig('prod', commonEnvironment);

  assert.equal(uat.status, 0, uat.stderr);
  assert.equal(prod.status, 0, prod.stderr);
  assert.equal(JSON.parse(uat.stdout).ePrintBasicUsername, 'uat-user');
  assert.equal(JSON.parse(uat.stdout).ePrintBasicPassword, 'uat-password');
  assert.equal(JSON.parse(prod.stdout).ePrintBasicUsername, 'prod-user');
  assert.equal(JSON.parse(prod.stdout).ePrintBasicPassword, 'prod-password');
  assert.equal(readBuildProperty('prod', 'nsis.include', commonEnvironment), 'build/installer-prod.nsh');
});

test('rejects a build without Basic credentials', () => {
  const result = loadBuildConfig('prod');

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /Missing Basic credentials for prod/);
});

function loadBuildConfig(environment, overrides = {}) {
  const env = { ...process.env };
  for (const name of [
    'E_PRINT_BASIC_USERNAME',
    'E_PRINT_BASIC_PASSWORD',
    'E_PRINT_UAT_BASIC_USERNAME',
    'E_PRINT_UAT_BASIC_PASSWORD',
    'E_PRINT_PROD_BASIC_USERNAME',
    'E_PRINT_PROD_BASIC_PASSWORD'
  ]) {
    delete env[name];
  }

  return spawnSync(process.execPath, ['-e', inspectScript], {
    cwd: projectDir,
    env: {
      ...env,
      ...overrides,
      E_PRINT_BUILD_ENV: environment
    },
    encoding: 'utf8'
  });
}

function readBuildProperty(environment, propertyPath, overrides = {
  E_PRINT_BASIC_USERNAME: 'test-user',
  E_PRINT_BASIC_PASSWORD: 'test-password'
}) {
  const segments = propertyPath.split('.');
  const propertyScript = [
    "const config = require('./electron-builder.config');",
    `process.stdout.write(JSON.stringify(${JSON.stringify(segments)}.reduce((value, key) => value[key], config)));`
  ].join(' ');
  const env = { ...process.env, ...overrides, E_PRINT_BUILD_ENV: environment };
  const result = spawnSync(process.execPath, ['-e', propertyScript], {
    cwd: projectDir,
    env,
    encoding: 'utf8'
  });
  assert.equal(result.status, 0, result.stderr);
  return JSON.parse(result.stdout);
}
