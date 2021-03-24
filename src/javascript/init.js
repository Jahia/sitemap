import {registry} from '@jahia/ui-extender';
import i18next from 'i18next';

export default function () {
    // TODO update permission
    registry.add('callback', 'sitemap', {
        targets: ['jahiaApp-init:25'],
        callback: async () => {
            await i18next.loadNamespaces('sitemap');
            registry.add('adminRoute', 'siteSettingsSeo/sitemap', {
                targets: ['jcontent-siteSettingsSeo:75'],
                label: 'sitemap:labels.sitemap',
                isSelectable: true,
                // RequiredPermission: 'siteAdminUrlmapping',
                requireModuleInstalledOnSite: 'sitemap',
                render: () => 'Hello Sitemap'
            });

            console.log('%c Sitemap registered routes', 'color: #3c8cba');
        }
    });
}
