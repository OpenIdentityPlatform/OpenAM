/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/**
 * "Portions Copyrighted 2011-2013 ForgeRock Inc"
 */
/*global define*/
/*jslint regexp:false */

/**
 * @author jfeasel
 */
define("config/routes/AMRoutesConfig", [
    "org/forgerock/openam/ui/common/util/Constants"
], function(Constants) {

    var obj = {
        "forgotPassword": {
            view: "org/forgerock/openam/ui/user/profile/ForgotPasswordView",
            url: /forgotPassword(\/[^\&]*)(\&.+)?/,
            pattern: "forgotPassword??",
            forceUpdate: true,
            defaults: ["/",""],
            argumentNames: ["realm", "additionalParameters"]
        },
        "forgotPasswordChange": {
            view: "org/forgerock/openam/ui/user/profile/ForgotPasswordView",
            url: /forgotPasswordChange(\/[^\&]*)(\&.+)?/,
            pattern: "forgotPasswordChange??",
            forceUpdate: true,
            defaults: ["/",""],
            argumentNames: ["realm", "additionalParameters"]
        },
        "continueSelfRegister": {
            view: "org/forgerock/openam/ui/user/profile/RegisterView",
            url: /continueRegister(\/[^\&]*)(\&.+)?/,
            pattern: "continueRegister??",
            forceUpdate: true,
            defaults: ["/",""],
            argumentNames: ["realm", "additionalParameters"]
        },
        "confirmLogin": {
            view: "org/forgerock/openam/ui/user/login/RESTConfirmLoginView",
            role: "ui-user",
            url: "confirmLogin/" ,
            forceUpdate: true
        },
        "dashboard": {
            view: "org/forgerock/openam/ui/dashboard/DashboardView",
            role: "ui-user",
            url: "dashboard/",
            forceUpdate: true
        },
        "oauth2Tokens": {
            view: "org/forgerock/openam/ui/user/oauth2/TokensView",
            role: "ui-user",
            url: "oauth2/tokens",
            forceUpdate: true
        },
        "loggedOut" : {
            view: "org/forgerock/openam/ui/user/logout/RESTLogoutView",
            url: /loggedOut([^\&]+)?(&.+)?/,
            pattern: "loggedOut??",
            defaults: ["/",""],
            argumentNames: ["realm","additionalParameters"]
        },

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
        /*
        "listSubject": {
            view: "org/forgerock/openam/ui/uma/views/subject/SubjectListView",
            url: /^uma\/resources\/(.+?)\/(users)\//,
            role: "ui-user",
            pattern: "uma/resources/?/users/"
        },*/
        "listHistory": {
            view: "org/forgerock/openam/ui/uma/views/history/ListHistory",
            role: "ui-user",
            url: /^uma\/history\/$/,
            pattern: "uma/history/"
        },
        "listSubject": {
            view: "org/forgerock/openam/ui/uma/views/subjects/ListSubject",
            role: "ui-user",
            url: /^uma\/users\/$/,
            pattern: "uma/users/"
        },
        "listApplication": {
            view: "org/forgerock/openam/ui/uma/views/application/ListApplication",
            role: "ui-user",
            defaults: [""],
            url: /^uma\/apps\/(.*?)(?:\/){0,1}$/,
            pattern: "uma/apps/?"
        },

        // Common Tasks
        "commonTasks": {
            view: "org/forgerock/openam/ui/admin/views/commonTasks/CommonTasksView",
            url: /^commonTasks\/$/,
            pattern: "commonTasks/",
            role: "ui-admin"
        },

        // Realms
        "realms": {
            view: "org/forgerock/openam/ui/admin/views/realms/RealmsView",
            url: /^realms\/$/,
            pattern: "realms/",
            role: "ui-admin"
        },
        "realmGeneral": {
            view: "org/forgerock/openam/ui/admin/views/realms/GeneralView",
            url: /^realms\/(.*?)\/general\/$/,
            pattern: "realms/?/general/",
            role: "ui-admin"
        },

        "authentication": {
            view: "org/forgerock/openam/ui/admin/views/console/realms/authentication/Authentication",
            url: /^realms\/authentication/,
            pattern: "realms/authentication/",
            role: "ui-admin"
        },
        "advancedSettings": {
            view: "org/forgerock/openam/ui/admin/views/console/realms/authentication/advanced/AdvancedSettings",
            url: /^realms\/authentication\/advanced/,
            pattern: "realms/authentication/advanced/",
            role: "ui-admin"
        },
        "chains": {
            view: "org/forgerock/openam/ui/admin/views/console/realms/authentication/chains/Chains",
            url: /^console\/realms\/authentication\/chains\/(.*?)(?:\/){0,1}$/,
            pattern: "console/realms/authentication/chains/?",
            defaults: [""],
            role: "ui-admin"
        },
        "module": {
            view: "org/forgerock/openam/ui/admin/views/console/realms/authentication/modules/Modules",
            url: /^realms\/authentication\/modules\/(.*?)(?:\/){0,1}$/,
            pattern: "realms/authentication/modules/?",
            defaults: [""],
            role: "ui-user"
        },

        // Federation
        "federation": {
            view: "org/forgerock/openam/ui/admin/views/federation/FederationView",
            url: /^federation\/$/,
            pattern: "federation/",
            role: "ui-admin"
        },

        // Configuration
        "configuration": {
            view: "org/forgerock/openam/ui/admin/views/configuration/ConfigurationView",
            url: /^configuration\/$/,
            pattern: "configuration/",
            role: "ui-admin"
        },

        // Sessions
        "sessions": {
            view: "org/forgerock/openam/ui/admin/views/sessions/SessionsView",
            url: /^sessions\/$/,
            pattern: "sessions/",
            role: "ui-admin"
        }
    };

    return obj;
});
