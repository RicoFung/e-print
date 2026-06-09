'use strict';

function normalizeTask(raw) {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) {
    throw new Error('print task must be an object');
  }

  const task = {
    taskId: raw.taskId || raw.id || null,
    clientId: requiredString(raw.clientId, 'clientId'),
    templateType: requiredString(raw.templateType, 'templateType'),
    templateCode: requiredString(raw.templateCode, 'templateCode'),
    copies: normalizeCopies(raw.copies),
    printerName: raw.printerName || null,
    data: raw.data && typeof raw.data === 'object' && !Array.isArray(raw.data) ? raw.data : {}
  };

  return task;
}

function parseTaskMessage(message) {
  const payload = typeof message === 'string' ? JSON.parse(message) : message;

  if (payload && payload.type === 'print-task') {
    return normalizeTask(payload.payload || payload.task);
  }

  return normalizeTask(payload);
}

function createResult(task, status, extra) {
  return {
    type: 'print-result',
    taskId: task.taskId,
    clientId: task.clientId,
    templateType: task.templateType,
    templateCode: task.templateCode,
    status,
    message: extra && extra.message ? String(extra.message) : undefined,
    printedAt: new Date().toISOString()
  };
}

function requiredString(value, field) {
  if (typeof value !== 'string' || value.trim() === '') {
    throw new Error(`${field} is required`);
  }

  return value.trim();
}

function normalizeCopies(value) {
  if (value === undefined || value === null) {
    return 1;
  }

  const copies = Number(value);
  if (!Number.isInteger(copies) || copies < 1 || copies > 100) {
    throw new Error('copies must be an integer between 1 and 100');
  }

  return copies;
}

module.exports = {
  createResult,
  normalizeTask,
  parseTaskMessage
};
