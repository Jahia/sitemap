package org.jahia.modules.sitemap.filter;

import net.htmlparser.jericho.*;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.jahia.services.render.filter.RenderFilter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.List;

/**
 * Filter that adds robots meta tag to head element
 */
@Component(service = RenderFilter.class)
public class RobotsFilter extends AbstractFilter {
    public static final Logger logger = LoggerFactory.getLogger(RobotsFilter.class);

    private static final String NO_FOLLOW_PROP = "noFollow";
    private static final String NO_INDEX_PROP = "noIndex";

    @Activate
    public void activate() {
        setPriority(15.1f); //Priority before cache filter
        setApplyOnNodeTypes("jseomix:sitemapResource");
        setApplyOnModes("preview,live");
        setDescription("Responsible for handling 'nofollow' and 'noindex' attributes of <meta name='robots'/> tag.");
        logger.debug("Activated RobotsFilter");
    }

    @Override
    public String execute(String previousOut, RenderContext renderContext, Resource resource, RenderChain chain) throws Exception {
        Source source = new Source(previousOut);
        OutputDocument od = new OutputDocument(source);
        addRobotsTag(source, od, resource);
        return od.toString();
    }

    private void addRobotsTag(Source source, OutputDocument od, Resource resource) throws RepositoryException {
        JCRNodeWrapper node = resource.getNode();
        StringBuilder tag = new StringBuilder("<meta name=\"robots\"");
        StringBuilder content = new StringBuilder();

        if (node.hasProperty(NO_FOLLOW_PROP) && node.getProperty(NO_FOLLOW_PROP).getBoolean()) {
            content.append("nofollow");
        }

        if (node.hasProperty(NO_INDEX_PROP) && node.getProperty(NO_INDEX_PROP).getBoolean()) {
            if (content.length() != 0) {
                content.append(",");
            }

            content.append("noindex");
        }

        if (content.length() != 0) {
            tag.append(String.format(" content=\"%s\"", content));
            tag.append("/>\n");

            //Add meta tags to top of head tag.
            List<Element> headList = source.getAllElements(HTMLElementName.HEAD);
            if (!headList.isEmpty()) {
                StartTag et = headList.get(0).getStartTag();
                od.replace(et.getEnd(), et.getEnd(), tag);
            }
        }
    }
}
