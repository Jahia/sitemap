package groovy;

import org.jahia.services.content.JCRCallback
import org.jahia.services.content.JCRNodeWrapper
import org.jahia.services.content.JCRSessionWrapper
import org.jahia.services.content.JCRTemplate
import org.jahia.services.content.decorator.JCRSiteNode
import org.jahia.services.query.ScrollableQuery
import org.jahia.services.query.ScrollableQueryCallback
import org.jahia.services.sites.JahiaSitesService

import javax.jcr.NodeIterator
import javax.jcr.RepositoryException
import javax.jcr.query.Query
import javax.jcr.query.QueryManager
import org.apache.log4j.Logger

abstract class Scroller extends ScrollableQueryCallback<Void> {
    protected JCRSessionWrapper session;
    protected String sitePath;
    Logger logger = Logger.getLogger("MigrateSitemap")
    Scroller(JCRSessionWrapper session, String sitePath) {
        this.session = session
        this.sitePath = sitePath
    }

    abstract String getQuery();

    JCRSessionWrapper getSession() {
        return session
    }

    /**
     * Remove the mixins added in the version 3 of the module because in the version 4, the jnt:page and jmix:mainResource are
     * automatically added to the sitemap.
     * Set the mixin jseomix:noIndex where there was the mixin jmix:noindex
     * @param node to update
     */
    protected void cleanOldMixins(JCRNodeWrapper node) {
        if (node.isNodeType("jmix:sitemap") && node.isNodeType("jmix:noindex")) {
            logger.error("Node " + node.getPath() + " was added to the sitemap but also marked as noIndex");
        }
        if (node.isNodeType("jmix:sitemap")) {
            node.removeMixin("jmix:sitemap");
            logger.info("Mixins jmix:sitemap has been removed on node " + node.getPath());
        }
        if (node.isNodeType("jmix:noindex")) {
            node.removeMixin("jmix:noindex");
            node.addMixin("jseomix:noIndex");
            logger.info("Mixin jmix:noindex has been removed and jseomix:noIndex has been added on node " + node.getPath());
        }
    }
}

class RemoveSiteMapNodes extends Scroller {

    RemoveSiteMapNodes(JCRSessionWrapper session, String sitePath) {
        super(session, sitePath)
    }

    @Override
    String getQuery() {
        return "select * from [jnt:sitemap] as sel where isdescendantnode(sel,['" + sitePath + "'])";
    }

    @Override
    boolean scroll() throws RepositoryException {
        NodeIterator nodeIterator = stepResult.getNodes();
        while (nodeIterator.hasNext()) {
            JCRNodeWrapper node = (JCRNodeWrapper) nodeIterator.nextNode();
            node.remove();
        }
        session.save();
        return true;
    }

    @Override
    protected Void getResult() {
        return null
    }
}

/**
 * Get the jnt:page nodes of a site and clean the mixins on them
 */
class ProcessJntPage extends Scroller {

    ProcessJntPage(JCRSessionWrapper session, String sitePath) {
        super(session, sitePath)
    }

    @Override
    String getQuery() {
        return "select * from [jnt:page] as sel where isdescendantnode(sel,['" + sitePath + "'])";
    }

    @Override
    boolean scroll() throws RepositoryException {
        NodeIterator nodeIterator = stepResult.getNodes();
        while (nodeIterator.hasNext()) {
            JCRNodeWrapper node = (JCRNodeWrapper) nodeIterator.nextNode();
            cleanOldMixins(node);
        }
        session.save();
        return true;
    }

    @Override
    protected Void getResult() {
        return null
    }
}

/**
 * Get the jnt:content nodes of a site and clean the mixins on them
 */
class ProcessJntContent extends Scroller {

    ProcessJntContent(JCRSessionWrapper session, String sitePath) {
        super(session, sitePath)
    }

    @Override
    String getQuery() {
        return "select * from [jnt:content] as sel where isdescendantnode(sel,['" + sitePath + "'])";
    }

    @Override
    boolean scroll() throws RepositoryException {
        NodeIterator nodeIterator = stepResult.getNodes();
        while (nodeIterator.hasNext()) {
            JCRNodeWrapper node = (JCRNodeWrapper) nodeIterator.nextNode();
            cleanOldMixins(node);
        }
        session.save();
        return true;
    }

    @Override
    protected Void getResult() {
        return null
    }
}

def executeScrolledQuery(Scroller scroller) {
    QueryManager qm = scroller.getSession().getWorkspace().getQueryManager();
    final Query q = qm.createQuery(scroller.getQuery(), Query.JCR_SQL2);
    ScrollableQuery scrollableQuery = new ScrollableQuery(100, q);
    scrollableQuery.execute(scroller);
}

def getApplicableSiteNodes() {
    JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<List<JCRSiteNode>>() {
        @Override
        List<JCRSiteNode> doInJCR(JCRSessionWrapper session) throws RepositoryException {
            List<JCRSiteNode> sites = JahiaSitesService.getInstance().getSitesNodeList(session);
            return sites.findAll {site -> site.getInstalledModules().contains("sitemap")};
        }
    })
}

def removeJntSiteMapNodes(List<JCRSiteNode> sites) {
    JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Void>() {

        @Override
        Void doInJCR(JCRSessionWrapper session) throws RepositoryException {
            for (JCRSiteNode site : sites) {
                executeScrolledQuery(new RemoveSiteMapNodes(session, site.getPath()));
            }
            return null;
        }
    });
}

def processNodes(List<JCRSiteNode> sites) {
    JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Void>() {

        @Override
        Void doInJCR(JCRSessionWrapper session) throws RepositoryException {
            for (JCRSiteNode site : sites) {
                executeScrolledQuery(new ProcessJntPage(session, site.getPath()));
                executeScrolledQuery(new ProcessJntContent(session, site.getPath()));
            }
            return null;
        }
    });
}

def runProgram() {
    System.out.println("************** Starting Sitemap migration **************");
    List<JCRSiteNode> sites = getApplicableSiteNodes();
    removeJntSiteMapNodes(sites);
    processNodes(sites);
    System.out.println("************** Sitemap migration completed **************");
    System.out.println("************** Do not forget to verify and publish your site **************");
}

runProgram();
