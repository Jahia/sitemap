const parseSitemap = require('./parseSitemap')

module.exports = (on, config) => {
    //https://github.com/archfz/cypress-terminal-report
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    require('@jahia/cypress/dist/plugins/registerPlugins').registerPlugins(on, config)

    const optionsPrinter = {
        printLogsToConsole: 'always',
        includeSuccessfulHookLogs: true,
    }
    require('cypress-terminal-report/src/installLogsPrinter')(on, optionsPrinter)

    on('task', {
        parseSitemap(params) {
            console.log(params)
            return parseSitemap(params)
        },
    })

    return config;
};
