/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014-2015 ForgeRock AS.
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
], function (Constants, EventManager) {
    var obj = [
        {
            startEvent: Constants.EVENT_HANDLE_DEFAULT_ROUTE,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router"
            ],
            processDescription: function (event, router) {
                EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.manageApps});
            }
        },
        {
            startEvent: Constants.EVENT_RETURN_TO_AM_CONSOLE,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Configuration"
            ],
            processDescription: function (event, conf) {
                var realm = conf.globalData.auth.realm;
                window.location.href = "/" + Constants.context + "/realm/RMRealm?RMRealm.tblDataActionHref=" + encodeURIComponent(realm);
            }
        },
        {
            startEvent: Constants.EVENT_GO_TO_SCRIPTS_EDITOR,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Configuration"
            ],
            processDescription: function (event, conf) {
                var realm = conf.globalData.auth.realm !== '/' ? '?realm=' + conf.globalData.auth.realm : '';
                window.location.href = "/" + Constants.context + "/scripts" + realm + "#list";
            }
        },
        {
            startEvent: Constants.EVENT_UNAUTHORIZED,
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
                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true});
                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.login });
                } else {
                    viewManager.showDialog(router.configuration.routes.loginDialog.dialog);
                }
            }
        }
    ];
    return obj;
});