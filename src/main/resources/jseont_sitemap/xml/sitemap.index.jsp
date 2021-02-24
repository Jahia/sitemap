<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>

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
<?xml version="1.0" encoding="UTF-8"?>
<sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
        <c:if test="${renderContext.liveMode}">
                <c:set var="siteMapPath" value="${fn:replace(currentNode.path, '.html', '')}" />
                <sitemap>
                        <loc>${url.server}<c:url value="${siteMapPath}.xml"/></loc>
                </sitemap>
                <%--        <sitemap>--%>
                <%--                <loc>${url.server}<c:url value="${siteMapPath}.custom.xml"/></loc>--%>
                <%--        </sitemap>--%>
                <%--        <sitemap>--%>
                <%--                <loc>${url.server}<c:url value="${siteMapPath}.images.xml"/></loc>--%>
                <%--        </sitemap>--%>
                <%-- for pdfs and maybe other resources --%>
                <%--        <sitemap>--%>
                <%--                <loc>${url.server}<c:url value="${siteMapPath}.resources.xml"/></loc>--%>
                <%--        </sitemap>--%>

                <%-- language site maps --%>
                <jcr:nodeProperty node="${renderContext.site}" name="j:languages" var="languages"/>
                <jcr:nodeProperty node="${renderContext.site}" name="j:inactiveLanguages" var="inactiveLanguages"/>
                <c:set var="currentLanguage" value="${renderContext.site.language}"/>
                <c:forEach var="lang" items="${languages}">
                        <c:if test="${not (currentLanguage eq lang) and not functions:contains(inactiveLanguages, lang)}">
                                <c:url value="${url.getBase(lang.toString())}${siteMapPath}.xml" var="languageResource"/>
                                <sitemap>
                                        <loc>${url.server}${languageResource}</loc>
                                </sitemap>
                        </c:if>
                </c:forEach>

                <%--  Separate sitemaps for jseont:sitemapResource node option --%>
                <%-- TODO: replace jnt:page to jseont:sitemapResource once nodetype is available --%>
                <jcr:jqom var="additionalMaps">
                        <query:selector nodeTypeName="jnt:page" selectorName="stmp"/>
                        <query:descendantNode path="${renderContext.site.path}" selectorName="stmp"/>
                </jcr:jqom>
                <c:forEach var="node" items="${additionalMaps.nodes}">
                        <sitemap>
                                <loc>${url.server}<c:url value="${fn:replace(node.url, '.html', '/sitemap.xml')}"/></loc>
                        </sitemap>
                </c:forEach>
        </c:if>
</sitemapindex>
