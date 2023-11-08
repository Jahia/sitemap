import java.util.*
import javax.jcr.*
import javax.jcr.query.*
import org.jahia.services.content.*

int totalCount = 0;
int levelCount = 0;
int maxPages = 1000;
int maxPerLevel = 20;
String startPath = "/sites/digitall/home";
int currentLevel = 0;
int maxLevel = 7;
int maxLocalePerPage = 5
def locales = new ArrayList<Locale>();

public int createPages(def page, def session, int maxPerLevel, int maxPages, int initCount, int currentLevel, int maxLevel, def locales, int maxLocalePerPage) {
    String texttitle = "Mypage";
    long startNumberTitle = 0;
    def template = "home";
    List createdPages = new ArrayList();
    int totalCount = initCount;
    for (int i = 0; i < maxPerLevel; i++) {
        if (totalCount >= maxPages) {
            return totalCount;
        }
        def pageKey = JCRContentUtils.generateNodeName(texttitle + (startNumberTitle++));
        def subPage = page.addNode(pageKey, org.jahia.api.Constants.JAHIANT_PAGE);
        subPage.setProperty("j:templateName", template);
        Collections.shuffle locales;
        l = 0;
        for (locale in locales) {
            def translation = subPage.getOrCreateI18N(locale);
            translation.setProperty("jcr:title", texttitle + startNumberTitle);
            if (l++ > maxLocalePerPage) break;
        }
        createdPages.add(subPage);
        totalCount++;
        if (totalCount % 200 == 0) {
            session.save(JCRObservationManager.IMPORT);
            log.info("[createPages] {} pages created", totalCount, page.getPath())
        }
    }
    if (currentLevel >= maxLevel) {
        return totalCount;
    } else {
        currentLevel++;
        for (def createdPage : createdPages) {
            totalCount = createPages(createdPage, session, maxPerLevel, maxPages, totalCount, currentLevel, maxLevel, locales, maxLocalePerPage)
        }
    }
    return totalCount;
}


JCRTemplate.getInstance().doExecute(false, "root", "live", Locale.ENGLISH, new JCRCallback<Object>() {
    public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
        log.info("Get locales")
        session.getNode(startPath).getResolveSite().activeLiveLanguages.forEach(lang -> locales.add(Locale.forLanguageTag(lang)));
        log.info("{} locales added", locales.size())
        def rootPage = session.getNode(startPath).addNode(JCRContentUtils.findAvailableNodeName(session.getNode(startPath), "startPageTest"), "jnt:page")
        rootPage.setProperty("j:templateName", "home")
        for (def locale : locales) {
            def translation = rootPage.getOrCreateI18N(locale);
            translation.setProperty("jcr:title", "Root page of " + maxPages + " generated Pages");
        }
        session.save();
        int pageCreated = createPages(rootPage, session, maxPerLevel, maxPages, 0, currentLevel, maxLevel, locales, maxLocalePerPage);
        session.save();
        log.info("[createPages] {} pages created", pageCreated)
    }
});