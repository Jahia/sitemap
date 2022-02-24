<%@ page language="java" contentType="text/xml;charset=UTF-8" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="sitemap" uri="http://www.jahia.org/sitemap" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="`url" type="org.jahia.services.render.URLGenerator"--%>
<%-- TODO This JSP is a copy paste of sitemap.lang.jsp, please consider centralizing the code using a shared view --%>

<c:set var="entryNode" value="${currentNode.parent}"/>

<c:if test="${entryNode.isNodeType('jseomix:sitemapResource')}">
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="https://www.sitemaps.org/schemas/sitemap/0.9"
        xmlns:xhtml="https://www.w3.org/1999/xhtml">

    <%-- current page node --%>
    <c:set var="urlNode" value="${entryNode}" scope="request"/>
    <c:set var="renderContext" value="${renderContext}" scope="request"/>
    <jsp:include page="../../common/sitemap-entry.jsp"/>

    <%-- list of parent nodes to exclude --%>
    <jcr:sql var="excludeNodes"
         sql="SELECT * FROM [jseomix:sitemapResource]
            WHERE ISDESCENDANTNODE(['${entryNode.path}'])"/>

    <%-- jnt:page under currentNode --%>
    <c:forEach var="childUrlNode" items="${sitemap:getSitemapEntries(renderContext, entryNode.path, 'jnt:page')}">
        <c:if test="${!sitemap:excludeNode(childUrlNode, excludeNodes.nodes)}">
            <c:set var="urlNode" value="${childUrlNode}" scope="request"/>
            <c:set var="renderContext" value="${renderContext}" scope="request"/>
            <jsp:include page="../../common/sitemap-entry.jsp"/>
        </c:if>
    </c:forEach>

    <%-- jmix:mainResource under currentNode --%>
    <c:forEach var="childUrlNode" items="${sitemap:getSitemapEntries(renderContext, entryNode.path, 'jmix:mainResource')}">
        <c:if test="${!sitemap:excludeNode(childUrlNode, excludeNodes.nodes)}">
            <c:set var="urlNode" value="${childUrlNode}" scope="request"/>
            <c:set var="renderContext" value="${renderContext}" scope="request"/>
            <jsp:include page="../../common/sitemap-entry.jsp"/>
        </c:if>
    </c:forEach>

</urlset>
</c:if>
