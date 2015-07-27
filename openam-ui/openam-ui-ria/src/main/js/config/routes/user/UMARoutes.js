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

/*global define*/
define("config/routes/user/UMARoutes", function () {
    return {
        "umaResources": {
            view: "org/forgerock/openam/ui/uma/views/resource/LabelTreeNavigationView",
            page: "org/forgerock/openam/ui/uma/views/resource/ListResourcesPage",
            url: /^uma\/resources\/?(.+)?\/?$/,
            pattern: "uma/resources/?",
            role: "ui-user",
            defaults: ["myresources"],
            forceUpdate: true
        },
        "umaResourceEdit": {
            view: "org/forgerock/openam/ui/uma/views/resource/LabelTreeNavigationView",
            page: "org/forgerock/openam/ui/uma/views/resource/EditResourcePage",
            url: /^uma\/resources\/(.+)\/([^\/]+)\/?$/,
            role: "ui-user",
            pattern: "uma/resources/?/?",
            forceUpdate: true
        },
        "umaHistory": {
            view: "org/forgerock/openam/ui/uma/views/history/ListHistory",
            role: "ui-user",
            url: /^uma\/history\/?$/,
            pattern: "uma/history"
        },
        "umaRequestEdit": {
            view: "org/forgerock/openam/ui/uma/views/request/EditRequest",
            role: "ui-user",
            url: /^uma\/requests\/(.*?)(?:\/){0,1}$/,
            pattern: "uma/requests/?"
        },
        "umaRequestList": {
            view: "org/forgerock/openam/ui/uma/views/request/ListRequest",
            role: "ui-user",
            defaults: [""],
            url: /^uma\/requests\/?$/,
            pattern: "uma/requests/"
        },
        "umaBaseShare": {
            view: "org/forgerock/openam/ui/uma/views/share/BaseShare",
            url: /^uma\/share\/(.*?)(?:\/){0,1}$/,
            pattern: "uma/share/?",
            defaults: [""],
            role: "ui-user"
        }
    };
});
