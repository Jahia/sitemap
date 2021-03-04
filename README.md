Sitemap (optional module)
=========================

In which format is my sitemap available ?
-----------------------------------------

The sitemap rendering is done by templates as default we provide three types of sitemap, html, text or xml (following 
standard from sitemaps.org.

How to customize the sitemap ?
------------------------------

The sitemap will list all sub elements of the current node having the options . By default, all objects of type 
`jnt:page` will have this options automatically activated by some rules. You can add your own rules for different types, 
or you can manually activate the option on a specific content.

Here is the rule for automatically assigning the option to a newly created page.

    rule "Add sitemap mixin to page"
        salience -10
        no-loop
        when
            A new node is created
                - it has the type jnt:page
        then
            Add the type 
    end

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