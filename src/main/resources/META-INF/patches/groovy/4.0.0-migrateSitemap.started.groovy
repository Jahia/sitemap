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

    protected void reverseMixins(JCRNodeWrapper node) {

        if (node.isNodeType("jmix:sitemap") && node.isNodeType("jmix:noindex")) {
            logger.error("Node " + node.getPath() + " was added to the sitemap but also marked as noIndex");
            node.removeMixin("jmix:sitemap");
            node.removeMixin("jmix:noindex");
            node.addMixin("jseomix:noIndex");
            logger.info("Mixins jmix:sitemap and jmix:noindex has been removed and jseomix:noIndex has been added on node " + node.getPath());
        } else if (node.isNodeType("jmix:sitemap")) {
            node.removeMixin("jmix:sitemap");
            logger.info("Mixins jmix:sitemap has been removed on node " + node.getPath());
        } else if (node.isNodeType("jmix:noindex")) {
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
            reverseMixins(node);
        }
        session.save();
        return true;
    }

    @Override
    protected Void getResult() {
        return null
    }
}

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
            reverseMixins(node);
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

def addRemoeApplicableMixins(List<JCRSiteNode> sites) {
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
    addRemoeApplicableMixins(sites);
    System.out.println("************** Sitemap migration completed **************");
    System.out.println("************** Do not forget to verify and publish your site **************");
}

runProgram();
