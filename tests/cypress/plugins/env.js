module.exports = (on, config) => {
    console.log('Setting additional environment variables')
    config.env.JAHIA_PROCESSING_URL = process.env.JAHIA_PROCESSING_URL

    console.log('JAHIA_PROCESSING_URL =', config.env.JAHIA_PROCESSING_URL)
    return config
}