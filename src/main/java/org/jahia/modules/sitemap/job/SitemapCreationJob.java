package org.jahia.modules.sitemap.job;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.sitemap.beans.SitemapEntry;
import org.jahia.modules.sitemap.services.SitemapService;
import org.jahia.modules.sitemap.utils.Utils;
import org.jahia.osgi.BundleUtils;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.scheduler.BackgroundJob;
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SitemapCreationJob extends BackgroundJob {


    private static final Logger logger = LoggerFactory.getLogger(SitemapCreationJob.class);
    private static final Map<Locale, JCRSessionWrapper> sessionPerLocale = new HashMap<>();


    @Override
    public void executeJahiaJob(JobExecutionContext jobExecutionContext) throws Exception {
        final ClassLoader initialClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // Switch class loader to Jahia (for url rewrite service)
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            SitemapService sitemapService = BundleUtils.getOsgiService(SitemapService.class, null);
            if (sitemapService == null) {
                logger.info("Sitemap service not yet available");
                return;
            }
            String siteKey = jobExecutionContext.getJobDetail().getName();

            // Set the job as running (in case of a scheduled job)
            Utils.markSitemapGenerationAsRunning(siteKey);
            boolean isDebug = jobExecutionContext.getJobDetail().getJobDataMap().getBoolean("debug");
            // Set user to be use later by the system sessions.
            JCRSessionFactory.getInstance().setCurrentUser(JahiaUserManagerService.getInstance().lookupRootUser().getJahiaUser());
            JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
                final JahiaUser guestUser = ServicesRegistry.getInstance().getJahiaUserManagerService().lookupUser(Constants.GUEST_USERNAME).getJahiaUser();
                JCRSiteNode siteNode = JahiaSitesService.getInstance().getSiteByKey(siteKey, session);
                // Open Session per language
                final ArrayList<Locale> locales = new ArrayList<>(siteNode.getActiveLiveLanguagesAsLocales());
                locales.add(null);
                for (Locale locale : locales) {
                    JahiaUser currentUser = JCRSessionFactory.getInstance().getCurrentUser();
                    JCRSessionFactory.getInstance().setCurrentUser(guestUser);
                    JCRSessionWrapper localizedSession = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.LIVE_WORKSPACE, locale);
                    sessionPerLocale.put(locale, localizedSession);
                    JCRSessionFactory.getInstance().setCurrentUser(currentUser);
                }

                URL serverUrl;
                String hostName = Utils.getHostName(siteNode);
                if (StringUtils.isEmpty(hostName)) {
                    logger.warn("The host name can not be extracted from the property sitemapIndexURL");
                    return null;
                }

                try {
                    serverUrl = new URL(hostName);
                } catch (MalformedURLException e) {
                    logger.warn("The property sitemapIndexURL does not match an URL pattern, Sitemap generation won't happen");
                    return null;
                }

                // Mocked Objects
                final HttpServletRequestMock request = new HttpServletRequestMock(new HashMap<>(), serverUrl.getHost(), serverUrl.getPath());
                final HttpServletResponseMock response = new HttpServletResponseMock(new StringWriter());
                request.setAttribute("jahiaHostname", hostName);
                RenderContext customRenderContext = new RenderContext(request, response, guestUser);
                customRenderContext.setSite(siteNode);


                // For each activated language for each site

                // Build sitemap entries
                long startTime = System.currentTimeMillis();
                logger.info("Start generating sitemap entries for {} languages", locales.size() - 1);
                final Set<String> sitemapRoots = Utils.getSitemapRoots(customRenderContext, null);
                for (String sitemapRoot : sitemapRoots) {
                    Map<Locale, Set<SitemapEntry>> entriesByLocale = new HashMap<>();
                    Map<String, Set<SitemapEntry>> entriesByPath = new HashMap<>();
                    Utils.generateSitemapEntries(sessionPerLocale, customRenderContext, sitemapRoot, sitemapRoots, entriesByLocale, entriesByPath);

                    final long endTime = System.currentTimeMillis();
                    logger.info("End generating entries: {} path added in {}s", entriesByPath.size(), (endTime - startTime) / 1000);

                    for (Locale currentLocale : siteNode.getActiveLiveLanguagesAsLocales()) {
                        // Get all sitemaps to generate
                        startTime = System.currentTimeMillis();
                        logger.info("Sitemap generation started for sitemap ROOT {} and locale {}", sitemapRoot, currentLocale);
                        try {
                            Path tmpFile = Files.createTempFile(siteKey + "-" + currentLocale.toLanguageTag(), ".xml");
                            try (FileWriter output = new FileWriter(tmpFile.toFile())) {
                                Set<SitemapEntry> entries = entriesByLocale.get(currentLocale);
                                if (entries == null || entries.isEmpty()) {
                                    logger.info("No sitemap entries found for {}", currentLocale);
                                    continue;
                                }
                               // Build node settings
                                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                                Document doc = docBuilder.newDocument();
                                doc.setXmlStandalone(true);
                                Element rootSitemap = doc.createElement("urlset");
                                rootSitemap.setAttribute("xmlns", "http://www.sitemaps.org/schemas/sitemap/0.9");
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
                                    final Text locText = doc.createTextNode(entry.getLink());
                                    loc.appendChild(locText);
                                    url.appendChild(loc);

                                    for (SitemapEntry langEntry : entriesByPath.get(entry.getPath())) {
                                        Element langLink = doc.createElement("xhtml:link");
                                        langLink.setAttribute("rel", "alternate");
                                        langLink.setAttribute("hreflang", langEntry.getLocale().toString().replace("_", "-"));
                                        langLink.setAttribute("href", langEntry.getLink());
                                        url.appendChild(langLink);
                                    }
                                }

                                TransformerFactory tf = TransformerFactory.newInstance();
                                Transformer t = tf.newTransformer();
                                t.setOutputProperty(OutputKeys.INDENT, "yes");
                                t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                                t.transform(new DOMSource(doc), new StreamResult(output));
                                sitemapService.addSitemap(siteKey, JCRContentUtils.escapeLocalNodeName(sitemapRoot) + "#" + currentLocale, tmpFile);

                            } catch (Throwable e) {
                                throw new RuntimeException(e);
                            }
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                        logger.info("Sitemap generation End for sitemap ROOT {} and locale {} in {}s", sitemapRoot, currentLocale, (System.currentTimeMillis() - startTime) / 1000);
                    }
                }
                return null;
            });
        } finally {
            // Clear job marker
            JCRTemplate.getInstance().doExecuteWithSystemSession(session ->  {
                JCRSiteNode siteNode = JahiaSitesService.getInstance().getSiteByKey(jobExecutionContext.getJobDetail().getName(), session);
                // This can happen if the sitemap module is uninstalled from a site as the job is running.
                if (siteNode.hasProperty("isSitemapJobTriggered")) {
                    siteNode.getProperty("isSitemapJobTriggered").remove();
                    session.save();
                }
                return null;
            });
            // No need to close sessions as it's automatically done at the end of the job
            Thread.currentThread().setContextClassLoader(initialClassLoader);
        }
    }

}
