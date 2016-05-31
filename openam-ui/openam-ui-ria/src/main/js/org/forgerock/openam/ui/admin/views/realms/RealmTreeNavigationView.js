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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/services/global/RealmsService",
    "org/forgerock/openam/ui/common/components/TreeNavigation",
    "org/forgerock/openam/ui/admin/views/common/navigation/createBreadcrumbs",
    "org/forgerock/openam/ui/admin/views/common/navigation/createTreeNavigation"
], ($, _, Constants, EventManager, Router, RealmsService, TreeNavigation, createBreadcrumbs,
    createTreeNavigation) => {

    const navData = [{
        title: "console.common.navigation.dashboard",
        icon: "fa-dashboard",
        route: "realmsDashboard"
    }, {
        title: "console.common.navigation.authentication",
        icon: "fa-user",
        children: [{
            title: "console.common.navigation.settings",
            icon: "fa-angle-right",
            route: "realmsAuthenticationSettings"
        }, {
            title: "console.common.navigation.chains",
            icon: "fa-angle-right",
            route: "realmsAuthenticationChains"
        }, {
            title: "console.common.navigation.modules",
            icon: "fa-angle-right",
            route: "realmsAuthenticationModules"
        }]
    }, {
        title: "console.common.navigation.services",
        icon:"fa-plug",
        route:"realmsServices"
    }, {
        title: "console.common.navigation.dataStores",
        icon: "fa-database",
        event: "main.navigation.EVENT_REDIRECT_TO_JATO_DATASTORES"
    }, {
        title: "console.common.navigation.privileges",
        icon: "fa-check-square-o",
        event: "main.navigation.EVENT_REDIRECT_TO_JATO_PRIVILEGES"
    }, {
        title: "console.common.navigation.authorization",
        icon: "fa-key",
        children: [{
            title: "console.common.navigation.policySets",
            icon: "fa-angle-right",
            route: "realmsPolicySets"
        }, {
            title: "console.common.navigation.resourceTypes",
            icon: "fa-angle-right",
            route: "realmsResourceTypes"
        }]
    }, {
        title: "console.common.navigation.subjects",
        icon: "fa-users",
        event: "main.navigation.EVENT_REDIRECT_TO_JATO_SUBJECTS"
    }, {
        title: "console.common.navigation.agents",
        icon: "fa-male",
        event: "main.navigation.EVENT_REDIRECT_TO_JATO_AGENTS"
    }, {
        title: "console.common.navigation.sts",
        icon: "fa-ticket",
        event: "main.navigation.EVENT_REDIRECT_TO_JATO_STS"
    }, {
        title: "console.common.navigation.scripts",
        icon: "fa-code",
        route: "realmsScripts"
    }];

    function shortenRealmName (realmPath) {
        let realmName;
        if (realmPath === "/") {
            realmName = $.t("console.common.topLevelRealm");
        } else {
            realmName = _.last(realmPath.split("/"));
        }
        return realmName;
    }

    const RealmTreeNavigationView = TreeNavigation.extend({
        events: {
            "click [data-event]": "sendEvent"
        },
        sendEvent (e) {
            e.preventDefault();
            EventManager.sendEvent($(e.currentTarget).data().event, this.data.realmPath);
        },

        realmExists (path) {
            return RealmsService.realms.get(path);
        },
        render (args, callback) {
            this.data.realmPath = args[0];
            this.data.realmName = shortenRealmName(this.data.realmPath);
            this.data.crumbs = createBreadcrumbs(this.route.pattern);
            this.data.treeNavigation = createTreeNavigation(navData, [encodeURIComponent(this.data.realmPath)]);
            this.data.title = this.data.realmName;
            this.data.home = `#${Router.getLink(
                Router.configuration.routes.realmDefault, [encodeURIComponent(this.data.realmPath)])}`;
            this.data.icon = "fa-cloud";
            this.realmExists(this.data.realmPath).then(() => {
                TreeNavigation.prototype.render.call(this, args, callback);
            }, (xhr) => {
                /**
                 * If a non-existant realm was specified, return to realms list
                 */
                if (xhr.status === 404) {
                    Router.routeTo(Router.configuration.routes.realms, {
                        args: [],
                        trigger: true
                    });
                }
            });
        }
    });

    return new RealmTreeNavigationView();
});
