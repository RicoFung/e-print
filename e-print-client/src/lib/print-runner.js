'use strict';

const { createResult, normalizeTask } = require('./task');
const { getTemplate } = require('./template-cache');
const { renderTemplate } = require('./template-renderer');

async function runPrintTask(rawTask, config, dependencies) {
  const deps = dependencies || {};
  const task = normalizeTask(rawTask);

  try {
    const templateHtml = await (deps.getTemplate || getTemplate)(task.templateType, task.templateCode, config, {
      ...deps,
      forceRefresh: true
    });
    const renderedHtml = await (deps.renderTemplate || renderTemplate)(templateHtml, task.data, deps);
    await deps.printer.print(renderedHtml, {
      copies: task.copies,
      printerName: task.printerName || config.printerName,
      silent: config.silent !== false
    });

    const result = createResult(task, 'success');
    await reportResult(result, deps);
    return result;
  } catch (error) {
    const result = createResult(task, 'failed', {
      message: error && error.message ? error.message : String(error)
    });
    await reportResult(result, deps);
    return result;
  }
}

async function reportResult(result, dependencies) {
  if (dependencies && typeof dependencies.reportResult === 'function') {
    await dependencies.reportResult(result);
  }
}

module.exports = {
  runPrintTask
};
