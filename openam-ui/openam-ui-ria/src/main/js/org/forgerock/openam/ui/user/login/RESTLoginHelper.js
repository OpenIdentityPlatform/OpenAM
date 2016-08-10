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

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/ServiceInvoker",
    "org/forgerock/commons/ui/common/main/ViewManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/user/login/tokens/SessionToken",
    "org/forgerock/openam/ui/user/services/AuthNService",
    "org/forgerock/openam/ui/user/services/SessionService",
    "org/forgerock/openam/ui/user/UserModel"
], ($, _, AbstractConfigurationAware, Configuration, ServiceInvoker, ViewManager, Constants, URIUtils,
    fetchUrl, SessionToken, AuthNService, SessionService, UserModel) => {
    var obj = new AbstractConfigurationAware();

    obj.login = function (params, successCallback, errorCallback) {
        var self = this;
        AuthNService.getRequirements(params).then(function (requirements) {
            // populate the current set of requirements with the values we have from params
            var populatedRequirements = _.clone(requirements);
            _.each(requirements.callbacks, function (obj, i) {
                if (params.hasOwnProperty(`callback_${i}`)) {
                    populatedRequirements.callbacks[i].input[0].value = params[`callback_${i}`];
                }
            });

            AuthNService.submitRequirements(populatedRequirements, params).then(function (result) {
                if (result.hasOwnProperty("tokenId")) {
                    obj.getLoggedUser(function (user) {
                        Configuration.setProperty("loggedUser", user);
                        self.setSuccessURL(result.tokenId, result.successUrl).then(function () {
                            successCallback(user);
                            AuthNService.resetProcess();
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
                            href += `/${realm}`;
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
        const sessionToken = SessionToken.get();
        const noSessionHandler = (xhr) => {
            // Try to remove any cookie that is lingering, as it is apparently no longer valid
            SessionToken.remove();

            if (xhr && xhr.responseJSON && xhr.responseJSON.code === 404) {
                errorCallback("loggedIn");
            } else {
                errorCallback();
            }
        };
        // TODO AME-11593 Call to idFromSession is required to populate the fullLoginURL, which we use later to
        // determine the parameters you logged in with. We should remove the support of fragment parameters and use
        // persistent url query parameters instead.
        ServiceInvoker.restCall({
            url: `${Constants.host}/${Constants.context}/json${
                fetchUrl.default("/users?_action=idFromSession")}`,
            headers: { "Accept-API-Version": "protocol=1.0,resource=2.0" },
            type: "POST",
            errorsHandlers: { "serverError": { status: "503" }, "unauthorized": { status: "401" } }
        }).then((data) => {
            Configuration.globalData.auth.fullLoginURL = data.fullLoginURL;
        });

        if (sessionToken) {
            return SessionService.getSessionInfo(sessionToken).then((data) => {
                return UserModel.fetchById(data.uid).then(successCallback);
            }, noSessionHandler);
        } else {
            noSessionHandler();
        }
    };

    obj.getSuccessfulLoginUrlParams = function () {
        // The successfulLoginURL is populated by the server (not from window.location of the browser), upon successful
        // authentication.
        var successfulLoginURL = Configuration.globalData.auth.fullLoginURL,
            successfulLoginURLParams = successfulLoginURL
                ? successfulLoginURL.substring(successfulLoginURL.indexOf("?") + 1) : "";

        return URIUtils.parseQueryString(successfulLoginURLParams);
    };

    obj.setSuccessURL = function (tokenId, successUrl) {
        const promise = $.Deferred();
        const urlParams = URIUtils.parseQueryString(URIUtils.getCurrentCompositeQueryString());
        let context = "";

        if (urlParams && urlParams.goto) {
            AuthNService.setGoToUrl(tokenId, urlParams.goto).then((data) => {
                if (data.successURL.indexOf("/") === 0 &&
                    data.successURL.indexOf(`/${Constants.context}`) !== 0) {
                    context = `/${Constants.context}`;
                }
                Configuration.globalData.auth.urlParams.goto = context + data.successURL;
                promise.resolve();
            }, () => {
                promise.reject();
            });
        } else {
            if (successUrl !== Constants.CONSOLE_PATH) {
                if (!Configuration.globalData.auth.urlParams) {
                    Configuration.globalData.auth.urlParams = {};
                }
                if (!Configuration.globalData.auth.urlParams.goto) {
                    Configuration.globalData.auth.urlParams.goto = successUrl;
                }
            }
            promise.resolve();
        }
        return promise;
    };

    obj.filterUrlParams = function (params) {
        const filtered = ["arg", "authIndexType", "authIndexValue", "goto", "gotoOnFail", "ForceAuth", "locale"];
        return _.reduce(_.pick(params, filtered), (result, value, key) => `${result}&${key}=${value}`, "");
    };

    obj.logout = function (successCallback = function () {}, errorCallback = function () {}) {
        const sessionToken = SessionToken.get();
        SessionService.isSessionValid(sessionToken).then((result) => {
            if (result.valid) {
                SessionService.logout(sessionToken).then((response) => {
                    SessionToken.remove();
                    successCallback(response);
                    return true;
                }, () => {
                    SessionToken.remove();
                });
            } else {
                SessionToken.remove();
                successCallback();
            }
        }, () => {
            errorCallback();
        });
    };

    return obj;
});
