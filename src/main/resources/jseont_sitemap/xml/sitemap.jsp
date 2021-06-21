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
        <c:if test="${renderContext.liveMode and renderContext.site.defaultLanguage eq renderContext.mainResourceLocale.language}">
            <c:set var="nodeUrl" value="${currentNode.url}" />
            <c:set var="currentLanguage" value="${renderContext.site.language}"/>
            <c:set var="languageToReplacePart" value="/${renderContext.site.language}/"/>
            <%-- The URL host server name based on the input from sitemap UI panel--%>
            <c:set var="urlHostServerName" value="${renderContext.site.getPropertyAsString('sitemapIndexURL')}"/>
            <c:set var="serverName" value="${sitemap:getServerName(urlHostServerName)}"/>
            <c:set var="langXmlChunk" value="-lang.xml"/>
            <sitemap>
                <c:url value="${nodeUrl}" var="reesolvedUrl"/>
                <loc>${serverName}${fn:replace(reesolvedUrl, '.html', langXmlChunk)}</loc>
            </sitemap>
            <%-- language site maps --%>
            <jcr:nodeProperty node="${renderContext.site}" name="j:languages" var="languages"/>
            <jcr:nodeProperty node="${renderContext.site}" name="j:inactiveLanguages" var="inactiveLanguages"/>
            <c:forEach var="lang" items="${languages}">
                <c:if test="${not (currentLanguage eq lang) and not functions:contains(inactiveLanguages, lang)}">
                    <c:set value="/${lang.toString()}/" var="anotherLanguagePart"/>
                    <c:set var="langNodeUrl" value="${fn:replace(nodeUrl, languageToReplacePart, anotherLanguagePart)}"/>
                    <sitemap>
                        <c:url value="${langNodeUrl}" var="resolvedLangUrl"/>
                        <loc>${serverName}${fn:replace(resolvedLangUrl, '.html', langXmlChunk)}</loc>
                    </sitemap>
                </c:if>
            </c:forEach>

            <%--  Separate sitemaps for jseomix:sitemapResource node option --%>
            <jcr:jqom var="additionalMaps">
                <query:selector nodeTypeName="jseomix:sitemapResource" selectorName="stmp"/>
                <query:descendantNode path="${renderContext.site.path}" selectorName="stmp"/>
            </jcr:jqom>
            <c:set var="sitemapName" value="/sitemap-lang.xml"/>
            <c:forEach var="node" items="${additionalMaps.nodes}">
                <sitemap>
                    <loc>${serverName}<c:url value="${fn:replace(node.url, '.html', sitemapName)}"/></loc>
                </sitemap>
            </c:forEach>
        </c:if>
    </sitemapindex>
</c:if>
