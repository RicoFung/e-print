'use strict';

const buildEnvironment = String(process.env.E_PRINT_BUILD_ENV || '').toLowerCase();

if (!['uat', 'prod'].includes(buildEnvironment)) {
  throw new Error('E_PRINT_BUILD_ENV must be either uat or prod');
}

const productName = buildEnvironment === 'uat' ? 'E-Print-UAT' : 'E-Print';
const executableName = buildEnvironment === 'uat' ? 'e-print-uat' : 'e-print';
const artifactPrefix = buildEnvironment === 'uat' ? 'e-print-uat' : 'e-print';
const credentials = readBuildCredentials(buildEnvironment);

function readBuildCredentials(environment) {
  const environmentPrefix = `E_PRINT_${environment.toUpperCase()}_BASIC`;
  const username = process.env[`${environmentPrefix}_USERNAME`]
    || process.env.E_PRINT_BASIC_USERNAME;
  const password = process.env[`${environmentPrefix}_PASSWORD`]
    || process.env.E_PRINT_BASIC_PASSWORD;

  if (!username || !password) {
    throw new Error(
      `Missing Basic credentials for ${environment}. Set ${environmentPrefix}_USERNAME and `
      + `${environmentPrefix}_PASSWORD (or E_PRINT_BASIC_USERNAME and E_PRINT_BASIC_PASSWORD).`
    );
  }

  return { username, password };
}

module.exports = {
  appId: `com.eprint.client.${buildEnvironment}`,
  productName,
  extraMetadata: {
    ePrintEnvironment: buildEnvironment,
    ePrintBasicUsername: credentials.username,
    ePrintBasicPassword: credentials.password
  },
  directories: {
    output: `dist/${buildEnvironment}`
  },
  files: [
    'src/**/*',
    'assets/**/*',
    'package.json'
  ],
  asar: true,
  win: {
    target: ['nsis'],
    executableName,
    icon: 'assets/e-print-icon.ico'
  },
  nsis: {
    oneClick: false,
    allowToChangeInstallationDirectory: true,
    createDesktopShortcut: true,
    createStartMenuShortcut: true,
    shortcutName: productName,
    include: `build/installer-${buildEnvironment}.nsh`,
    artifactName: `${artifactPrefix}-setup-\${version}.\${ext}`
  },
  portable: {
    artifactName: `${artifactPrefix}-portable-\${version}.\${ext}`
  }
};
