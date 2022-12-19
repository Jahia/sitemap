import Sitemapper from 'sitemapper'

const parseSitemap = (sitemapUrl: string) => {
    return new Promise((resolve, reject) => {
        const sitemapper = new Sitemapper({
            timeout: 5000,
        })
        return sitemapper
            .fetch(sitemapUrl)
            .then(({ sites }) => {
                console.log(`Fetched sitemap from: ${sitemapUrl} - Contains: ${sites.length} URLs`)
                resolve(sites)
            })
            .catch((error) => {
                console.log(`Unable to fetch sitemap from: ${sitemapUrl}`)
                reject(error)
            })
    })
}

module.exports = parseSitemap
