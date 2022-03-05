module.exports = (on, config) => {
    console.log('Setting additional environment variables')
    config.env.JAHIA_PROCESSING_URL = process.env.JAHIA_PROCESSING_URL
    config.env.JAHIA_USERNAME = process.env.JAHIA_USERNAME
    config.env.JAHIA_PASSWORD = process.env.JAHIA_PASSWORD

    console.log('JAHIA_PROCESSING_URL =', config.env.JAHIA_PROCESSING_URL)
    return config
}