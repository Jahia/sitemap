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

    private static final String NO_FOLLOW_MIXIN = "jseomix:noFollow";
    private static final String NO_INDEX_MIXIN = "jseomix:noIndex";

    @Activate
    public void activate() {
        // TODO Why do we want it not be cached ? please consider moving this filter in a better position so it could benefit from cache system
        setPriority(15.1f); //Priority before cache filter
        // TODO missing setApplyOnMainResource(true);
        // TODO this filter will be triggered even for pages and other main resources displayed in pages
        setApplyOnNodeTypes(String.format("%s,%s", NO_FOLLOW_MIXIN, NO_INDEX_MIXIN));
        // TODO why preview, no robots can crawl the preview ... ?
        setApplyOnModes("preview,live");
        setDescription("Responsible for handling 'nofollow' and 'noindex' attributes of <meta name='robots'/> tag.");
        logger.debug("Activated RobotsFilter");
    }

    @Override
    public String execute(String previousOut, RenderContext renderContext, Resource resource, RenderChain chain) throws Exception {
        // TODO optimization here: Why not checking first that the HTML contains a <head> before continue ?
        // TODO it's done at the end of the addRobotsTag, would be better to do it before calculating stuff.
        Source source = new Source(previousOut);
        OutputDocument od = new OutputDocument(source);
        addRobotsTag(source, od, resource);
        return od.toString();
    }

    private void addRobotsTag(Source source, OutputDocument od, Resource resource) throws RepositoryException {
        JCRNodeWrapper node = resource.getNode();
        StringBuilder tag = new StringBuilder("<meta name=\"robots\"");
        StringBuilder content = new StringBuilder();

        if (node.isNodeType(NO_FOLLOW_MIXIN)) {
            content.append("nofollow");
        }

        if (node.isNodeType(NO_INDEX_MIXIN)) {
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
