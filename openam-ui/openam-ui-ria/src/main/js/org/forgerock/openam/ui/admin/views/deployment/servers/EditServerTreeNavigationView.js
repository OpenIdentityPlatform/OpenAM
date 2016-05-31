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
    "org/forgerock/openam/ui/common/components/TreeNavigation",
    "org/forgerock/openam/ui/admin/views/common/navigation/createBreadcrumbs",
    "org/forgerock/openam/ui/admin/views/common/navigation/createTreeNavigation",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/services/global/ServersService"
], (TreeNavigation, createBreadcrumbs, createTreeNavigation, Router, ServersService) => {

    const navData = [{
        title: "console.common.navigation.general",
        icon: "fa-cog",
        route: "editServerGeneral"
    }, {
        title: "console.common.navigation.security",
        icon: "fa-lock",
        route: "editServerSecurity"
    }, {
        title: "console.common.navigation.session",
        icon: "fa-key",
        route: "editServerSession"
    }, {
        title: "console.common.navigation.sdk",
        icon: "fa-th",
        route: "editServerSdk"
    }, {
        title: "console.common.navigation.cts",
        icon: "fa-database",
        route: "editServerCts"
    }, {
        title: "console.common.navigation.uma",
        icon: "fa-check-circle-o",
        route: "editServerUma"
    }, {
        title: "console.common.navigation.advanced",
        icon: "fa-cogs",
        route: "editServerAdvanced"
    }, {
        title: "console.common.navigation.directoryConfiguration",
        icon: "fa-folder-open",
        route: "editServerDirectoryConfiguration"
    }];

    const EditServerTreeNavigationView = TreeNavigation.extend({
        render (args, callback) {
            const serverName = args[0];
            ServersService.servers.getUrl(serverName).always((url) => {
                this.data.treeNavigation = createTreeNavigation(navData, args);
                this.data.title = url || serverName;
                this.data.home = `#${Router.getLink(Router.configuration.routes.editServerGeneral, [serverName])}`;
                this.data.icon = "fa-server";
                TreeNavigation.prototype.render.call(this, args, callback);
            });
        }
    });

    return new EditServerTreeNavigationView();
});
