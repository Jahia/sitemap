<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sitemap" uri="http://www.jahia.org/sitemap" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>

<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="childUrlNode" type="org.jahia.services.content.JCRNodeWrapper"--%>

<c:set var="renderContext" value="${requestScope['renderContext']}"/>
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="https://www.sitemaps.org/schemas/sitemap/0.9"
        xmlns:xhtml="https://www.w3.org/1999/xhtml">
    <c:set var="urlRewriteEnabled" value=""/>
    <%-- current page node --%>
    <jcr:node var="entryNode" path="${param.entryNodePath}"/>
    <jcr:nodeProperty node="${entryNode}" name="j:inactiveLiveLanguages" var="inactiveLiveLanguages"/>
    <c:if test="${empty inactiveLiveLanguages || not functions:contains(inactiveLiveLanguages, renderContext.mainResourceLocale.language)}">
        <jsp:include page="./sitemap-entry.jsp">
            <jsp:param name="urlNodePath" value="${param.entryNodePath}"/>
        </jsp:include>
    </c:if>
    <%-- jnt:page under currentNode --%>
    <c:forEach var="childUrlNodePath" items="${sitemap:getSitemapEntries(param.entryNodePath, 'jnt:page', renderContext.mainResourceLocale)}">
        <jcr:nodeProperty node="${childUrlNode}" name="j:inactiveLiveLanguages" var="inactiveLiveLanguages"/>
        <c:if test="${empty inactiveLiveLanguages || not functions:contains(inactiveLiveLanguages, renderContext.mainResourceLocale.language)}">
            <jsp:include page="./sitemap-entry.jsp">
                <jsp:param name="urlNodePath" value="${childUrlNodePath}"/>
            </jsp:include>
        </c:if>
    </c:forEach>

    <%-- jmix:mainResource under currentNode --%>
    <c:forEach var="childUrlNodePath"
               items="${sitemap:getSitemapEntries(param.entryNodePath, 'jmix:mainResource', renderContext.mainResourceLocale)}">
        <jsp:include page="./sitemap-entry.jsp">
            <jsp:param name="urlNodePath" value="${childUrlNodePath}"/>
        </jsp:include>
    </c:forEach>

</urlset>