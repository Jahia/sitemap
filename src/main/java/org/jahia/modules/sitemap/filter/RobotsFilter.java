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
import java.util.ArrayList;
import java.util.List;

/**
 * Filter that adds robots meta tag to head element
 */
@Component(service = RenderFilter.class)
public class RobotsFilter extends AbstractFilter {
    public static final Logger logger = LoggerFactory.getLogger(RobotsFilter.class);

    private static final String NO_FOLLOW_MIXIN = "jseomix:noFollow";
    private static final String NO_INDEX_MIXIN = "jseomix:noIndex";
    private static final String HEADER_META_TEMPLATE = "<meta name=\"robots\" content=\"%s\"/>";

    @Activate
    public void activate() {
        setPriority(17);
        setApplyOnNodeTypes(String.format("%s,%s", NO_FOLLOW_MIXIN, NO_INDEX_MIXIN));
        setApplyOnModes("live");
        setDescription("Responsible for handling 'nofollow' and 'noindex' attributes of <meta name='robots'/> tag.");
        logger.debug("Activated RobotsFilter");
    }

    @Override
    public String execute(String previousOut, RenderContext renderContext, Resource resource, RenderChain chain) throws Exception {
        Source source = new Source(previousOut);
        //Add meta tags to top of head tag.
        List<Element> headList = source.getAllElements(HTMLElementName.HEAD);
        String tag;
        if (!headList.isEmpty() && (tag = addRobotsTag(resource)) != null) {
            OutputDocument od = new OutputDocument(source);
            StartTag et = headList.get(0).getStartTag();
            od.replace(et.getEnd(), et.getEnd(), tag);
            return od.toString();
        }
        return previousOut;
    }

    private String addRobotsTag(Resource resource) throws RepositoryException {
        JCRNodeWrapper node = resource.getNode();
        List<String> content = new ArrayList<>();

        if (node.isNodeType(NO_FOLLOW_MIXIN)) {
            content.add("nofollow");
        }

        if (node.isNodeType(NO_INDEX_MIXIN)) {
            content.add("noindex");
        }

        if (!content.isEmpty()) {
            return String.format(HEADER_META_TEMPLATE, String.join(",", content));
        }

        return null;
    }
}
