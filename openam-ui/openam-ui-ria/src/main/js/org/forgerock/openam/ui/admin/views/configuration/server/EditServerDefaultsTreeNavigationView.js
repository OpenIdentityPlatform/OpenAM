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

define([
    "jquery",
    "org/forgerock/openam/ui/common/components/TreeNavigation",
    "org/forgerock/openam/ui/admin/views/common/navigation/createTreeNavigation",
    "org/forgerock/commons/ui/common/main/Router"
], ($, TreeNavigation, createTreeNavigation, Router) => {

    const navData = [{
        title: "console.common.navigation.general",
        icon: "fa-cog",
        route: "editServerDefaultsGeneral"
    }, {
        title: "console.common.navigation.security",
        icon: "fa-lock",
        route: "editServerDefaultsSecurity"
    }, {
        title: "console.common.navigation.session",
        icon: "fa-key",
        route: "editServerDefaultsSession"
    }, {
        title: "console.common.navigation.sdk",
        icon: "fa-th",
        route: "editServerDefaultsSdk"
    }, {
        title: "console.common.navigation.cts",
        icon: "fa-database",
        route: "editServerDefaultsCts"
    }, {
        title: "console.common.navigation.uma",
        icon: "fa-check-circle-o",
        route: "editServerDefaultsUma"
    }, {
        title: "console.common.navigation.advanced",
        icon: "fa-cogs",
        route: "editServerDefaultsAdvanced"
    }];

    const EditServerDefaultsTreeNavigationView = TreeNavigation.extend({
        render (args, callback) {
            this.data.treeNavigation = createTreeNavigation(navData);
            this.data.title = $.t("console.common.navigation.serverDefaults");
            this.data.home = `#${Router.getLink(Router.configuration.routes.editServerDefaultsGeneral, args)}`;
            TreeNavigation.prototype.render.call(this, args, callback);
        }
    });

    return new EditServerDefaultsTreeNavigationView();
});
