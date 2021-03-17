import {registry} from '@jahia/ui-extender';
import i18next from 'i18next';

// TODO update permission
export default function () {
    registry.add('callback', 'sitemap', {
        targets: ['jahiaApp-init:22'],
        callback: async () => {
            await i18next.loadNamespaces('sitemap');
            registry.add('adminRoute', 'sitemap', {
                targets: ['jcontent:75'],
                icon: window.jahia.moonstone.toIconComponent('Search'),
                label: 'sitemap:labels.seo',
                isSelectable: false,
                requiredPermission: 'siteAdminUrlmapping',
                requireModuleInstalledOnSite: 'sitemap'
            });

            console.log('%c Sitemap registered routes', 'color: #3c8cba');
        }
    });
}
