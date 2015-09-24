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

/*global define, window */
define("config/process/AMConfig", [
    "jquery",
    "underscore",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/delegates/SMSGlobalDelegate",
    "org/forgerock/openam/ui/common/util/ThemeManager",
    "org/forgerock/commons/ui/common/util/URIUtils"
], function ($, _, Constants, EventManager, Router, SMSGlobalDelegate, ThemeManager, URIUtils) {
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

            sessionManager.logout(function () {
                conf.setProperty("loggedUser", null);
                EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true });
                delete conf.gotoURL;
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
        startEvent: Constants.EVENT_SHOW_CONFIRM_PASSWORD_DIALOG,
        description: "",
        dependencies: [
            "org/forgerock/commons/ui/user/profile/ConfirmPasswordDialog"
        ],
        processDescription: function (event, ConfirmPasswordDialog) {
            ConfirmPasswordDialog.show();
        }
    }, {
        startEvent: Constants.EVENT_SHOW_CHANGE_SECURITY_DIALOG,
        override: true,
        dependencies: [
            "org/forgerock/openam/ui/user/profile/ChangeSecurityDataDialog"
        ],
        processDescription: function (event, ChangeSecurityDataDialog) {
            ChangeSecurityDataDialog.show(event);
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
        startEvent: Constants.EVENT_AUTHENTICATED,
        description: "",
        dependencies: [
            "underscore",
            "org/forgerock/commons/ui/common/main/Configuration",
            "org/forgerock/commons/ui/common/components/Navigation"
        ],
        processDescription: function (event, _, Configuration, Navigation) {
            ThemeManager.getTheme(true);
            if (_.contains(Configuration.loggedUser.roles, "ui-admin")) {
                Navigation.addLink({
                    "url": "#" + Router.getLink(Router.configuration.routes.realmDefault,
                                                [encodeURIComponent("/")]),
                    "name": $.t("console.common.topLevelRealm"),
                    "cssClass": "dropdown-sub"
                }, "admin", "realms");

                Navigation.addLink({
                    "url": "#realms",
                    "name": $.t("config.AppConfiguration.Navigation.links.realms.viewAll"),
                    "cssClass": "dropdown-sub"
                }, "admin", "realms");

                SMSGlobalDelegate.realms.all().done(function (data) {
                    var urls = Navigation.configuration.links.admin.urls.realms.urls,
                        realms = [];

                    _.forEach(data.result, function (realm) {
                        if (realm.active === true && realm.path !== "/" && realms.length < 2) {
                            realms.push({
                                "url": "#" + Router.getLink(Router.configuration.routes.realmDefault,
                                                            [encodeURIComponent(realm.path)]),
                                "name": realm.name,
                                "cssClass": "dropdown-sub"
                            });
                        }
                    });

                    urls.splice.apply(urls, [-1, 0].concat(realms));

                    Navigation.reload();
                });

                Navigation.reload();
            }
        }
    }];
});
