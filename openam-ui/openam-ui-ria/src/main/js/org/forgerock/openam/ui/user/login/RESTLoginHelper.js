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
 * Portions copyright 2011-2016 ForgeRock AS.
 */

define("org/forgerock/openam/ui/user/login/RESTLoginHelper", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/openam/ui/user/delegates/AuthNDelegate",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/user/delegates/SessionDelegate",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "org/forgerock/openam/ui/user/UserModel",
    "org/forgerock/commons/ui/common/main/ViewManager"
], function ($, _, AbstractConfigurationAware, AuthNDelegate, CookieHelper, Configuration, Constants, SessionDelegate,
             URIUtils, UserModel, ViewManager) {
    var obj = new AbstractConfigurationAware();

    obj.login = function (params, successCallback, errorCallback) {
        var self = this;
        AuthNDelegate.getRequirements().then(function (requirements) {
            // populate the current set of requirements with the values we have from params
            var populatedRequirements = _.clone(requirements);

            // used in auto login from self registration
            if (params.userName && params.password && requirements.stage === "DataStore1") {
                populatedRequirements.callbacks[0].input[0].value = params.userName;
                populatedRequirements.callbacks[1].input[0].value = params.password;
            } else {
                _.each(requirements.callbacks, function (obj, i) {
                    if (params.hasOwnProperty("callback_" + i)) {
                        populatedRequirements.callbacks[i].input[0].value = params["callback_" + i];
                    }
                });
            }

            AuthNDelegate.submitRequirements(populatedRequirements).then(function (result) {
                if (result.hasOwnProperty("tokenId")) {
                    obj.getLoggedUser(function (user) {
                        Configuration.setProperty("loggedUser", user);
                        self.setSuccessURL(result.tokenId).then(function () {
                            successCallback(user);
                            AuthNDelegate.resetProcess();
                        });
                    }, errorCallback);
                } else if (result.hasOwnProperty("authId")) {
                    // re-render login form for next set of required inputs
                    if (ViewManager.currentView === "LoginView") {
                        ViewManager.refresh();
                    } else {
                        // TODO: If using a module chain with autologin the user is
                        // currently routed to the first login screen.
                        var href = "#login",
                            realm = Configuration.globalData.auth.subRealm;
                        if (realm) {
                            href += "/" + realm;
                        }
                        location.href = href;
                    }
                }
            }, function (failedStage, errorMsg) {
                if (failedStage > 1) {
                    // re-render login form, sending back to the start of the process.
                    ViewManager.refresh();
                }
                errorCallback(errorMsg);
            });
        });
    };

    obj.getLoggedUser = function (successCallback, errorCallback) {
        return UserModel.getProfile().then(successCallback, function (xhr) {
            // Try to remove any cookie that is lingering, as it is apparently no longer valid
            obj.removeSessionCookie();

            if (xhr && xhr.responseJSON && xhr.responseJSON.code === 404) {
                errorCallback("loggedIn");
            } else {
                errorCallback();
            }
        });
    };

    obj.getSuccessfulLoginUrlParams = function () {
        // The successfulLoginURL is populated by the server (not from window.location of the browser), upon successful
        // authentication.
        var successfulLoginURL = Configuration.globalData.auth.fullLoginURL,
            successfulLoginURLParams = successfulLoginURL
                ? successfulLoginURL.substring(successfulLoginURL.indexOf("?") + 1) : "";

        return URIUtils.parseQueryString(successfulLoginURLParams);
    };

    obj.setSuccessURL = function (tokenId) {
        var promise = $.Deferred(),
            urlParams = URIUtils.parseQueryString(URIUtils.getCurrentCompositeQueryString()),
            url = Configuration.globalData.auth.successURL,
            context = "";
        if (urlParams && urlParams.goto) {
            AuthNDelegate.setGoToUrl(tokenId, urlParams.goto).then(function (data) {
                if (data.successURL.indexOf("/") === 0 &&
                    data.successURL.indexOf("/" + Constants.context) !== 0) {
                    context = "/" + Constants.context;
                }
                Configuration.globalData.auth.urlParams.goto = context + data.successURL;
                promise.resolve();
            }, function () {
                promise.reject();
            });
        } else {
            if (url !== Constants.CONSOLE_PATH) {
                if (!Configuration.globalData.auth.urlParams) {
                    Configuration.globalData.auth.urlParams = {};
                }
                if (!Configuration.globalData.auth.urlParams.goto) {
                    Configuration.globalData.auth.urlParams.goto = url;
                }
            }
            promise.resolve();
        }
        return promise;
    };

    obj.filterUrlParams = function (params) {
        var paramsToSave = ["arg", "authIndexType", "authIndexValue", "goto", "gotoOnFail", "ForceAuth", "locale"],
            filteredParams = _.pick(params, paramsToSave);

        return _.isEmpty(filteredParams) ? "" : "&" + $.param(filteredParams);
    };

    obj.logout = function (successCallback, errorCallback) {
        var tokenCookie = CookieHelper.getCookie(Configuration.globalData.auth.cookieName);
        SessionDelegate.isSessionValid(tokenCookie).then(function (result) {
            if (result.valid) {
                SessionDelegate.logout(tokenCookie).then(function (response) {
                    obj.removeSessionCookie();

                    successCallback(response);
                    return true;

                }, obj.removeSessionCookie);
            } else {
                obj.removeSessionCookie();
                successCallback();
            }
        }, function () {
            if (errorCallback) {
                errorCallback();
            }
        });
    };

    obj.removeSession = function () {
        var tokenCookie = CookieHelper.getCookie(Configuration.globalData.auth.cookieName);
        SessionDelegate.isSessionValid(tokenCookie).then(function (result) {
            if (result.valid) {
                SessionDelegate.logout(tokenCookie).then(function () {
                    obj.removeSessionCookie();
                });
            }
        });
    };

    obj.removeSessionCookie = function () {
        var auth = Configuration.globalData.auth;
        if (auth.cookieDomains && auth.cookieDomains.length !== 0) {
            _.each(auth.cookieDomains, function (cookieDomain) {
                CookieHelper.deleteCookie(auth.cookieName, "/", cookieDomain);
            });
        } else {
            CookieHelper.deleteCookie(auth.cookieName, "/", location.hostname);
        }
    };

    obj.removeAuthCookie = function () {
        var auth = Configuration.globalData.auth;
        CookieHelper.deleteCookie("authId", "/", auth.cookieDomains ? auth.cookieDomains : location.hostname);
    };

    return obj;
});
