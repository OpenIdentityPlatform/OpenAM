/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global define, require, window, _*/

define("config/process/PolicyConfig", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager"
], function (constants, eventManager) {
    var obj = [
        {
            startEvent: constants.EVENT_HANDLE_DEFAULT_ROUTE,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router"
            ],
            processDescription: function (event, router) {
                eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.manageApps});
            }
        },
        {
            startEvent: constants.EVENT_RETURN_TO_AM_CONSOLE,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Configuration"
            ],
            processDescription: function (event, conf) {
                var realm = conf.globalData.auth.realm;
                window.location.href = "/" + constants.context + "/realm/RMRealm?RMRealm.tblDataActionHref=" +
                        encodeURIComponent(realm);
            }
        },
        {
            startEvent: constants.EVENT_UNAUTHORIZED,
            override: true,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/ViewManager",
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/main/SessionManager",
                "org/forgerock/commons/ui/common/util/UIUtils",
                "LoginDialog"
            ],
            processDescription: function(error, viewManager, router, conf, sessionManager, uiUtils, loginDialog) {
                var saveGotoURL = function () {
                    var hash = uiUtils.getCurrentHash();
                    if(!conf.gotoURL && !hash.match(router.configuration.routes.login.url)) {
                        conf.setProperty("gotoURL", "#" + hash);
                    }
                };

                // multiple rest calls that all return authz failures will cause this event to be called multiple times
                if (conf.globalData.authorizationFailurePending !== undefined) {
                    return;
                }

                conf.globalData.authorizationFailurePending = true;

                if(!conf.loggedUser) {
                    saveGotoURL();
                    eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true});
                    eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.login });
                    return;
                } else {
                    viewManager.showDialog(router.configuration.routes.loginDialog.dialog);
                }
            }
        }
    ];
    return obj;
});
