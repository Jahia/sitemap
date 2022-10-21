import {configureSitemap} from "../../utils/configureSitemap";
import {removeSitemapConfiguration} from "../../utils/removeSitemapConfiguration";
import {publishAndWaitJobEnding} from "../../utils/publishAndWaitJobEnding";
import {deleteSitemapCache} from "../../utils/deleteSitemapCache";

const siteKey = 'digitall';
const sitePath = '/sites/' + siteKey;
const homePath = sitePath + '/home';

const createPage = (parent, name, dedicatedSiteMap = undefined) => {
    cy.apollo({
        variables: {
            parentPathOrId: parent,
            name: name,
            template: 'simple',
            language: 'en',
        },
        mutationFile: 'graphql/jcrAddPage.graphql',
    });
    if (dedicatedSiteMap) {
        cy.apollo({
            variables: {
                pathOrId: parent + '/' + name,
                mixinsToAdd: 'jseomix:sitemapResource'
            },
            mutationFile: 'graphql/jcrUpdateNode.graphql',
        })
    }
};

const addVanityUrl = (path, vanity) => {
    cy.apollo({
        variables: {
            pathOrId: path,
            vanityUrls: [{
                active: true,
                defaultMapping: true,
                language: 'en',
                url: vanity
            }]
        },
        mutationFile: 'graphql/addVanityUrl.graphql',
    })
}

describe('Check sitemap links are encoded correctly', () => {
    before('Configure sitemap for the tests', () => {
        // create pages
        createPage(homePath, 'encoding-sitemap-test');
        createPage(homePath + '/encoding-sitemap-test', 'sitemap-roots')
        createPage(homePath + '/encoding-sitemap-test', 'sitemap-pages')
        createPage(homePath + '/encoding-sitemap-test', 'sitemap-vanities')

        // create pages for encoding test: root sitemap
        createPage(homePath + '/encoding-sitemap-test/sitemap-roots', 'root>ü', true)
        createPage(homePath + '/encoding-sitemap-test/sitemap-roots', 'root<ü', true)
        createPage(homePath + '/encoding-sitemap-test/sitemap-roots', 'root&ü', true)
        createPage(homePath + '/encoding-sitemap-test/sitemap-roots', 'root"ü', true)
        createPage(homePath + '/encoding-sitemap-test/sitemap-roots', 'root\'ü', true)
        // create pages for encoding test: pages
        createPage(homePath + '/encoding-sitemap-test/sitemap-pages', 'page>ü', false)
        createPage(homePath + '/encoding-sitemap-test/sitemap-pages', 'page<ü', false)
        createPage(homePath + '/encoding-sitemap-test/sitemap-pages', 'page&ü', false)
        createPage(homePath + '/encoding-sitemap-test/sitemap-pages', 'page"ü', false)
        createPage(homePath + '/encoding-sitemap-test/sitemap-pages', 'page\'ü', false)
        // create pages with vanity for encoding test: vanities
        createPage(homePath + '/encoding-sitemap-test/sitemap-vanities', 'vanity>ü', false)
        addVanityUrl(homePath + '/encoding-sitemap-test/sitemap-vanities/vanity>ü', 'actual-vanity>');
        createPage(homePath + '/encoding-sitemap-test/sitemap-vanities', 'vanity<ü', false)
        addVanityUrl(homePath + '/encoding-sitemap-test/sitemap-vanities/vanity<ü', 'actual-vanity<');
        createPage(homePath + '/encoding-sitemap-test/sitemap-vanities', 'vanity&ü', false)
        addVanityUrl(homePath + '/encoding-sitemap-test/sitemap-vanities/vanity&ü', 'actual-vanity&');
        createPage(homePath + '/encoding-sitemap-test/sitemap-vanities', 'vanity"ü', false)
        addVanityUrl(homePath + '/encoding-sitemap-test/sitemap-vanities/vanity"ü', 'actual-vanity"');
        createPage(homePath + '/encoding-sitemap-test/sitemap-vanities', 'vanity\'ü', false)
        addVanityUrl(homePath + '/encoding-sitemap-test/sitemap-vanities/vanity\'ü', 'actual-vanity\'');

        publishAndWaitJobEnding(homePath + '/encoding-sitemap-test')

        configureSitemap(sitePath, Cypress.config().baseUrl + sitePath)
    })

    after('Remove sitemap configuration via GraphQL', () => {
        cy.apollo({
            variables: {
                pathOrId: homePath + '/encoding-sitemap-test'
            },
            mutationFile: 'graphql/jcrDeleteNode.graphql',
        })
        publishAndWaitJobEnding(homePath)
        removeSitemapConfiguration(sitePath)
    })

    it('Check encoding for sitemap pages', () => {
        const names = [
            '/encoding-sitemap-test/sitemap-pages/page&gt;%C3%BC.html',
            '/encoding-sitemap-test/sitemap-pages/page&lt;%C3%BC.html',
            '/encoding-sitemap-test/sitemap-pages/page&amp;%C3%BC.html',
            '/encoding-sitemap-test/sitemap-pages/page&apos;%C3%BC.html',
            '/encoding-sitemap-test/sitemap-pages/page&quot;%C3%BC.html',
        ]

        deleteSitemapCache(siteKey)
        cy.request('en/sites/digitall/sitemap-lang.xml').then((response) => {

            for (const name of names) {
                expect(response.body).to.contains(name + '</loc>') // loc
                expect(response.body).to.contains(name + '"/>') // alternate link
            }
        });
    })

    it('Check encoding for sitemap pages with vanities', () => {
        const names = [
            '/actual-vanity&gt;',
            '/actual-vanity&lt;',
            '/actual-vanity&amp;',
            '/actual-vanity&apos;',
            '/actual-vanity&quot;',
        ]

        deleteSitemapCache(siteKey)
        cy.request('en/sites/digitall/sitemap-lang.xml').then((response) => {

            for (const name of names) {
                expect(response.body).to.contains(name + '</loc>') // loc
                expect(response.body).to.contains(name + '"/>') // alternate link
            }
        });
    })

    it('Check encoding for sitemap roots', () => {
        const names = [
            '/encoding-sitemap-test/sitemap-roots/root&gt;%C3%BC',
            '/encoding-sitemap-test/sitemap-roots/root&lt;%C3%BC',
            '/encoding-sitemap-test/sitemap-roots/root&amp;%C3%BC',
            '/encoding-sitemap-test/sitemap-roots/root&apos;%C3%BC',
            '/encoding-sitemap-test/sitemap-roots/root&quot;%C3%BC',
        ]

        deleteSitemapCache(siteKey)
        cy.request('/sites/digitall/sitemap.xml').then((response) => {
            for (const name of names) {
                expect(response.body).to.contains(name + '/sitemap-lang.xml</loc>')
            }
        });
    })
})