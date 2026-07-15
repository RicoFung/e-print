'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const {
  createPrintFailureError,
  isPrintCancelledReason,
  PRINT_CANCELLED_CODE,
  PRINT_FAILED_CODE
} = require('../src/printer/electron-printer');

test('detects cancelled print failure reasons', () => {
  assert.equal(isPrintCancelledReason('cancelled'), true);
  assert.equal(isPrintCancelledReason('canceled'), true);
  assert.equal(isPrintCancelledReason('Print job canceled'), true);
  assert.equal(isPrintCancelledReason('用户取消操作'), true);
  assert.equal(isPrintCancelledReason('printer unavailable'), false);
});

test('creates structured print failure errors', () => {
  const cancelled = createPrintFailureError('cancelled');
  assert.equal(cancelled.code, PRINT_CANCELLED_CODE);
  assert.equal(cancelled.message, 'print cancelled');
  assert.equal(cancelled.failureReason, 'cancelled');

  const failed = createPrintFailureError('');
  assert.equal(failed.code, PRINT_FAILED_CODE);
  assert.equal(failed.message, 'print failed');
  assert.equal(failed.failureReason, '');
});
