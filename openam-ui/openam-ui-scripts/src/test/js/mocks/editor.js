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

/*global require, define*/
define([
    "text!templates/editor/views/EditScriptTemplate.html",
    "text!templates/editor/views/ChangeContextTemplate.html",
    "text!templates/editor/views/ScriptListTemplate.html",
    "text!templates/editor/views/ScriptListBtnToolbarTemplate.html",
    "text!templates/editor/views/ScriptValidationTemplate.html",
    "text!templates/editor/login/LoginDialog.html",
    "text!configuration.json"
], function () {
    /* an unfortunate need to duplicate the file names here, but I haven't
     yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [
            "templates/editor/views/EditScriptTemplate.html",
            "templates/editor/views/ChangeContextTemplate.html",
            "templates/editor/views/ScriptListTemplate.html",
            "templates/editor/views/ScriptListBtnToolbarTemplate.html",
            "templates/editor/views/ScriptValidationTemplate.html",
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

        server.respondWith(
            "GET",
            /\/json\/scripts/,
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
                '{"result":[{"_id":"c20fa877-e9b5-486e-b555-1396ae0d7b76","name":"qqq","description":"null","script":"dmFyIHg9MTs=","language":"GROOVY","context":"POLICY_CONDITION","createdBy":"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org","creationDate":1430918340590,"lastModifiedBy":"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org","lastModifiedDate":1431158440665}],"resultCount":1,"pagedResultsCookie":null,"remainingPagedResults":-1}'
            ]
        );

        server.respondWith(
            "GET",
            /\/json\/scripts\/c20fa877-e9b5-486e-b555-1396ae0d7b76/,
            [
                200,
                { },
                '{"_id":"c20fa877-e9b5-486e-b555-1396ae0d7b76","name":"qqq","description":"null","script":"dmFyIHg9MTs=","language":"GROOVY","context":"POLICY_CONDITION","createdBy":"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org","creationDate":1430918340590,"lastModifiedBy":"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org","lastModifiedDate":1431158440665}'
            ]
        );

        server.respondWith(
            "POST",
            /\/json\/scripts\/\?_action=validate/,
            [
                200,
                { },
                '{"success":true}'
            ]
        );

        server.respondWith(
            "GET",
            /\/json\/global-config\/services\/scripting\/contexts/,
            [
                200,
                { },
                '{"result":[{"defaultScript":"Ly8gU2VydmVyLXNpZGUgQXV0aG9yaXphdGlvbiBFbnRpdGxlbWVudCBDb25kaXRpb25zIHNjcmlwdA==","languages":["GROOVY","JAVASCRIPT"],"_id":"POLICY_CONDITION","defaultLanguage":"JAVASCRIPT"},{"defaultScript":"dmFyIFNUQVJUX1RJTUUgPSA5OyAgLy8gOWFtCnZhciBFTkRfVElNRSAgID0gMTc7IC8vIDVwbQoKbG9nZ2VyLm1lc3NhZ2UoIlN0YXJ0aW5nIGF1dGhlbnRpY2F0aW9uIGphdmFzY3JpcHQiKTsKbG9nZ2VyLm1lc3NhZ2UoIlVzZXI6ICIgKyB1c2VybmFtZSk7CgovLyBMb2cgb3V0IGN1cnJlbnQgY29va2llcyBpbiB0aGUgcmVxdWVzdAppZiAobG9nZ2VyLm1lc3NhZ2VFbmFibGVkKCkpIHsKICAgIHZhciBjb29raWVzID0gcmVxdWVzdERhdGEuZ2V0SGVhZGVycygnQ29va2llJyk7CiAgICBmb3IgKGNvb2tpZSBpbiBjb29raWVzKSB7CiAgICAgICAgbG9nZ2VyLm1lc3NhZ2UoJ0Nvb2tpZTogJyArIGNvb2tpZXNbY29va2llXSk7CiAgICB9Cn0KCmlmICh1c2VybmFtZSkgewogICAgLy8gRmV0Y2ggdXNlciBpbmZvcm1hdGlvbiB2aWEgUkVTVAogICAgdmFyIHJlc3BvbnNlID0gaHR0cENsaWVudC5nZXQoImh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9vcGVuYW0vanNvbi91c2Vycy8iICsgdXNlcm5hbWUsIHsKICAgICAgICBjb29raWVzIDogW10sCiAgICAgICAgaGVhZGVycyA6IFtdCiAgICB9KTsKICAgIC8vIExvZyBvdXQgcmVzcG9uc2UgZnJvbSBSRVNUIGNhbGwKICAgIGxvZ2dlci5tZXNzYWdlKCJVc2VyIFJFU1QgQ2FsbC4gU3RhdHVzOiAiICsgcmVzcG9uc2UuZ2V0U3RhdHVzQ29kZSgpICsgIiwgQm9keTogIiArIHJlc3BvbnNlLmdldEVudGl0eSgpKTsKfQoKdmFyIG5vdyA9IG5ldyBEYXRlKCk7CmxvZ2dlci5tZXNzYWdlKCJDdXJyZW50IHRpbWU6ICIgKyBub3cuZ2V0SG91cnMoKSk7CmlmIChub3cuZ2V0SG91cnMoKSA8IFNUQVJUX1RJTUUgfHwgbm93LmdldEhvdXJzKCkgPiBFTkRfVElNRSkgewogICAgbG9nZ2VyLmVycm9yKCJMb2dpbiBmb3JiaWRkZW4gb3V0c2lkZSB3b3JrIGhvdXJzISIpOwogICAgYXV0aFN0YXRlID0gRkFJTEVEOwp9IGVsc2UgewogICAgbG9nZ2VyLm1lc3NhZ2UoIkF1dGhlbnRpY2F0aW9uIGFsbG93ZWQhIik7CiAgICBhdXRoU3RhdGUgPSBTVUNDRVNTOwp9","languages":["GROOVY","JAVASCRIPT"],"_id":"AUTHENTICATION_SERVER_SIDE","defaultLanguage":"JAVASCRIPT"},{"defaultScript":"Ly8gQ2xpZW50LXNpZGUgc2NyaXB0","languages":["JAVASCRIPT"],"_id":"AUTHENTICATION_CLIENT_SIDE","defaultLanguage":"JAVASCRIPT"}],"resultCount":3,"pagedResultsCookie":null,"remainingPagedResults":-1}'
            ]
        );

        server.respondWith(
            "GET",
            /\/json\/global-config\/services\/scripting$/,
            [
                200,
                { },
                '{"defaults":{},"defaultContext":"POLICY_CONDITION"}'
            ]
        );
    };
});