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
            <c:choose>
                <c:when test="${((pageContext.request.scheme == 'http') && (pageContext.request.serverPort == 80)) || (pageContext.request.scheme == 'https') && (pageContext.request.serverPort == 443)}">
                    <c:set var="serverUrl" value="${pageContext.request.scheme}://${pageContext.request.serverName}"/>
                </c:when>
                <c:otherwise>
                    <c:set var="serverUrl" value="${pageContext.request.scheme}://${pageContext.request.serverName}:${pageContext.request.serverPort}"/>
                </c:otherwise>
            </c:choose>
            <%-- language site maps --%>
            <jcr:nodeProperty node="${renderContext.site}" name="j:languages" var="languages"/>
            <jcr:nodeProperty node="${renderContext.site}" name="j:inactiveLiveLanguages" var="inactiveLiveLanguages"/>
            <c:forEach var="lang" items="${languages}">
                <c:if test="${not functions:contains(inactiveLiveLanguages, lang)}">
                    <sitemap>
                        <c:set value="${renderContext.request.contextPath}/cms/render/live/${lang}${renderContext.site.path}/sitemap-lang.xml" var="resolvedLangUrl"/>
                        <loc>${serverUrl}${resolvedLangUrl}</loc>
                    </sitemap>
                </c:if>
            </c:forEach>

            <%--  Separate sitemaps for jseomix:sitemapResource node option --%>
            <jcr:jqom var="additionalMaps">
                <query:selector nodeTypeName="jseomix:sitemapResource" selectorName="stmp"/>
                <query:descendantNode path="${renderContext.site.path}" selectorName="stmp"/>
            </jcr:jqom>
            <c:forEach var="node" items="${additionalMaps.nodes}">
                <jcr:nodeProperty node="${renderContext.site}" name="j:inactiveLiveLanguages"
                                  var="inactiveLiveLanguages"/>
                <c:forEach var="lang" items="${languages}">
                    <c:if test="${not functions:contains(inactiveLiveLanguages, lang)}">
                        <sitemap>
                            <c:set var="nodePath" value="${node.path}/"/>
                            <c:set value="${renderContext.request.contextPath}/cms/render/live/${lang}${node.path}/sitemap-lang.xml" var="resolvedLangUrl"/>
                            <loc>${serverUrl}${resolvedLangUrl}</loc>
                        </sitemap>
                    </c:if>
                </c:forEach>
            </c:forEach>
        </c:if>
    </sitemapindex>
</c:if>
