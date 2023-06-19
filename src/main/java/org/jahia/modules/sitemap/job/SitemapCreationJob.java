package org.jahia.modules.sitemap.job;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
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

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.*;

public class SitemapCreationJob extends BackgroundJob {


    private static final Logger logger = LoggerFactory.getLogger(SitemapCreationJob.class);

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
            final HttpServletRequestMock request = new HttpServletRequestMock(new HashMap<>(), serverUrl.getHost(), serverUrl.getPath());
            final HttpServletResponseMock response = new HttpServletResponseMock(new StringWriter());
            // For each activated language for each site
            for (Locale currentLocale : siteNode.getActiveLiveLanguagesAsLocales()) {
                // Get all sitemaps to generate
                logger.info("Sitemap generation started for siteKey {} and locale {}", siteKey, currentLocale);
                final JahiaUser guestUser = ServicesRegistry.getInstance().getJahiaUserManagerService().lookupUser(Constants.GUEST_USERNAME).getJahiaUser();
                RenderContext customRenderContext = new RenderContext(null, null, guestUser);
                customRenderContext.setSite(siteNode);
                for (String sitemapRoot : Utils.getSitemapRoots(customRenderContext, currentLocale.toString())) {
                    final ClassLoader initialClassLoader = Thread.currentThread().getContextClassLoader();
                    try (StringWriter output = new StringWriter()) {
                        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                        Set<SitemapEntry> entries = Utils.getSitemapEntries(customRenderContext, sitemapRoot, currentLocale);
                        String targetSitemapCacheKey = JCRContentUtils.escapeLocalNodeName(sitemapRoot) + "#" + currentLocale;
                        // Build node settings
                        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                        Document doc = docBuilder.newDocument();
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
                            final Text locText = doc.createTextNode(hostName + encodeURI(urlRewriteService.rewriteOutbound(entry.getLink(), request, response)));
                            loc.appendChild(locText);
                            url.appendChild(loc);
                            for (SitemapEntry langEntry : entry.getLinksInOtherLanguages()) {
                                Element langLink = doc.createElement("xhtml:link");
                                langLink.setAttribute("rel", "alternate");
                                langLink.setAttribute("hreflang", langEntry.getLocale().toString().replace("_", "-"));
                                langLink.setAttribute("href", hostName + encodeURI(urlRewriteService.rewriteOutbound(langEntry.getLink(), request, response)));
                                url.appendChild(langLink);
                            }
                        }

                        TransformerFactory tf = TransformerFactory.newInstance();
                        Transformer t = tf.newTransformer();
                        t.setOutputProperty(OutputKeys.INDENT, "yes");
                        t.transform(new DOMSource(doc), new StreamResult(output));
                        sitemapService.addSitemap(siteKey, targetSitemapCacheKey, decodeURI(output.getBuffer().toString()));

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

    private static String encodeURI(String uri) throws UnsupportedEncodingException, URIException {
        String encodedUri = URLDecoder.decode(uri, "UTF-8");
        // Encode quote (') and double-quote (") - Force encoding of all entities
        encodedUri = StringUtils.replaceEach(encodedUri, new String[] {"\"", "'"} , new String[] {"-;quot-;", "-;apos-;"} );
        // URI Encode
        return URIUtil.encodePath(encodedUri, "UTF-8");
    }

    private static String decodeURI(String xml) {
        return StringUtils.replaceEach(xml, new String[] {"-;quot-;", "-;apos-;"} , new String[] {"&quot;", "&apos;"} );
    }

    private static class HttpServletRequestMock implements HttpServletRequest {
        private final Map<String, Object> attributes;
        private String servername;
        private String context;

        public HttpServletRequestMock(Map<String, Object> attributes, String servername, String context) {
            this.attributes = attributes;
            this.servername = servername;
            this.context = context;
        }

        @Override
        public String getAuthType() {
            return null;
        }

        @Override
        public Cookie[] getCookies() {
            return new Cookie[0];
        }

        @Override
        public long getDateHeader(String s) {
            return 0;
        }

        @Override
        public String getHeader(String s) {
            return null;
        }

        @Override
        public Enumeration<String> getHeaders(String s) {
            return null;
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return null;
        }

        @Override
        public int getIntHeader(String s) {
            return 0;
        }

        @Override
        public String getMethod() {
            return null;
        }

        @Override
        public String getPathInfo() {
            return null;
        }

        @Override
        public String getPathTranslated() {
            return null;
        }

        @Override
        public String getContextPath() {
            return context;
        }

        @Override
        public String getQueryString() {
            return null;
        }

        @Override
        public String getRemoteUser() {
            return null;
        }

        @Override
        public boolean isUserInRole(String s) {
            return false;
        }

        @Override
        public Principal getUserPrincipal() {
            return null;
        }

        @Override
        public String getRequestedSessionId() {
            return null;
        }

        @Override
        public String getRequestURI() {
            return null;
        }

        @Override
        public StringBuffer getRequestURL() {
            return null;
        }

        @Override
        public String getServletPath() {
            return null;
        }

        @Override
        public HttpSession getSession(boolean b) {
            return null;
        }

        @Override
        public HttpSession getSession() {
            return null;
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromUrl() {
            return false;
        }

        @Override
        public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
            return false;
        }

        @Override
        public void login(String s, String s1) throws ServletException {

        }

        @Override
        public void logout() throws ServletException {

        }

        @Override
        public Collection<Part> getParts() throws IOException, ServletException {
            return null;
        }

        @Override
        public Part getPart(String s) throws IOException, ServletException {
            return null;
        }

        @Override
        public Object getAttribute(String s) {
            return attributes.get(s);
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return Collections.enumeration(attributes.keySet());
        }

        @Override
        public String getCharacterEncoding() {
            return null;
        }

        @Override
        public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

        }

        @Override
        public int getContentLength() {
            return 0;
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return null;
        }

        @Override
        public String getParameter(String s) {
            return null;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return null;
        }

        @Override
        public String[] getParameterValues(String s) {
            return new String[0];
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return null;
        }

        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public String getScheme() {
            return null;
        }

        @Override
        public String getServerName() {
            return servername;
        }

        @Override
        public int getServerPort() {
            return 0;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return null;
        }

        @Override
        public String getRemoteAddr() {
            return null;
        }

        @Override
        public String getRemoteHost() {
            return null;
        }

        @Override
        public void setAttribute(String s, Object o) {
            attributes.put(s, o);
        }

        @Override
        public void removeAttribute(String s) {
            attributes.remove(s);
        }

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public Enumeration<Locale> getLocales() {
            return null;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String s) {
            return null;
        }

        @Override
        public String getRealPath(String s) {
            return null;
        }

        @Override
        public int getRemotePort() {
            return 0;
        }

        @Override
        public String getLocalName() {
            return null;
        }

        @Override
        public String getLocalAddr() {
            return null;
        }

        @Override
        public int getLocalPort() {
            return 0;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public AsyncContext startAsync() throws IllegalStateException {
            return null;
        }

        @Override
        public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
            return null;
        }

        @Override
        public boolean isAsyncStarted() {
            return false;
        }

        @Override
        public boolean isAsyncSupported() {
            return false;
        }

        @Override
        public AsyncContext getAsyncContext() {
            return null;
        }

        @Override
        public DispatcherType getDispatcherType() {
            return null;
        }
    }

    private static class HttpServletResponseMock implements HttpServletResponse {
        private final StringWriter out;

        @Override
        public int getStatus() {
            return 0;
        }

        @Override
        public String getHeader(String s) {
            return null;
        }

        @Override
        public Collection<String> getHeaders(String s) {
            return null;
        }

        @Override
        public Collection<String> getHeaderNames() {
            return null;
        }

        public HttpServletResponseMock(StringWriter out) {
            this.out = out;
        }

        @Override
        public void addCookie(Cookie cookie) {

        }

        @Override
        public boolean containsHeader(String name) {
            return false;
        }

        @Override
        public String encodeURL(String url) {
            return url;
        }

        @Override
        public String encodeRedirectURL(String url) {
            return null;
        }

        @Override
        public String encodeUrl(String url) {
            return null;
        }

        @Override
        public String encodeRedirectUrl(String url) {
            return null;
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {

        }

        @Override
        public void sendError(int sc) throws IOException {

        }

        @Override
        public void sendRedirect(String location) throws IOException {

        }

        @Override
        public void setDateHeader(String name, long date) {

        }

        @Override
        public void addDateHeader(String name, long date) {

        }

        @Override
        public void setHeader(String name, String value) {

        }

        @Override
        public void addHeader(String name, String value) {

        }

        @Override
        public void setIntHeader(String name, int value) {

        }

        @Override
        public void addIntHeader(String name, int value) {

        }

        @Override
        public void setStatus(int sc) {

        }

        @Override
        public void setStatus(int sc, String sm) {

        }

        @Override
        public String getCharacterEncoding() {
            return null;
        }

        @Override
        public void setCharacterEncoding(String charset) {

        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public void setContentType(String type) {

        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return null;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return new PrintWriter(out);
        }

        @Override
        public void setContentLength(int len) {

        }

        @Override
        public int getBufferSize() {
            return 0;
        }

        @Override
        public void setBufferSize(int size) {

        }

        @Override
        public void flushBuffer() throws IOException {

        }

        @Override
        public void resetBuffer() {

        }

        @Override
        public boolean isCommitted() {
            return false;
        }

        @Override
        public void reset() {

        }

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public void setLocale(Locale loc) {

        }
    }
}
