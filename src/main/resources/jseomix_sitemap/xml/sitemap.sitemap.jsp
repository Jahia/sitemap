<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="sitemap" uri="http://www.jahia.org/sitemap" %>

<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="flowRequestContext" type="org.springframework.webflow.execution.RequestContext"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>

<c:set target="${renderContext}" property="contentType" value="text/xml;charset=UTF-8"/>
<c:if test="${renderContext.site.isNodeType('jseomix:sitemap')}">
    <?xml version="1.0" encoding="UTF-8"?>
    <sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
    <c:if test="${renderContext.liveMode}">
        <c:set var="nodeUrl" value="${renderContext.site}"/>
        <c:set var="urlHostServerName" value="${renderContext.site.getPropertyAsString('sitemapHostname')}"/>
        <c:choose>
            <c:when test="${!empty urlHostServerName}">
                <c:set var="serverUrl" value="${urlHostServerName}"/>
            </c:when>
            <c:when test="${((pageContext.request.scheme == 'http') && (pageContext.request.serverPort == 80)) || (pageContext.request.scheme == 'https') && (pageContext.request.serverPort == 443)}">
                <c:set var="serverUrl" value="${pageContext.request.scheme}://${pageContext.request.serverName}"/>
            </c:when>
            <c:otherwise>
                <c:set var="serverUrl"
                       value="${pageContext.request.scheme}://${pageContext.request.serverName}:${pageContext.request.serverPort}"/>
            </c:otherwise>
        </c:choose>
        <c:forEach var="lang" items="${renderContext.site.activeLiveLanguages}">
                <!-- Dedicated sitemap entries for language: ${lang} -->
            <c:forEach var="nodePath" items="${sitemap:getSitemapRoots(renderContext, lang)}">
                <sitemap>
                    <c:set value="/cms/render/live/${lang}${nodePath}/sitemap-lang.xml"
                           var="resolvedLangUrl"/>
                    <loc>${serverUrl}${sitemap:encodeSitemapLink(resolvedLangUrl, false, renderContext, true)}</loc>
                </sitemap>
            </c:forEach>
        </c:forEach>
        </sitemapindex>
    </c:if>
</c:if>