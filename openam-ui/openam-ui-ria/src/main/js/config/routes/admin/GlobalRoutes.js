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

define("config/routes/admin/GlobalRoutes", [], function () {
    return {
        configurationSystem: {
            view: "org/forgerock/openam/ui/admin/views/global/SystemConfigurationListView",
            url: /configuration\/system/,
            pattern: "configuration/system",
            role: "ui-global-admin",
            navGroup: "admin"
        },
        configurationAuthentication: {
            view: "org/forgerock/openam/ui/admin/views/global/AuthenticationConfigurationListView",
            url: /configuration\/authentication/,
            pattern: "configuration/authentication",
            role: "ui-global-admin",
            navGroup: "admin"
        },
        configurationConsole: {
            view: "org/forgerock/openam/ui/admin/views/global/ConsoleConfigurationListView",
            url: /configuration\/console/,
            pattern: "configuration/console",
            role: "ui-global-admin",
            navGroup: "admin"
        }
    };
});
