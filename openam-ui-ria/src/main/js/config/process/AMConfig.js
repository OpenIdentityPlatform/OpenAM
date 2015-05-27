/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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

define("config/process/AMConfig", [
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function(Constants, EventManager, UIUtils) {
    var obj = [
        {
            startEvent: Constants.EVENT_LOGOUT,
            description: "used to override common logout event",
            override: true,
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/main/SessionManager"
            ],
            processDescription: function(event, router, conf, sessionManager) {
                var argsURLFragment = event ? (event.args ? event.args[0] : '') : '',
                    urlParams = UIUtils.convertQueryParametersToJSON(argsURLFragment),
                    gotoURL = urlParams.goto;

                sessionManager.logout(function() {
                    conf.setProperty('loggedUser', null);
                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true});
                    delete conf.gotoURL;
                    if (gotoURL) {
                        UIUtils.setUrl(decodeURIComponent(gotoURL));
                    } else {
                        EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.loggedOut });
                    }
                }, function(){
                    conf.setProperty('loggedUser', null);
                    EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true});
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unauthorized");
                    if (gotoURL) {
                        UIUtils.setUrl(decodeURIComponent(gotoURL));
                    } else {
                        EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.login });
                    }
                });
            }
        },
        {
            startEvent: Constants.EVENT_INVALID_REALM,
            override: true,
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/Configuration"
            ],
            processDescription: function(event, router, conf) {
                if (event.error.responseJSON.message.indexOf('Invalid realm') > -1 ) {
                    if (conf.baseTemplate) {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidRealm");
                    }
                    else {
                        router.navigate('login', {trigger: true});
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidRealm");
                    }
                }
            }
        },
        {

            startEvent: Constants.EVENT_SHOW_CONFIRM_PASSWORD_DIALOG,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/user/profile/ConfirmPasswordDialog"
            ],
            processDescription: function(event, ConfirmPasswordDialog) {
                ConfirmPasswordDialog.show();
            }
        },
        {
            startEvent: Constants.EVENT_SHOW_CHANGE_SECURITY_DIALOG,
            override: true,
            dependencies: [
                "org/forgerock/openam/ui/user/profile/ChangeSecurityDataDialog"
            ],
            processDescription: function(event, ChangeSecurityDataDialog) {
                ChangeSecurityDataDialog.show(event);
            }
        },
        {
            startEvent: Constants.EVENT_RETURN_TO_AM_CONSOLE,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Configuration"
            ],
            processDescription: function (event, conf) {
                var subRealm = conf.globalData.auth.subRealm || "/";
                window.location.href = "/" + Constants.context + "/realm/RMRealm?RMRealm.tblDataActionHref=" + encodeURIComponent(subRealm);
            }
        },
        {
            startEvent: Constants.EVENT_AUTHENTICATED,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/components/Navigation"
            ],
            processDescription: function (event, Configuration, Navigation) {

                /*if(_.contains(Configuration.loggedUser.roles, 'ui-admin')){

                    // TODO: This is only mock data. This would be replaced with a
                    // Delegate and call to appropriate endpoint. Possibly even
                    // carrying out this functionality in a specific module elsewhere.
                    var realms = [{
                        "url": "#realms/authentication/advanced/",
                        "name": "Top Level Realm"
                    },
                    {
                        "url": "#realms/authentication/chains/",
                        "name": "My Realm"
                    },
                    {
                        "url": "#realms/authentication/",
                        "name": "Another Realm"
                    }];

                    Navigation.configuration.links.admin.urls.realms.urls.push({
                        divider: true
                    });

                    _.each(realms, function(obj){
                        Navigation.configuration.links.admin.urls.realms.urls.push(obj);
                    });

                    Navigation.reload();
                }*/
            }
        }


    ];
    return obj;
});
