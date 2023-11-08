package org.jahia.modules.sitemap.beans;

import java.util.Locale;

/**
 * Representation of sitemap entry
 */
public class SitemapEntry {
    final private String path;
    final private String link;
    final private String lastMod;
    final private Locale locale;
    final private String primaryNodetype;
    final private String identifier;

    public SitemapEntry(String path, String link, String lastMod, Locale locale, String primaryNodetype, String identifier) {
        this.path = path;
        this.link = link;
        this.lastMod = lastMod;
        this.locale = locale;
        this.primaryNodetype = primaryNodetype;
        this.identifier = identifier;
    }

    public String getPath() {
        return path;
    }

    public String getLink() {
        return link;
    }

    public String getLastMod() {
        return lastMod;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getPrimaryNodetype() {
        return primaryNodetype;
    }

    public String getIdentifier() {
        return identifier;
    }
}
