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
    <c:choose>
        <c:when test="${((pageContext.request.scheme == 'http') && (pageContext.request.serverPort == 80)) || (pageContext.request.scheme == 'https') && (pageContext.request.serverPort == 443)}">
            <c:set var="serverUrl" value="${pageContext.request.scheme}://${pageContext.request.serverName}"/>
        </c:when>
        <c:otherwise>
            <c:set var="serverUrl"
                   value="${pageContext.request.scheme}://${pageContext.request.serverName}:${pageContext.request.serverPort}"/>
        </c:otherwise>
    </c:choose>
    <c:forEach var="sitemapEntry"
               items="${sitemap:getSitemapEntries(renderContext, param.entryNodePath, ['jnt:page', 'jmix:mainResource'], renderContext.mainResourceLocale)}">
        <url>
            <c:if test="${param[\"sitemap_debug\"] eq \"true\"}">
                <jcr:node var="sitemapEntryNode" path="${sitemapEntry.path}" />
                <%-- we add some current node informations for debug --%>
                <c:set var="nodePath" value="${sitemapEntryNode.path}"/>
                <c:set var="nodeUrl" value="${sitemapEntryNode.url}"/>
                <%-- xml comment can't contain two hyphens --%>
                <c:if test="${fn:contains(nodePath, '--') || fn:contains(nodeUrl, '--')}">
                    <c:set var="nodePath" value="${nodePath.replaceAll('-', '%2D')}"/>
                    <c:set var="nodeUrl" value="${nodeUrl.replaceAll('-', '%2D')}"/>
                </c:if>
                <!-- node path: ${nodePath} -->
                <!-- node url: ${nodeUrl} -->
                <!-- node type: ${sitemapEntryNode.primaryNodeTypeName} -->
                <!-- node uuid: ${sitemapEntryNode.identifier} -->
            </c:if>
            <loc>${serverUrl}<c:url context="/" value="${sitemapEntry.link}"/></loc>
            <lastmod>${sitemapEntry.lastMod}</lastmod>
            <c:forEach items="${sitemapEntry.linksInOtherLanguages}" var="link">
                <xhtml:link rel="alternate" hreflang="${link.locale}"
                            href="${serverUrl}<c:url value="${link.link}" context="/"/>"/>
            </c:forEach>
        </url>
    </c:forEach>
</urlset>