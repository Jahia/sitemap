<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<c:set target="${renderContext}" property="contentType" value="text/plain;charset=UTF-8"/>

<c:choose>
    <c:when test="${(currentNode.name == 'home')}">
        <c:set var="siteMapNode" value="${currentNode.parent}"/>
    </c:when>
    <c:otherwise>
        <c:set var="siteMapNode" value="${currentNode}"/>
        <c:url value="${url.base}${currentNode.path}.html"/>
    </c:otherwise>
</c:choose>    

<jcr:jqom var="sitemaps">
    <query:selector nodeTypeName="jmix:sitemap" selectorName="stmp"/>
    <query:descendantNode path="${siteMapNode.path}" selectorName="stmp"/>
</jcr:jqom>

<c:forEach items="${sitemaps.nodes}" varStatus="status" var="sitemapEL">
    <c:url value="${url.base}${sitemapEL.path}.html"/>
</c:forEach>    
