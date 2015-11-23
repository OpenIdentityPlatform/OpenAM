/**
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
 * Copyright 2015 ForgeRock AS.
 */

define("config/routes/admin/RealmsRoutes", function () {
    var scopedByRealm = function (fragment) {
            return new RegExp("^realms\/([^\/]+)\/" + fragment + "$");
        },
        defaultScopedByRealm = function (fragment) {
            return scopedByRealm("?(?:" + fragment + ")?");
        },
        routes = {
            "realms": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmsListView",
                url: /^realms\/*$/,
                pattern: "realms",
                role: "ui-realm-admin",
                navGroup: "admin"
            },
            "realmsDashboard": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/dashboard/DashboardView",
                url: defaultScopedByRealm("dashboard\/?"),
                pattern: "realms/?/dashboard",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsAuthenticationSettings": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authentication/SettingsView",
                url: scopedByRealm("authentication\/?(?:settings\/?)?"),
                pattern: "realms/?/authentication/settings",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsAuthenticationChains": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authentication/ChainsView",
                url: scopedByRealm("authentication\/chains\/?"),
                pattern: "realms/?/authentication/chains",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsAuthenticationChainEdit": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authentication/chains/EditChainView",
                url: scopedByRealm("authentication\/chains\/([^\/]+)"),
                pattern: "realms/?/authentication/chains/?",
                role: "ui-realm-admin",
                navGroup: "admin"
            },
            "realmsAuthenticationModules": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authentication/ModulesView",
                url: scopedByRealm("authentication\/modules\/?"),
                pattern: "realms/?/authentication/modules",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsAuthenticationModuleEdit": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authentication/modules/EditModuleView",
                url: scopedByRealm("authentication\/modules\/([^\/]+)\/([^\/]+)"),
                pattern: "realms/?/authentication/modules/?/?",
                role: "ui-realm-admin",
                navGroup: "admin"
            },
            "realmsPolicySets": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authorization/policySets/PolicySetsView",
                url: scopedByRealm("policySets\/list"),
                pattern: "realms/?/policySets/list",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsPolicySetEdit": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authorization/policySets/EditPolicySetView",
                url: scopedByRealm("policySets\/edit\/([^\/]+)"),
                pattern: "realms/?/policySets/edit/?",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsPolicySetNew": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authorization/policySets/EditPolicySetView",
                url: scopedByRealm("policySets\/new"),
                pattern: "realms/?/policySets/new",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsPolicyNew": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authorization/policies/EditPolicyView",
                url: scopedByRealm("policySets\/edit\/([^\/]+)\/policies\/new"),
                pattern: "realms/?/policySets/edit/?/policies/new",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsPolicyEdit": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authorization/policies/EditPolicyView",
                url: scopedByRealm("policySets\/edit\/([^\/]+)\/policies\/edit\/([^\/]+)"),
                pattern: "realms/?/policySets/edit/?/policies/edit/?",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsResourceTypes": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/ResourceTypesView",
                url: scopedByRealm("resourceTypes\/list"),
                pattern: "realms/?/resourceTypes/list",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsResourceTypeEdit": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/EditResourceTypeView",
                url: scopedByRealm("resourceTypes\/edit\/([^\/]*)"),
                pattern: "realms/?/resourceTypes/edit/?",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsResourceTypeNew": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/EditResourceTypeView",
                url: scopedByRealm("resourceTypes\/new"),
                pattern: "realms/?/resourceTypes/new",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsScripts": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/scripts/ScriptsView",
                url: scopedByRealm("scripts\/list"),
                pattern: "realms/?/scripts/list",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsScriptEdit": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/scripts/EditScriptView",
                url: scopedByRealm("scripts\/edit\/([^\/]*)"),
                pattern: "realms/?/scripts/edit/?",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            }
        };

    routes.realmDefault = routes.realmsDashboard;

    return routes;
});
