// eslint-disable-next-line @typescript-eslint/no-var-requires
const cypressTypeScriptPreprocessor = require("./cy-ts-preprocessor");

/* eslint-disable @typescript-eslint/no-var-requires */
const env = require('./env')

module.exports = (on, config) => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    require("@cypress/code-coverage/task")(on, config);

    //https://github.com/archfz/cypress-terminal-report
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    require('@jahia/cypress/dist/plugins/registerPlugins').registerPlugins(on, config)

    env(on, config)

    on("file:preprocessor", cypressTypeScriptPreprocessor);

    const optionsPrinter = {
        printLogsToConsole: 'always',
        includeSuccessfulHookLogs: true,
    }
    require('cypress-terminal-report/src/installLogsPrinter')(on, optionsPrinter)

    return config;
};
