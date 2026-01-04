import { defineConfig } from 'cypress'

export default defineConfig({
    chromeWebSecurity: false,
    defaultCommandTimeout: 30000,
    reporter: 'cypress-multi-reporters',
    reporterOptions: {
        configFile: 'reporter-config.json',
    },
    screenshotsFolder: './results/screenshots',
    videosFolder: './results/videos',
    viewportWidth: 1366,
    viewportHeight: 768,
    watchForFileChanges: false,
    e2e: {
        // Reference the support file for global setup and custom commands
        supportFile: 'cypress/support/e2e.js',
        setupNodeEvents(on, config) {
            // eslint-disable-next-line @typescript-eslint/no-var-requires
            return require('./cypress/plugins/index.js')(on, config)
        },
        excludeSpecPattern: '*.example.ts',
    },
})
