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
    return {
        // My Resources
        "umaResourcesMyResources": {
            view: "org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView",
            page: "org/forgerock/openam/ui/user/uma/views/resource/MyResourcesPage",
            url: /^uma\/resources\/?(myresources)?\/?([^\/]+)?\/?$/,
            pattern: "uma/resources/?/?",
            role: "ui-uma-user",
            navGroup: "user",
            defaults: ["myresources", ""],
            forceUpdate: true
        },
        "umaResourcesMyResourcesResource": {
            view: "org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView",
            page: "org/forgerock/openam/ui/user/uma/views/resource/ResourcePage",
            url: /^uma\/resources\/myresources\/([^\/]+)\/([^\/]+)\/?$/,
            role: "ui-uma-user",
            navGroup: "user",
            pattern: "uma/resources/myresources/?/?",
            defaults: [""],
            forceUpdate: true
        },

        // Shared with me
        "umaResourcesSharedWithMe": {
            view: "org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView",
            page: "org/forgerock/openam/ui/user/uma/views/resource/SharedWithMePage",
            url: /^uma\/resources\/sharedwithme\/?$/,
            pattern: "uma/resources/sharedwithme",
            role: "ui-uma-user",
            navGroup: "user",
            forceUpdate: true
        },
        "umaResourcesSharedWithMeResource": {
            view: "org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView",
            page: "org/forgerock/openam/ui/user/uma/views/resource/ResourcePage",
            url: /^uma\/resources\/sharedwithme\/([^\/]+)\/?$/,
            role: "ui-uma-user",
            navGroup: "user",
            pattern: "uma/resources/sharedwithme/?",
            defaults: [""],
            forceUpdate: true
        },

        // Starred
        "umaResourcesStarred": {
            view: "org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView",
            page: "org/forgerock/openam/ui/user/uma/views/resource/StarredPage",
            url: /^uma\/resources\/starred\/?$/,
            pattern: "uma/resources/starred",
            role: "ui-uma-user",
            navGroup: "user",
            forceUpdate: true
        },
        "umaResourcesStarredResource": {
            view: "org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView",
            page: "org/forgerock/openam/ui/user/uma/views/resource/ResourcePage",
            url: /^uma\/resources\/starred\/([^\/]+)\/?$/,
            role: "ui-uma-user",
            navGroup: "user",
            pattern: "uma/resources/starred/?",
            defaults: [""],
            forceUpdate: true
        },

        // My Labels
        "umaResourcesMyLabels": {
            view: "org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView",
            page: "org/forgerock/openam/ui/user/uma/views/resource/MyLabelsPage",
            url: /^uma\/resources\/mylabels\/([^\/]+)\/?$/,
            pattern: "uma/resources/mylabels/?",
            role: "ui-uma-user",
            navGroup: "user",
            defaults: [""],
            forceUpdate: true
        },
        "umaResourcesMyLabelsResource": {
            view: "org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView",
            page: "org/forgerock/openam/ui/user/uma/views/resource/ResourcePage",
            url: /^uma\/resources\/mylabels\/([^\/]+)\/([^\/]+)\/?$/,
            role: "ui-uma-user",
            navGroup: "user",
            pattern: "uma/resources/mylabels/?/?",
            defaults: ["", ""],
            forceUpdate: true
        },

        // History
        "umaHistory": {
            view: "org/forgerock/openam/ui/user/uma/views/history/ListHistory",
            role: "ui-uma-user",
            navGroup: "user",
            url: /^uma\/history\/?$/,
            pattern: "uma/history"
        },
        // Requests
        "umaRequestEdit": {
            view: "org/forgerock/openam/ui/user/uma/views/request/EditRequest",
            role: "ui-uma-user",
            navGroup: "user",
            url: /^uma\/requests\/(.*?)(?:\/){0,1}$/,
            pattern: "uma/requests/?"
        },
        "umaRequestList": {
            view: "org/forgerock/openam/ui/user/uma/views/request/ListRequest",
            role: "ui-uma-user",
            navGroup: "user",
            defaults: [""],
            url: /^uma\/requests\/?$/,
            pattern: "uma/requests/"
        },
        // Share
        "umaBaseShare": {
            view: "org/forgerock/openam/ui/user/uma/views/share/BaseShare",
            url: /^uma\/share\/(.*?)(?:\/){0,1}$/,
            pattern: "uma/share/?",
            defaults: [""],
            role: "ui-uma-user",
            navGroup: "user"
        }
    };
});
