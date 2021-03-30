import React from 'react';
import {registry} from '@jahia/ui-extender';
import i18next from 'i18next';
import SitemapPanelApp from './components/SitemapPanelApp';

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
                render: () => React.createElement(SitemapPanelApp, {
                    dxContext: {
                        ...window.contextJsParameters
                    }
                })
            });

            console.log('%c Sitemap registered routes', 'color: #3c8cba');
        }
    });
}
