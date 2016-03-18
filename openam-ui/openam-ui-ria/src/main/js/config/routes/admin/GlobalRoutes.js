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

define("config/routes/admin/GlobalRoutes", [], () => ({
    listAuthenticationSettings: {
        view: "org/forgerock/openam/ui/admin/views/global/ListAuthenticationView",
        url: /configure\/authentication/,
        pattern: "configure/authentication",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    editAuthenticationSettings: {
        view: "org/forgerock/openam/ui/admin/views/global/EditConfigurationView",
        url: /configure\/authentication\/([^\/]+)/,
        pattern: "configure/authentication/?",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    listGlobalServices: {
        view: "org/forgerock/openam/ui/admin/views/global/ListGlobalServicesView",
        url: /configure\/global-services/,
        pattern: "configure/global-services",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    editGlobalService: {
        view: "org/forgerock/openam/ui/admin/views/global/EditConfigurationView",
        url: /configure\/global-service\/([^\/]+)/,
        pattern: "configure/global-service/?",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    // TODO : Uncomment when view ready.
    /*editServerDefaults: {
        view: "org/forgerock/openam/ui/admin/views/global/EditServerDefaultsView",
        url: /configure\/server-defaults/,
        pattern: "configure/server-defaults",
        role: "ui-global-admin",
        navGroup: "admin"
    },*/
    listSites: {
        view: "org/forgerock/openam/ui/admin/views/deployment/ListSitesView",
        url: /deployment\/sites/,
        pattern: "deployment/sites",
        role: "ui-realm-admin",
        navGroup: "admin"
    },
    editSite: {
        view: "org/forgerock/openam/ui/admin/views/deployment/EditSiteView",
        url: /deployment\/sites\/edit\/([^\/]+)/,
        pattern: "deployment/sites/edit/?",
        role: "ui-global-admin",
        navGroup: "admin"
    },
    newSite: {
        view: "org/forgerock/openam/ui/admin/views/deployment/NewSiteView",
        url: /deployment\/sites\/new/,
        pattern: "deployment/sites/new",
        role: "ui-global-admin",
        navGroup: "admin"
    }
}));
