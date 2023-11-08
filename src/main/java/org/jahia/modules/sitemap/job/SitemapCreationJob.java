package org.jahia.modules.sitemap.job;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.sitemap.beans.SitemapEntry;
import org.jahia.modules.sitemap.services.SitemapService;
import org.jahia.modules.sitemap.utils.Utils;
import org.jahia.osgi.BundleUtils;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.services.seo.urlrewrite.UrlRewriteService;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

public class SitemapCreationJob extends BackgroundJob {


    private static final Logger logger = LoggerFactory.getLogger(SitemapCreationJob.class);

    private static final String[] ENTITIES = new String[]{"&amp;", "&apos;", "&quot;", "&gt;", "&lt;"};
    private static final String[] ENCODED_ENTITIES = new String[]{"_-amp-_", "_-apos-_", "_-quot-_", "_-gt-_", "_-lt-_"};

    @Override
    public void executeJahiaJob(JobExecutionContext jobExecutionContext) throws Exception {
        UrlRewriteService urlRewriteService = BundleUtils.getOsgiService(UrlRewriteService.class, null);
        SitemapService sitemapService = BundleUtils.getOsgiService(SitemapService.class, null);
        if (sitemapService == null) {
            logger.info("Sitemap service not yet available");
            return;
        }
        String siteKey = jobExecutionContext.getJobDetail().getName();
        boolean isDebug = jobExecutionContext.getJobDetail().getJobDataMap().getBoolean("debug");
        // Set user to be use later by the system sessions.
        JCRSessionFactory.getInstance().setCurrentUser(JahiaUserManagerService.getInstance().lookupRootUser().getJahiaUser());
        JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
            JCRSiteNode siteNode = JahiaSitesService.getInstance().getSiteByKey(siteKey, session);
            String hostName = siteNode.getPropertyAsString("sitemapHostname");
            URL serverUrl;
            try {
                serverUrl = new URL(hostName);
                // trim context from hostname
                hostName = StringUtils.substringBeforeLast(hostName, serverUrl.getPath());
            } catch (MalformedURLException e) {
                logger.warn("{} is not a valid url for site {} , update your settings , Sitemap generation won't happen", hostName, siteKey);
                return null;
            }
            if (StringUtils.isEmpty(hostName)) {
                logger.warn("Unable to trigger Sitemap job without sitemap hostname set");
                return null;
            }
            // Mocked Objects
            final JahiaUser guestUser = ServicesRegistry.getInstance().getJahiaUserManagerService().lookupUser(Constants.GUEST_USERNAME).getJahiaUser();
            RenderContext customRenderContext = new RenderContext(null, null, guestUser);
            customRenderContext.setSite(siteNode);

            final HttpServletRequestMock request = new HttpServletRequestMock(new HashMap<>(), serverUrl.getHost(), serverUrl.getPath());
            final HttpServletResponseMock response = new HttpServletResponseMock(new StringWriter());
            // For each activated language for each site
            for (Locale currentLocale : siteNode.getActiveLiveLanguagesAsLocales()) {
                // Get all sitemaps to generate
                logger.info("Sitemap generation started for siteKey {} and locale {}", siteKey, currentLocale);
                for (String sitemapRoot : Utils.getSitemapRoots(customRenderContext, currentLocale.toString())) {
                    final ClassLoader initialClassLoader = Thread.currentThread().getContextClassLoader();
                    try (StringWriter output = new StringWriter()) {
                        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                        Set<SitemapEntry> entries = Utils.getSitemapEntries(customRenderContext, sitemapRoot, currentLocale);
                        // Build node settings
                        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                        Document doc = docBuilder.newDocument();
                        doc.setXmlStandalone(true);
                        Element rootSitemap = doc.createElement("urlset");
                        rootSitemap.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                        rootSitemap.setAttribute("xsi:schemaLocation", "http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd http://www.w3.org/1999/xhtml http://www.w3.org/2002/08/xhtml/xhtml1-strict.xsd");
                        rootSitemap.setAttribute("xmlns", "http://www.w3.org/2001/XMLSchema-instance");
                        rootSitemap.setAttribute("xmlns:xhtml", "http://www.w3.org/1999/xhtml");
                        doc.appendChild(rootSitemap);
                        for (SitemapEntry entry : entries) {
                            if (isDebug) {
                                Comment comment = doc.createComment(" nodePath: " + entry.getPath().replaceAll("-", "%2D"));
                                rootSitemap.appendChild(comment);
                                comment = doc.createComment(" nodeUrl: " + entry.getLink().replaceAll("-", "%2D"));
                                rootSitemap.appendChild(comment);
                                comment = doc.createComment(" type: " + entry.getPrimaryNodetype());
                                rootSitemap.appendChild(comment);
                                comment = doc.createComment(" uuid: " + entry.getIdentifier());
                                rootSitemap.appendChild(comment);
                            }
                            Element url = doc.createElement("url");
                            rootSitemap.appendChild(url);
                            Element lastmod = doc.createElement("lastmod");
                            lastmod.appendChild(doc.createTextNode(entry.getLastMod()));
                            url.appendChild(lastmod);
                            Element loc = doc.createElement("loc");
                            final Text locText = doc.createTextNode(hostName + encode(urlRewriteService.rewriteOutbound(entry.getLink(), request, response)));
                            loc.appendChild(locText);
                            url.appendChild(loc);
                            for (SitemapEntry langEntry : entry.getLinksInOtherLanguages()) {
                                Element langLink = doc.createElement("xhtml:link");
                                langLink.setAttribute("rel", "alternate");
                                langLink.setAttribute("hreflang", langEntry.getLocale().toString().replace("_", "-"));
                                langLink.setAttribute("href", hostName + encode(urlRewriteService.rewriteOutbound(langEntry.getLink(), request, response)));
                                url.appendChild(langLink);
                            }
                        }

                        TransformerFactory tf = TransformerFactory.newInstance();
                        Transformer t = tf.newTransformer();
                        t.setOutputProperty(OutputKeys.INDENT, "yes");
                        t.transform(new DOMSource(doc), new StreamResult(output));
                        sitemapService.addSitemap(siteKey, JCRContentUtils.escapeLocalNodeName(sitemapRoot) + "#" + currentLocale, decode(output.getBuffer().toString()));

                    } catch (ParserConfigurationException | TransformerException | ServletException | IOException |
                             InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } finally {
                        Thread.currentThread().setContextClassLoader(initialClassLoader);
                    }
                }
                logger.info("Sitemap generation End for siteKey {} and locale {}", siteKey, currentLocale);
            }
            return null;
        });
    }

    /**
     * Simple encoding function that encodes xml entities.
     *
     * @param uri to encode
     * @return an encoded uri
     * @throws UnsupportedEncodingException
     * @throws URIException
     */
    private static String encode(String uri) throws UnsupportedEncodingException, URIException {
        return StringUtils.replaceEach(Utils.encodeSitemapLink(uri, true), ENTITIES, ENCODED_ENTITIES);
    }

    /**
     * Same as encode, but the way around
     *
     * @param xml to decode
     * @return a decoded xml
     */
    private static String decode(String xml) {
        return StringUtils.replaceEach(xml, ENCODED_ENTITIES, ENTITIES);
    }

}
