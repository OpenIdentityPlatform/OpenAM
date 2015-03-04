/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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

/*global require, define*/
define([
    "text!templates/editor/ScriptListTemplate.html",
    "text!templates/editor/login/LoginDialog.html",
    "text!configuration.json"
], function () {
    /* an unfortunate need to duplicate the file names here, but I haven't
     yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [
            "templates/editor/ScriptListTemplate.html",
            "templates/editor/login/LoginDialog.html",
            "configuration.json"
        ],
        deps = arguments;

    return function (server) {
        _.each(staticFiles, function (file, i) {
            server.respondWith(
                "GET",
                new RegExp(file.replace(/([\/\.\-])/g, "\\$1") + "$"),
                [
                    200,
                    { },
                    deps[i]
                ]
            );
        });

        server.respondWith(
            "GET",
            /\/json\/serverinfo\/\*/,
            [
                200,
                {
                    "Date": "Wed, 04 Mar 2015 08:59:05 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;-451943112&quot;",
                    "Content-Length": "447",
                    "Content-API-Version": "protocol=1.0,resource=1.1",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"domains\":[\".esergueeva.com\"],\"protectedUserAttributes\":[],\"cookieName\":\"iPlanetDirectoryPro\",\"secureCookie\":false,\"twoFactorAuthEnabled\":\"false\",\"twoFactorAuthMandatory\":\"false\",\"forgotPassword\":\"false\",\"selfRegistration\":\"false\",\"lang\":\"en\",\"successfulUserRegistrationDestination\":\"default\",\"socialImplementations\":[],\"referralsEnabled\":\"false\",\"zeroPageLogin\":{\"enabled\":false,\"refererWhitelist\":[\"\"],\"allowedWithoutReferer\":true},\"FQDN\":null}"
            ]
        );
    };
});