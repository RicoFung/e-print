'use strict';

const APP_IDENTITIES = Object.freeze({
  loc: Object.freeze({
    appId: 'com.eprint.client.loc',
    productName: 'E-Print',
    executableName: 'E-Print',
    artifactPrefix: 'e-print'
  }),
  uat: Object.freeze({
    appId: 'com.eprint.client.uat',
    productName: 'E-Print-UAT',
    executableName: 'E-Print-UAT',
    artifactPrefix: 'e-print-uat'
  }),
  prod: Object.freeze({
    appId: 'com.eprint.client.prod',
    productName: 'E-Print',
    executableName: 'E-Print',
    artifactPrefix: 'e-print'
  })
});

function getAppIdentity(environment) {
  const normalizedEnvironment = String(environment || 'loc').toLowerCase();
  const identity = APP_IDENTITIES[normalizedEnvironment];

  if (!identity) {
    throw new Error(`Unsupported E-Print environment: ${environment}`);
  }

  return identity;
}

module.exports = {
  getAppIdentity
};
