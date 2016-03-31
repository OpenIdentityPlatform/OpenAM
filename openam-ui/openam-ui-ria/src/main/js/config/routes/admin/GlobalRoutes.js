/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

define("config/routes/admin/GlobalRoutes", [
    "lodash"
], (_) => {
    const routes = {
        listAuthenticationSettings: {
            view: "org/forgerock/openam/ui/admin/views/configuration/authentication/ListAuthenticationView",
            url: /configure\/authentication/,
            pattern: "configure/authentication",
            role: "ui-global-admin",
            navGroup: "admin"
        },
        editAuthenticationSettings: {
            view: "org/forgerock/openam/ui/admin/views/configuration/EditConfigurationView",
            url: /configure\/authentication\/([^\/]+)/,
            pattern: "configure/authentication/?",
            role: "ui-global-admin",
            navGroup: "admin"
        },
        listGlobalServices: {
            view: "org/forgerock/openam/ui/admin/views/configuration/global/ListGlobalServicesView",
            url: /configure\/global-services/,
            pattern: "configure/global-services",
            role: "ui-global-admin",
            navGroup: "admin"
        },
        editGlobalService: {
            view: "org/forgerock/openam/ui/admin/views/configuration/EditConfigurationView",
            url: /configure\/global-service\/([^\/]+)/,
            pattern: "configure/global-service/?",
            role: "ui-global-admin",
            navGroup: "admin"
        },
        listSites: {
            view: "org/forgerock/openam/ui/admin/views/deployment/sites/ListSitesView",
            url: /deployment\/sites/,
            pattern: "deployment/sites",
            role: "ui-realm-admin",
            navGroup: "admin"
        },
        editSite: {
            view: "org/forgerock/openam/ui/admin/views/deployment/sites/EditSiteView",
            url: /deployment\/sites\/edit\/([^\/]+)/,
            pattern: "deployment/sites/edit/?",
            role: "ui-global-admin",
            navGroup: "admin"
        },
        newSite: {
            view: "org/forgerock/openam/ui/admin/views/deployment/sites/NewSiteView",
            url: /deployment\/sites\/new/,
            pattern: "deployment/sites/new",
            role: "ui-global-admin",
            navGroup: "admin"
        }
    };

    // Add routes for "Server Defaults" tree navigation
    _.each(["general", "security", "session", "sdk", "cts", "uma", "advanced"], (id) => {
        routes[`editServerDefaults${_.capitalize(id)}`] = {
            view: "org/forgerock/openam/ui/admin/views/configuration/server/EditServerDefaultsTreeNavigationView",
            page: "org/forgerock/openam/ui/admin/views/configuration/server/ServerDefaultsView",
            url: new RegExp(`configure/server-defaults/${id}`),
            pattern: `configure/server-defaults/${id}`,
            role: "ui-global-admin",
            navGroup: "admin",
            defaults: [id],
            forceUpdate: true
        };
    });

    return routes;
});
