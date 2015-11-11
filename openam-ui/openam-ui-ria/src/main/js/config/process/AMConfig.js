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
 * Portions copyright 2011-2015 ForgeRock AS.
 */

define("config/process/AMConfig", [
    "jquery",
    "underscore",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/common/util/ThemeManager",
    "org/forgerock/commons/ui/common/util/URIUtils"
], function ($, _, Constants, EventManager, Router, ThemeManager, URIUtils) {
    return [{
        startEvent: Constants.EVENT_LOGOUT,
        description: "used to override common logout event",
        override: true,
        dependencies: [
            "org/forgerock/commons/ui/common/main/Router",
            "org/forgerock/commons/ui/common/main/Configuration",
            "org/forgerock/commons/ui/common/main/SessionManager"
        ],
        processDescription: function (event, router, conf, sessionManager) {
            var argsURLFragment = event ? (event.args ? event.args[0] : "") : "",
                urlParams = URIUtils.parseQueryString(argsURLFragment),
                gotoURL = urlParams.goto;

            sessionManager.logout(function (response) {
                conf.setProperty("loggedUser", null);
                EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true });
                delete conf.gotoURL;

                if (!gotoURL && response) {
                    gotoURL = response.goto;
                }

                if (gotoURL) {
                    Router.setUrl(decodeURIComponent(gotoURL));
                } else {
                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                        route: router.configuration.routes.loggedOut
                    });
                }
                ThemeManager.getTheme(true);
            }, function () {
                conf.setProperty("loggedUser", null);
                EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true });
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unauthorized");
                if (gotoURL) {
                    Router.setUrl(decodeURIComponent(gotoURL));
                } else {
                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                        route: router.configuration.routes.login
                    });
                }
            });
        }
    }, {
        startEvent: Constants.EVENT_INVALID_REALM,
        override: true,
        dependencies: [
            "org/forgerock/commons/ui/common/main/Router",
            "org/forgerock/commons/ui/common/main/Configuration"
        ],
        processDescription: function (event, router, conf) {
            if (event.error.responseJSON.message.indexOf("Invalid realm") > -1) {
                if (conf.baseTemplate) {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidRealm");
                } else {
                    router.navigate("login", { trigger: true });
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidRealm");
                }
            }
        }
    }, {
        startEvent: Constants.EVENT_ADD_NEW_REALM_DIALOG,
        override: true,
        dependencies: [
            "org/forgerock/commons/ui/common/main/Router",
            "org/forgerock/openam/ui/admin/views/realms/CreateUpdateRealmDialog",
            "org/forgerock/openam/ui/admin/views/realms/RealmsListView",
            "org/forgerock/openam/ui/admin/delegates/SMSGlobalDelegate"
        ],
        processDescription: function (event, Router, CreateUpdateRealmDialog, RealmsListView, SMSGlobalDelegate) {
            Router.routeTo(Router.configuration.routes.realms, {
                args: [],
                trigger: true
            });

            SMSGlobalDelegate.realms.all().done(function (data) {
                var allRealmPaths = [];
                _.each(data.result, function (realm) {
                    allRealmPaths.push(realm.path);
                });

                CreateUpdateRealmDialog.show({
                    allRealmPaths : allRealmPaths,
                    callback : function () {
                        RealmsListView.render();
                    }
                });
            });
        }
    }, {
        startEvent: Constants.EVENT_RETURN_TO_AM_CONSOLE,
        description: "",
        dependencies: [
            "org/forgerock/commons/ui/common/main/Configuration"
        ],
        processDescription: function (event, conf) {
            var subRealm = conf.globalData.auth.subRealm || "/";
            window.location.href = "/" + Constants.context + "/realm/RMRealm?RMRealm.tblDataActionHref=" +
                                   encodeURIComponent(subRealm);
        }
    }, {
        startEvent: Constants.EVENT_HANDLE_DEFAULT_ROUTE,
        description: "",
        dependencies: [
            "org/forgerock/commons/ui/common/main/Router"
        ],
        processDescription: function (event, Router) {
            Router.routeTo(Router.configuration.routes.profile, { trigger: true });
        }
    }, {
        startEvent: Constants.EVENT_THEME_CHANGED,
        description: "",
        dependencies: [
            "Footer",
            "org/forgerock/commons/ui/common/components/LoginHeader"
        ],
        processDescription: function (event, Footer, LoginHeader) {
            Footer.render();
            LoginHeader.render();
        }
    }, {
        startEvent: Constants.EVENT_AUTHENTICATED,
        description: "",
        dependencies: [
            "underscore",
            "org/forgerock/commons/ui/common/main/Configuration",
            "org/forgerock/openam/ui/common/util/NavigationHelper"
        ],
        processDescription: function (event, _, Configuration, NavigationHelper) {
            ThemeManager.getTheme(true);
            if (_.contains(Configuration.loggedUser.uiroles, "ui-admin")) {
                NavigationHelper.setAdminNav();
            }
        }
    },
    {
        startEvent: Constants.EVENT_UNAUTHORIZED,
        description: "",
        override: true,
        dependencies: [
            "org/forgerock/commons/ui/common/main/Router",
            "org/forgerock/commons/ui/common/main/Configuration",
            "org/forgerock/commons/ui/common/main/SessionManager"
        ],
        processDescription: function (event, Router, Configuration, SessionManager) {
            var loggedIn = Configuration.loggedUser,
                setGoToUrlProperty = function () {
                    var hash = Router.getCurrentHash();
                    if (!Configuration.gotoURL && !hash.match(Router.configuration.routes.login.url)) {
                        Configuration.setProperty("gotoURL", "#" + hash);
                    }
                },
                forbiddenPage = function () {
                    delete Configuration.globalData.authorizationFailurePending;
                    return EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                        route: {
                            view: "org/forgerock/openam/ui/common/views/error/ForbiddenView",
                            url: /.*/
                        },
                        fromRouter: true
                    });
                },
                forbiddenError = function () {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unauthorized");
                },
                logout = function () {
                    setGoToUrlProperty();

                    return SessionManager.logout().then(function () {
                        EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, {
                            anonymousMode: true
                        });
                        return EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                            route: Router.configuration.routes.login
                        });
                    });

                },
                loginDialog = function () {
                    return EventManager.sendEvent(Constants.EVENT_SHOW_LOGIN_DIALOG);
                };

            // Multiple rest calls that all return authz failures will cause this event to be called multiple times
            if (Configuration.globalData.authorizationFailurePending !== undefined) {
                return;
            }
            Configuration.globalData.authorizationFailurePending = true;


            if (!loggedIn) {
                // 401 no session
                return logout();
            } else if (_.get(event, "error.status") === 401) {
                // 401 session timeout
                return loginDialog();
            } else if (event.fromRouter) {
                // 403 route change
                return forbiddenPage();
            } else {
                // 403 rest call
                return forbiddenError();
            }
        }
    }];
});
