package groovy

import org.jahia.services.content.JCRCallback
import org.jahia.services.content.JCRNodeWrapper
import org.jahia.services.content.JCRSessionWrapper
import org.jahia.services.content.JCRNodeIteratorWrapper
import org.jahia.services.content.JCRTemplate
import org.jahia.services.content.decorator.JCRSiteNode
import org.jahia.services.sites.JahiaSitesService
import org.jahia.settings.SettingsBean

import javax.jcr.NodeIterator
import javax.jcr.RepositoryException
import javax.jcr.query.InvalidQueryException
import javax.jcr.query.Query
import javax.jcr.query.QueryResult

def getApplicableSiteNodes(JCRSessionWrapper session) {
    List<JCRSiteNode> sites = JahiaSitesService.getInstance().getSitesNodeList(session);
    return sites.findAll { site -> site.getInstalledModules().contains("sitemap") };
}

def checkIfBothMixinsArePresent(JCRNodeWrapper node) {
    if (node.isNodeType("jmix:sitemap") && node.isNodeType("jmix:noindex")) {
        System.err.println("Node " + node.getPath() + " was added to the sitemap but also marked as noIndex");
    }
}

def removeJntSitemapNodes(JCRSessionWrapper session, String sitePath) {
    QueryResult qr = session.getWorkspace().getQueryManager().createQuery("select * from [jnt:sitemap] as sel where isdescendantnode(sel,['" + sitePath + "'])", Query.JCR_SQL2).execute();
    NodeIterator nodeIterator = qr.getNodes();
    while (nodeIterator.hasNext()) {
        JCRNodeWrapper node = (JCRNodeWrapper) nodeIterator.nextNode();
        node.remove();
    }
}

def removeJmixSitemapMixin(JCRSessionWrapper session, String sitePath) {
    QueryResult qr = session.getWorkspace().getQueryManager().createQuery("select * from [jmix:sitemap] as sel where isdescendantnode" +
            "(sel,['" + sitePath + "'])", Query.JCR_SQL2).execute();
    for (JCRNodeIteratorWrapper nodeIt = qr.getNodes(); nodeIt.hasNext();) {
        JCRNodeWrapper node = nodeIt.next();
        checkIfBothMixinsArePresent(node);
        node.removeMixin("jmix:sitemap");
        System.out.println("Mixins jmix:sitemap has been removed on node " + node.getPath());
    }
}

def updateJmixNoindexMixin(JCRSessionWrapper session, String sitePath) {
    try{
        QueryResult qr = session.getWorkspace().getQueryManager().createQuery("select * from [jmix:noindex] as sel where isdescendantnode" +
                "(sel,['" + sitePath + "'])", Query.JCR_SQL2).execute();
        for (JCRNodeIteratorWrapper nodeIt = qr.getNodes(); nodeIt.hasNext();) {
            JCRNodeWrapper node = nodeIt.next();
            checkIfBothMixinsArePresent(node);
            node.removeMixin("jmix:noindex");
            node.addMixin("jseomix:noIndex");
            System.out.println("Mixin jmix:noindex has been removed and jseomix:noIndex has been added on node " + node.getPath());
        }
    } catch (InvalidQueryException e){
        System.out.println("jmix:noindex mixin is not present on the Jahia instance");
    }
}

def removeSitemapResourceNodes(JCRSessionWrapper session, String sitePath, String nodetype) {
    System.out.println("Clean up of " + nodetype + " node on " + session.getWorkspace().getName() + " workspace");
    QueryResult qr = session.getWorkspace().getQueryManager().createQuery("select * from [" + nodetype + "] as sel where isdescendantnode" +
            "(sel,['" + sitePath + "'])", Query.JCR_SQL2).execute();
    NodeIterator nodeIterator = qr.getNodes();
    for (JCRNodeIteratorWrapper nodeIt = nodeIterator; nodeIt.hasNext();) {
        JCRNodeWrapper node = nodeIt.next();
        System.out.println("Node of type "  + nodetype + " with path [" + node.getPath() + "] will be removed");
        node.remove();
    }
}

def processNodes(List<JCRSiteNode> sites, JCRSessionWrapper session) {
    for (JCRSiteNode site : sites) {
        removeJntSitemapNodes(session, site.getPath());
        removeJmixSitemapMixin(session, site.getPath());
        updateJmixNoindexMixin(session, site.getPath());
        removeSitemapResourceNodes(session, site.getPath(), "jseont:sitemapResource");
        removeSitemapResourceNodes(session, site.getPath(), "jseont:sitemap");
    }
    session.save();
}

def runProgram() {
    if (SettingsBean.getInstance().isProcessingServer()) {
        System.out.println("************** Starting Sitemap migration **************");
        JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Void>() {
            @Override
            Void doInJCR(JCRSessionWrapper session) throws RepositoryException {
                processNodes(getApplicableSiteNodes(session) as List<JCRSiteNode>, session);
            }
        });
        System.out.println("************** Sitemap migration completed **************");
        System.out.println("************** Do not forget to verify and publish your site **************");
    }
}

runProgram();
