import {waitUntilRefresh} from '../utils/waitUntilRefresh'
import {configureSitemap} from '../utils/configureSitemap'
import {deleteSitemapCache} from '../utils/deleteSitemapCache'

const siteKey = 'digitall'
const sitePath = `/sites/${siteKey}`
const siteMapRootUrl = `${Cypress.config().baseUrl}${sitePath}`
const sitemapUrl = `${siteMapRootUrl}/sitemap.xml`

const filterLang = 'de'
const filterPath = `/${filterLang}${sitePath}/`

describe('Testing sitemap only contains language', () => {
    let filteredUrlsforLang = 0
    before('Configure sitemap', () => {
        configureSitemap(sitePath, siteMapRootUrl, Cypress.config().baseUrl)

        cy.log(`Verify sitemap is configured properly for site: ${sitePath}`)
        cy.apollo({
            variables: {
                pathOrId: sitePath,
                mixinsFilter: { filters: [{ fieldName: 'name', value: 'jseomix:sitemap' }] },
                propertyNames: ['sitemapIndexURL', 'sitemapCacheDuration'],
            },
            queryFile: 'graphql/jcrGetSitemapConfig.graphql',
        }).should((response) => {
            const r = response?.data?.jcr?.nodeByPath
            cy.log(JSON.stringify(r))
            expect(r.id).not.to.be.null
            expect(r.mixinTypes[0].name).to.equal('jseomix:sitemap')
            expect(r.name).to.equal(siteKey)
        })

        // Flush the cache to force a refresh
        deleteSitemapCache(siteKey)

        // Wait until the sitemap contains some URLs
        waitUntilRefresh(sitemapUrl, [], 150)
    })

    // Before removing the language, verify the sitemap does contain
    // links with language corresponding to filterLang
    it(`Verify that the sitemap contains pages with language: ${filterLang}`, function () {
        cy.task('parseSitemap', { url: sitemapUrl }).then((urls: Array<string>) => {
            cy.log(`Sitemap contains: ${urls.length} URLs in total`)
            expect(urls.length).to.be.greaterThan(0)

            // If this suite is the first executed, it might happen that not all languages
            // have been published by the time this test is executed. This will wait until the sitemap
            // contains pages (at least 10) in the language we are testing
            // This works in conjunction with the before step that will wait until 150 links are present
            // The default for two languages is 118
            const nonLangPages = urls.filter((u) => !u.includes(filterPath))
            waitUntilRefresh(sitemapUrl, nonLangPages, 10)

            cy.task('parseSitemap', { url: sitemapUrl }).then((urls: Array<string>) => {
                const filteredLang = urls.filter((u) => u.includes(filterPath))
                cy.log(`Sitemap contains ${filteredLang.length} URLs with path: ${filterPath}`)
                filteredUrlsforLang = filteredLang.length
                expect(filteredLang.length).to.be.greaterThan(0)
            })
        })
    })

    it(`Disable language: ${filterLang} and verify no such pages are in the sitemap anymore`, function () {
        cy.task('parseSitemap', { url: sitemapUrl }).then((originalSitemapUrls: Array<string>) => {
            cy.log(`Sitemap contains: ${originalSitemapUrls.length} URLs in total`)
            expect(originalSitemapUrls.length).to.be.greaterThan(0)

            // Disable language filterLang
            cy.apollo({
                variables: {
                    pathOrId: sitePath,
                    propertyName: 'j:inactiveLiveLanguages',
                    propertyValues: [filterLang],
                },
                mutationFile: 'graphql/jcrMutateProperties.graphql',
            })

            // Flush cache
            deleteSitemapCache(siteKey)

            // Wait until the sitemap is modified
            waitUntilRefresh(sitemapUrl, originalSitemapUrls)

            // Fetch the new sitemaps again and test the result
            cy.task('parseSitemap', { url: sitemapUrl }).then((newSitemapUrls: Array<string>) => {
                cy.log(`The updated sitemap contains ${newSitemapUrls.length} URLs`)

                cy.log(`The updated sitemap should contain 0 URLs with path: ${filterPath}`)
                const filteredLang = newSitemapUrls.filter((u) => u.includes(filterPath))
                expect(filteredLang.length).to.equal(0)
            })
        })
    })

    it(`Restore all languages for site ${sitePath}`, function () {
        cy.task('parseSitemap', { url: sitemapUrl }).then((originalSitemapUrls: Array<string>) => {
            cy.log(`Sitemap contains: ${originalSitemapUrls.length} URLs in total`)
            expect(originalSitemapUrls.length).to.be.greaterThan(0)

            cy.log(`Restore all languages for site: ${sitePath}`)
            cy.apollo({
                variables: {
                    pathOrId: sitePath,
                    propertyName: 'j:inactiveLiveLanguages',
                    propertyValues: [],
                },
                mutationFile: 'graphql/jcrMutateProperties.graphql',
            })

            // Flush cache
            deleteSitemapCache(siteKey)

            // Wait until the sitemap contains at least 150 urls
            waitUntilRefresh(sitemapUrl, [], 150)

            // Fetch the new sitemaps again and test the result
            cy.task('parseSitemap', { url: sitemapUrl }).then((newSitemapUrls: Array<string>) => {
                cy.log(`The updated sitemap contains ${newSitemapUrls.length} URLs`)

                const filteredLang = newSitemapUrls.filter((u) => u.includes(filterPath))
                cy.log(`Sitemap contains ${filteredLang.length} URLs with path: ${filterPath}`)
                expect(filteredLang.length).to.be.greaterThan(0)

                cy.log(
                    `It should container the same number of URLs than before disabling the language: ${filteredUrlsforLang}`,
                )
                expect(filteredLang.length).to.equal(filteredUrlsforLang)
            })
        })
    })
})
