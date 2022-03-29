package org.jahia.modules.sitemap.filter;

import org.jahia.bin.filters.AbstractServletFilter;
import org.jahia.modules.sitemap.utils.Utils;
import org.osgi.service.component.annotations.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Web Filter to add sitemap attributes that will be used by the url rewrite rule.
 */
@Component(immediate = true, service = AbstractServletFilter.class)
public class SitemapWebFilter extends AbstractServletFilter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        setOrder(0);
        setUrlPatterns(new String[]{"*.xml"});
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (((HttpServletRequest) request).getServletPath().endsWith("/sitemap.xml")) {
            Utils.addRequestAttributes(request);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Do nothing
    }
}
