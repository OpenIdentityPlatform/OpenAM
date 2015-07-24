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
        "uma": {
            view: "org/forgerock/openam/ui/uma/views/resource/ListResource",
            url: /^uma/,
            pattern: "uma/resources/",
            role: "ui-user"
        },
        "editResource": {
            view: "org/forgerock/openam/ui/uma/views/resource/EditResource",
            url: /^uma\/resources\/(.*?)(?:\/){0,1}$/,
            role: "ui-user",
            pattern: "uma/resources/?"
        },
        "listResource": {
            view: "org/forgerock/openam/ui/uma/views/resource/ListResource",
            url: /^uma\/resources\/$/,
            defaults: [""],
            role: "ui-user",
            pattern: "uma/resources/"
        },
        "baseShare": {
            view: "org/forgerock/openam/ui/uma/views/share/BaseShare",
            url: /^uma\/share\/(.*?)(?:\/){0,1}$/,
            pattern: "uma/share/?",
            defaults: [""],
            role: "ui-user"
        },
        "listHistory": {
            view: "org/forgerock/openam/ui/uma/views/history/ListHistory",
            role: "ui-user",
            url: /^uma\/history\/?$/,
            pattern: "uma/history/"
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
        }
    };
});
