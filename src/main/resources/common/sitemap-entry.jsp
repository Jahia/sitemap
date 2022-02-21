<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sitemap" uri="http://www.jahia.org/sitemap" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<c:set var="renderContext" value="${requestScope['renderContext']}"/>
<c:set var="urlNode" value="${requestScope['urlNode']}"/>
<c:set var="urlRewriteEnabled" value="${sitemap:urlRewriteEnabled()}"/>

<c:if test="${!urlNode.isNodeType('jseomix:noIndex')}">
<url>
    <%-- Note that there is an issue with vanity urls being processed by c:url when urlRewriteSeoRulesEnabled = false --%>
    <%-- So node url is used instead if seo features (vanity) is not available --%>
    <c:url var="vanityUrl" value="${sitemap:getLocaleVanityUrl(urlNode, renderContext.site.language)}"/>
    <c:set var="localizedUr" value="${urlNode.url}"/>
    <c:url var="localeUrl" value="${localizedUr}"  context="/"/>
    <c:set var="goodLocalUrl" value="${urlRewriteEnabled ? localeUrl : localizedUr}"/>
    <c:set var="finalUrl" value="${ (not empty vanityUrl and urlRewriteEnabled) ? vanityUrl : goodLocalUrl}"/>
    <%-- The URL host server name based on the input from sitemap UI panel--%>
    <c:set var="urlHostServerName" value="${renderContext.site.getPropertyAsString('sitemapIndexURL')}"/>
    <c:set var="serverName" value="${sitemap:getServerName(urlHostServerName)}"/>

    <jcr:nodeProperty var="lastModified" node="${urlNode}" name="jcr:lastModified"/>
    <!--${urlNode}-->
    <loc>${serverName}${finalUrl}</loc>
    <lastmod><fmt:formatDate value="${lastModified.date.time}" pattern="yyyy-MM-dd"/></lastmod>

    <c:set var="languageToReplacePart" value="/${renderContext.site.language}/"/>
    <c:set var="locales" value="${urlNode.getExistingLocales()}"/>
    <c:if test="${locales.size() > 1}"><%-- no need for alt if there's only one locale --%>
        <c:forEach var="locale" items="${locales}">
            <c:set var="lang" value="${locale.toString()}"/>
            
            <%-- the alternate languages should list all the other (alternate) languages and not the current language --%>
            <c:if test="${lang != renderContext.site.language}">
                <c:set var="langReplacement" value="/${locale.toString()}/"/>
                <c:set var="langDashFormat" value="${locale.toLanguageTag()}"/>

                <c:url var="vanityUrl" value="${sitemap:getLocaleVanityUrl(urlNode, lang)}"/>
                <c:set var="localizedUrl" value="${fn:replace(urlNode.url, languageToReplacePart, langReplacement)}"/>
                <c:url var="localeUrl" value="${localizedUrl}" context="/"/>
                <c:set var="goodLocaleUrl" value="${urlRewriteEnabled ? localeUrl : localizedUrl}"/>
                <c:set var="localeAltUrl" value="${ (not empty vanityUrl and urlRewriteEnabled) ? vanityUrl : goodLocaleUrl}"/>

                <xhtml:link rel="alternate" hreflang="${langDashFormat}" href="${serverName}${localeAltUrl}"/>
            </c:if>
        </c:forEach>
    </c:if>
</url>
</c:if>
