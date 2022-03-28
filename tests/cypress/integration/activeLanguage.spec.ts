import { waitUntilRefresh } from '../utils/waitUntilRefresh'
import { configureSitemap } from '../utils/configureSitemap'
import { deleteSitemapCache } from '../utils/deleteSitemapCache'

const siteKey = 'digitall'
const sitePath = `/sites/${siteKey}`
const siteMapRootUrl = `${Cypress.config().baseUrl}${sitePath}`
const sitemapUrl = `${siteMapRootUrl}/sitemap.xml`

const filterLang = 'de'
const filterPath = `/${filterLang}${sitePath}/`

describe('Testing sitemap only contains language', () => {
    let filteredUrlsforLang = 0;
    before('Configure sitemap', () => {
        configureSitemap(sitePath, siteMapRootUrl)

        cy.log(`Verify sitemap is configured properly for site: ${sitePath}`)
        cy.apollo({
            variables: {
                pathOrId: sitePath,
                mixinsFilter: { filters: [{ fieldName: 'name', value: 'jseomix:sitemap' }] },
                propertyNames: ['sitemapIndexURL', 'sitemapCacheDuration'],
            },
            queryFile: 'graphql/jcrGetSitemapConfig.graphql',
        }).should((response: any) => {
            const r = response?.data?.jcr?.nodeByPath
            cy.log(JSON.stringify(r))
            expect(r.id).not.to.be.null
            expect(r.mixinTypes[0].name).to.equal('jseomix:sitemap')
            expect(r.name).to.equal(siteKey)
        })
    })

    // Before removing the language, verify the sitemap does contain
    // links with language filterLang
    it(`Verify that the sitemap container pages with language: ${filterLang}`, function () {
        cy.task('parseSitemap', { url: sitemapUrl }).then((urls: Array<string>) => {
            cy.log(`Sitemap contains: ${urls.length} URLs in total`)
            expect(urls.length).to.be.greaterThan(0)

            const filteredLang = urls.filter((u) => u.includes(filterPath))
            cy.log(`Sitemap contains ${filteredLang.length} URLs with path: ${filterPath}`)
            filteredUrlsforLang = filteredLang.length;
            expect(filteredLang.length).to.be.greaterThan(0)
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

            // Wait until the sitemap is modified
            waitUntilRefresh(sitemapUrl, originalSitemapUrls)

            // Fetch the new sitemaps again and test the result
            cy.task('parseSitemap', { url: sitemapUrl }).then((newSitemapUrls: Array<string>) => {
                cy.log(`The updated sitemap contains ${newSitemapUrls.length} URLs`)

                const filteredLang = newSitemapUrls.filter((u) => u.includes(filterPath))
                cy.log(`Sitemap contains ${filteredLang.length} URLs with path: ${filterPath}`)
                expect(filteredLang.length).to.be.greaterThan(0)

                cy.log(`It should container the same number of URLs than before disabling the language: ${filteredUrlsforLang}`)
                expect(filteredLang.length).to.equal(filteredUrlsforLang)
            })
        })
    })
})
