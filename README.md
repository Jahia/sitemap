SEO Sitemap Module
=========================

The sitemap module provides capability to render sitemaps for a given site using XML format standards. This module also provides the capability to submit the generated sitemap to search engines through a scheduled job for a given specified frequency.

How to customize the sitemap ?
------------------------------

Once sitemap is enabled for a given site, the sitemap module provides a sitemap index located in `<site>/sitemap.index.xml` linking to different sitemap.xml views that the module provides. This includes sitemaps for each available languages as well as dedicated sitemaps for certain resources.

Each of the _sitemap.xml_ views lists all resources of type `jnt:page` and `jmix:mainResource`(which we will call _sitemap resources_) for their given subnodes. Sitemap resources can be excluded by enabling _No index_ option for that resource. In addition, sitemap resources can also have its own dedicated _sitemap.xml_ view that lists all sitemap resources within that subnode. This list will then get added automatically to the main sitemap.index.xml. This is useful for managing the size and organization of available sitemaps.

Sitemap generation
------------------------------

For each sitemap resource included in the sitemap.xml:

* Add `<url>` entry with the given URL specified in `<loc>` that is UTF-8 encoded and entity-escaped.
* Each URL entry will have last modified date `<lastmod>` in W3C format.
* Default vanity URL will be used for a given sitemap resource if it exists.
* Alternate language URLs will also be included (including default language) using `<xhtml:link rel="alternate">` tag.

Sitemap includes only resources that have default guest privileges in the list.

How do I submit my sitemap to search engines like Google, Yahoo!, Microsoft ?
-----------------------------------------------------------------------------

The submission of your sitemap can be done automatically by a background job in Jahia Digital Experience Manager. There's cfg file 
located at `${jahia.deploy.targetServerDirectory}/digital-factory-data/karaf/etc` called `org.jahia.modules.sitemap.config.impl.ConfigServiceImpl.cfg`.

    sitemap.job-frequency=24
    # Comma separated values
    sitemap.search-engines=http://www.google.com/webmasters/tools/ping?sitemap=
    # Comma separated values
    sitemap.sitemap-urls="https://example.com/sitemap.xml,https://example.com/sitemap2.xml"
    # comma separated values
    sitemap.included-content-types=jnt:pages,jnt:mainResource,jmix:sitemap

At the moment, the configuration has 3 keys. 
* Job Frequency
* List of Search engines
* List of sitemap urls

### Job frequency
This value determines how often to send the sitemap url to the search engines. The base unit is in hours - thus a value of 24 means the 
background job will run every 24 hours.

### List of Search engines
This is a comma-separated value of the list of search engines to use by the background job.

### Sitemap urls
This is a comma-separated value of the list of urls to send to the search engine(s).

Note: If the value of `sitemap.search-engines` or `sitemap.sitemap-urls` is empty, the background job will not run.

For each search engine, you will have to follow their policy. For example on Google, they ask that for the first time 
you manually register you sitemap using their webmaster tools (to follow what happens and have feedback in your 
Google environment).

Deploying
-----------------------------------------------------------------------------
After installing the sitemap module to Jahia, the `cfg` file still needs to be deployed manually. The file can be found in this
[link.](https://github.com/Jahia/sitemap/blob/master/src/main/resources/META-INF/configuration/org.jahia.modules.sitemap.config.impl.ConfigServiceImpl.cfg)
It should be copied to `$FACTORY_DATA/karaf/etc`

Reading the configuration data from JSP
----
The `sitemap.tld` has been updated functions to get the configurations. To use them, simply use JSP syntax. For example:
```jsp
Job Frequency: ${sitemap:getJobFreqency()}
Sitemap urls: ${sitemap:getSitemapUrls()}
Search engines: ${sitemap:getSearchEngines()}
Included Content Types: ${sitemap:getIncludedContentTypes()}
```
## Migration

On Jahia >= 8.0.3 migration should happen automatically provided that you have previous version of the module installed.

If you would like to migrate on Jahia version < 8.0.3 then you can use the groovy script provided in `META-INF/patches/groovy` folder. 
Simply copy and paste the contents of the file into groovy console in the `tools` section. 

Note that migration will remove `jnt:sitemap` nodes and remove `jmix:sitemap` mixins from every site `sitemap` module is deployed on. Migration script will mark all 
`jnt:page` and `jnt:content` nodes, which were not previously marked with `jmix:sitemap` with `jseomix:sitemapResource` with `noIndex` option set to `true` in 
order to preserve original functionality as much as possible. 

Also note that manual revision and publication of your site after migration will still be required. 
