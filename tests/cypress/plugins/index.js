/* eslint-disable @typescript-eslint/no-var-requires */
const env = require('./env')

module.exports = (on, config) => {
    // base jahia cypress plugins
    require('@jahia/cypress/dist/plugins/registerPlugins').registerPlugins(on, config)


    // Load env variables
    env(on, config)

    const optionsPrinter = {
        printLogsToConsole: 'always',
        includeSuccessfulHookLogs: true,
    }
    require('cypress-terminal-report/src/installLogsPrinter')(on, optionsPrinter)

    return config
}