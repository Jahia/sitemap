<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sitemap" uri="http://www.jahia.org/sitemap" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>

<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="childUrlNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="sitemapEntry" type="org.jahia.modules.sitemap.beans.SitemapEntry"--%>

<c:set var="renderContext" value="${requestScope['renderContext']}"/>
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="https://www.sitemaps.org/schemas/sitemap/0.9"
        xmlns:xhtml="https://www.w3.org/1999/xhtml">
    <%-- The URL host server name based on the input from sitemap UI panel--%>
    <c:set var="urlHostServerName" value="${renderContext.site.getPropertyAsString('sitemapIndexURL')}"/>
    <c:set var="port" value=""/>
    <c:if test="${!empty pageContext.request.serverPort}">
        <c:set var="port" value=":${pageContext.request.serverPort}"/>
    </c:if>
    <c:set var="serverName" value="${pageContext.request.scheme}://${pageContext.request.serverName}${port}"/>
    <c:forEach var="sitemapEntry" items="${sitemap:getSitemapEntries(renderContext, param.entryNodePath, ['jnt:page', 'jmix:mainResource'], renderContext.mainResourceLocale)}">
        <url>
            <loc>${serverName}<c:url context="/" value="${sitemapEntry.link}"/></loc>
            <lastmod>${sitemapEntry.lastMod}</lastmod>
            <c:forEach items="${sitemapEntry.linksInOtherLanguages}" var="link">
                <xhtml:link rel="alternate" hreflang="${link.locale}" href="${serverName}<c:url value="${link.link}" context="/"/>"/>
            </c:forEach>
        </url>
    </c:forEach>
</urlset>