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
 * Copyright 2015 ForgeRock AS.
 */


define("org/forgerock/openam/ui/dashboard/views/DashboardView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/dashboard/views/DeviceManagementView",
    "org/forgerock/openam/ui/dashboard/views/MyApplicationsView",
    "org/forgerock/openam/ui/dashboard/views/OAuthTokensView",
    "org/forgerock/openam/ui/dashboard/views/TrustedDevicesView"
], function ($, _, AbstractView, DeviceManagementView, MyApplicationsView, OAuthTokensView, TrustedDevicesView) {
    var Dashboard = AbstractView.extend({
        template: "templates/openam/dashboard/DashboardTemplate.html",
        render: function () {
            this.parentRender(function () {
                MyApplicationsView.render();
                TrustedDevicesView.render();
                OAuthTokensView.render();
                DeviceManagementView.render();
            });
        }
    });

    return new Dashboard();
});
