'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const { runPrintTask } = require('../src/lib/print-runner');

test('runs full print flow and reports success', async () => {
  const printed = [];
  const reports = [];

  const result = await runPrintTask({
    taskId: 'TASK-1',
    clientId: 'CLIENT-001',
    templateCode: 'product-label',
    copies: 2,
    data: {
      productName: 'MacBook Pro'
    }
  }, {
    printerName: 'Zebra',
    silent: true
  }, {
    getTemplate: async () => '<div>{{productName}}</div>',
    renderTemplate: async (template, data) => template.replace('{{productName}}', data.productName),
    printer: {
      print: async (html, options) => printed.push({ html, options })
    },
    reportResult: async (payload) => reports.push(payload)
  });

  assert.equal(result.status, 'success');
  assert.equal(printed[0].html, '<div>MacBook Pro</div>');
  assert.deepEqual(printed[0].options, {
    copies: 2,
    printerName: 'Zebra',
    silent: true
  });
  assert.equal(reports[0].status, 'success');
});

test('reports failed result when printer throws', async () => {
  const reports = [];
  const result = await runPrintTask({
    clientId: 'CLIENT-001',
    templateCode: 'product-label'
  }, {}, {
    getTemplate: async () => '<div>ok</div>',
    renderTemplate: async () => '<div>ok</div>',
    printer: {
      print: async () => {
        throw new Error('printer offline');
      }
    },
    reportResult: async (payload) => reports.push(payload)
  });

  assert.equal(result.status, 'failed');
  assert.match(result.message, /printer offline/);
  assert.equal(reports[0].status, 'failed');
});
