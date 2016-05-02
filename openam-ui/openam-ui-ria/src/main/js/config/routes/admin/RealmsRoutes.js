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
 * Copyright 2015-2016 ForgeRock AS.
 */

define(function () {
    var scopedByRealm = function (fragment) {
            return new RegExp(`^realms\/((?:%2F)[^\/]*)\/${fragment}$`);
        },
        defaultScopedByRealm = function (fragment) {
            return scopedByRealm(`?(?:${fragment})?`);
        },
        routes = {
            "realms": {
                view: "org/forgerock/openam/ui/admin/views/realms/ListRealmsView",
                url: /^realms\/*$/,
                pattern: "realms",
                role: "ui-realm-admin",
                navGroup: "admin"
            },
            "realmEdit": {
                view: "org/forgerock/openam/ui/admin/views/realms/EditRealmView",
                url: scopedByRealm("edit"),
                pattern: "realms/?/edit",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmNew": {
                view: "org/forgerock/openam/ui/admin/views/realms/EditRealmView",
                url: /^realms\/new/,
                pattern: "realms/new",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsDashboard": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/dashboard/DashboardView",
                url: defaultScopedByRealm("dashboard/?"),
                pattern: "realms/?/dashboard",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsAuthenticationSettings": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authentication/SettingsView",
                url: scopedByRealm("authentication-settings/?"),
                pattern: "realms/?/authentication-settings",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsAuthenticationChains": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authentication/ChainsView",
                url: scopedByRealm("authentication-chains/?"),
                pattern: "realms/?/authentication-chains",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsAuthenticationChainEdit": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authentication/chains/EditChainView",
                url: scopedByRealm("authentication-chains/edit/([^/]+)"),
                pattern: "realms/?/authentication-chains/edit/?",
                role: "ui-realm-admin",
                navGroup: "admin"
            },
            "realmsAuthenticationChainNew": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authentication/chains/AddChainView",
                url: scopedByRealm("authentication-chains/new"),
                pattern: "realms/?/authentication-chains/new",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },

            "realmsAuthenticationModules": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authentication/ModulesView",
                url: scopedByRealm("authentication-modules/?"),
                pattern: "realms/?/authentication-modules",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsAuthenticationModuleNew": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authentication/modules/AddModuleView",
                url: scopedByRealm("authentication-modules/new"),
                pattern: "realms/?/authentication-modules/new",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsAuthenticationModuleEdit": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authentication/modules/EditModuleView",
                url: scopedByRealm("authentication-modules/([^/]+)/edit/([^/]+)"),
                pattern: "realms/?/authentication-modules/?/edit/?",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsServices": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/services/ServicesView",
                url: scopedByRealm("services/?"),
                pattern: "realms/?/services",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsServiceEdit": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/services/EditServiceView",
                url: scopedByRealm("services/edit/([^/]+)"),
                pattern: "realms/?/services/edit/?",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsServiceNew": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/services/NewServiceView",
                url: scopedByRealm("services/new"),
                pattern: "realms/?/services/new",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsServiceSubSchemaNew": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/services/NewServiceSubSchemaView",
                url: scopedByRealm("services/edit/([^/]+)/([^/]+)/new"),
                pattern: "realms/?/services/edit/?/?/new",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsServiceSubSchemaEdit": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/services/EditServiceSubSchemaView",
                url: scopedByRealm("services/edit/([^/]+)/([^/]+)/edit/([^/]+)"),
                pattern: "realms/?/services/edit/?/?/edit/?",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsPolicySets": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authorization/policySets/PolicySetsView",
                url: scopedByRealm("authorization-policySets/?"),
                pattern: "realms/?/authorization-policySets",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsPolicySetEdit": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authorization/policySets/EditPolicySetView",
                url: scopedByRealm("authorization-policySets/edit/([^/]+)"),
                pattern: "realms/?/authorization-policySets/edit/?",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsPolicySetNew": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authorization/policySets/EditPolicySetView",
                url: scopedByRealm("authorization-policySets/new"),
                pattern: "realms/?/authorization-policySets/new",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsPolicyNew": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authorization/policies/EditPolicyView",
                url: scopedByRealm("authorization-policySets/edit/([^/]+)/policies/new"),
                pattern: "realms/?/authorization-policySets/edit/?/policies/new",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsPolicyEdit": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authorization/policies/EditPolicyView",
                url: scopedByRealm("authorization-policySets/edit/([^/]+)/policies/edit/([^/]+)"),
                pattern: "realms/?/authorization-policySets/edit/?/policies/edit/?",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsResourceTypes": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/ResourceTypesView",
                url: scopedByRealm("authorization-resourceTypes/?"),
                pattern: "realms/?/authorization-resourceTypes",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsResourceTypeEdit": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/EditResourceTypeView",
                url: scopedByRealm("authorization-resourceTypes/edit/([^/]*)"),
                pattern: "realms/?/authorization-resourceTypes/edit/?",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsResourceTypeNew": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/EditResourceTypeView",
                url: scopedByRealm("authorization-resourceTypes/new"),
                pattern: "realms/?/authorization-resourceTypes/new",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsScripts": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/scripts/ScriptsView",
                url: scopedByRealm("scripts/?"),
                pattern: "realms/?/scripts",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsScriptEdit": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/scripts/EditScriptView",
                url: scopedByRealm("scripts/edit/([^/]*)"),
                pattern: "realms/?/scripts/edit/?",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            },
            "realmsScriptNew": {
                view: "org/forgerock/openam/ui/admin/views/realms/RealmTreeNavigationView",
                page: "org/forgerock/openam/ui/admin/views/realms/scripts/EditScriptView",
                url: scopedByRealm("scripts/new"),
                pattern: "realms/?/scripts/new",
                role: "ui-realm-admin",
                navGroup: "admin",
                forceUpdate: true
            }
        };

    routes.realmDefault = routes.realmsDashboard;

    return routes;
});
