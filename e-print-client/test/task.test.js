'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const { createResult, normalizeTask, parseTaskMessage } = require('../src/lib/task');

test('normalizes valid print task defaults', () => {
  const task = normalizeTask({
    clientId: ' CLIENT-001 ',
    templateCode: 'product-label',
    data: {
      sku: 'MBP-001'
    }
  });

  assert.equal(task.clientId, 'CLIENT-001');
  assert.equal(task.templateCode, 'product-label');
  assert.equal(task.copies, 1);
  assert.deepEqual(task.data, { sku: 'MBP-001' });
});

test('parses wrapped websocket print-task message', () => {
  const task = parseTaskMessage(JSON.stringify({
    type: 'print-task',
    payload: {
      id: 'TASK-1',
      clientId: 'CLIENT-001',
      templateCode: 'product-label',
      copies: 2
    }
  }));

  assert.equal(task.taskId, 'TASK-1');
  assert.equal(task.copies, 2);
});

test('rejects invalid copies', () => {
  assert.throws(() => normalizeTask({
    clientId: 'CLIENT-001',
    templateCode: 'product-label',
    copies: 0
  }), /copies/);
});

test('creates result payload for server callback', () => {
  const result = createResult({
    taskId: 'TASK-1',
    clientId: 'CLIENT-001',
    templateCode: 'product-label'
  }, 'success');

  assert.equal(result.type, 'print-result');
  assert.equal(result.status, 'success');
  assert.match(result.printedAt, /^\d{4}-\d{2}-\d{2}T/);
});
