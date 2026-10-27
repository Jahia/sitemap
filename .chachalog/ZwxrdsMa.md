---
# Allowed version bumps: patch, minor, major
sitemap: patch
---

Fixed the sitemap sometimes leaving out a page in every language after the page was unpublished in a single language, seen on clustered environments. The sitemap job now also reads the pages from the navigation tree of the site (the nodes carrying `jmix:navMenuItem`: pages, menu labels and links), so a page the search index has not caught up with yet is still listed right away instead of at a later generation.
