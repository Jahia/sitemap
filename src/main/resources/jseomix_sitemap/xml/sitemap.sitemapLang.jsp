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

<c:set var="currentLanguage" value="${renderContext.site.language}"/>
<c:set var="renderContext" value="${renderContext}" scope="request"/>
<jcr:nodeProperty node="${currentNode}" name="j:inactiveLiveLanguages" var="inactiveLiveLanguages"/>
<%-- node check type to make sure sitemap is not enabled then disabled --%>
<c:if test="${empty inactiveLiveLanguages || not functions:contains(inactiveLiveLanguages, renderContext.mainResourceLocale.language)}">
	<jsp:include page="../../common/sitemap-xml.jsp">
		<jsp:param name="entryNodePath" value="${currentNode.path}"/>
	</jsp:include>
</c:if>