<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sitemap" uri="http://www.jahia.org/sitemap" %>

<c:set var="renderContext" value="${requestScope['renderContext']}"/>
<c:set var="urlNode" value="${requestScope['urlNode']}"/>

<c:if test="${!urlNode.properties['noIndex'].boolean}">
<url>
    <c:url var="vanityUrl" value="${sitemap:getLocaleVanityUrl(urlNode, renderContext.site.language)}"/>
    <c:url var="localeUrl" value="${url.getBase(renderContext.site.language)}${urlNode.path}.html"/>
    <c:set var="finalUrl" value="${ (not empty vanityUrl) ? vanityUrl : localeUrl }"/>
    <jcr:nodeProperty var="lastModified" node="${urlNode}" name="jcr:lastModified"/>

    <loc>${url.server}${finalUrl}</loc>
    <lastmod><fmt:formatDate value="${lastModified.date.time}" pattern="yyyy-MM-dd"/></lastmod>

    <c:set var="locales" value="${urlNode.getExistingLocales()}"/>
    <c:if test="${locales.size() > 1}"><%-- no need for alt if there's only one locale --%>
        <c:forEach var="locale" items="${locales}">
            <c:set var="lang" value="${locale.toString()}"/>

            <c:url var="vanityUrl" value="${sitemap:getLocaleVanityUrl(urlNode, lang)}"/>
            <c:url var="localeUrl" value="${url.getBase(lang)}${urlNode.path}.html"/>
            <c:set var="localeAltUrl" value="${ (not empty vanityUrl) ? vanityUrl : localeUrl }"/>

            <xhtml:link rel="alternate" hreflang="${lang}" href="${url.server}${localeAltUrl}"/>
        </c:forEach>
    </c:if>
</url>
</c:if>
