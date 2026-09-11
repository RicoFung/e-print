'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const {
  buildResultHeaders,
  buildResultUrl,
  reportPrintResult
} = require('../src/lib/result-reporter');

const TEST_CREDENTIALS = {
  basicUsername: ['unit', 'test', 'user'].join('-'),
  basicPassword: ['unit', 'test', 'credential'].join('-')
};

function expectedBasicAuthorization() {
  const credentials = `${TEST_CREDENTIALS.basicUsername}:${TEST_CREDENTIALS.basicPassword}`;
  return `Basic ${Buffer.from(credentials, 'utf8').toString('base64')}`;
}

test('builds print result URL from template base URL', () => {
  assert.equal(
    buildResultUrl('http://localhost:9090/template', 'task/001'),
    'http://localhost:9090/task/task%2F001/result'
  );
});

test('builds basic authorization header for result callback', () => {
  assert.deepEqual(buildResultHeaders(TEST_CREDENTIALS), {
    'content-type': 'application/json',
    accept: 'application/json',
    authorization: expectedBasicAuthorization()
  });
});

test('reports successful print result through HTTP endpoint', async () => {
  let request;
  await reportPrintResult({
    taskId: 'TASK-001',
    templateType: 'sales_receipt',
    templateCode: '01',
    status: 'success'
  }, {
    templateBaseUrl: 'http://localhost:9090/template',
    ...TEST_CREDENTIALS
  }, {
    fetch: async (url, options) => {
      request = { url, options };
      return { ok: true, status: 200 };
    }
  });

  assert.equal(request.url, 'http://localhost:9090/task/TASK-001/result');
  assert.equal(request.options.method, 'POST');
  assert.equal(request.options.headers.authorization, expectedBasicAuthorization());
  assert.deepEqual(JSON.parse(request.options.body), {
    status: 'SUCCESS',
    templateType: 'sales_receipt',
    templateCode: '01'
  });
});

test('throws when result callback returns a non-success status', async () => {
  await assert.rejects(
    reportPrintResult({
      taskId: 'TASK-001',
      status: 'failed'
    }, {
      templateBaseUrl: 'http://localhost:9090/template'
    }, {
      fetch: async () => ({ ok: false, status: 503 })
    }),
    /HTTP 503/
  );
});
