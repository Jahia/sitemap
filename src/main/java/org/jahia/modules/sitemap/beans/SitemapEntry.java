package org.jahia.modules.sitemap.beans;

import java.util.List;
import java.util.Locale;

/**
 * Representation of sitemap entry
 */
public class SitemapEntry {
    final private String path;
    final private String link;
    final private String lastMod;
    final private Locale locale;
    final private List<SitemapEntry> linksInOtherLanguages;

    public SitemapEntry(String path, String link, String lastMod, Locale locale, List<SitemapEntry> linksInOtherLanguages) {
        this.path = path;
        this.link = link;
        this.lastMod = lastMod;
        this.locale = locale;
        this.linksInOtherLanguages = linksInOtherLanguages;
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

    public List<SitemapEntry> getLinksInOtherLanguages() {
        return linksInOtherLanguages;
    }
}
