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
 * Portions copyright 2014-2016 ForgeRock AS.
 */

define([
    "jquery",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/common/services/ServerService",
    "org/forgerock/openam/ui/common/util/isRealmChanged",
    "org/forgerock/openam/ui/user/login/tokens/SessionToken",
    "org/forgerock/openam/ui/user/services/SessionService",
    "UserProfileView"
], ($, Configuration, ServerService, isRealmChanged, SessionToken, SessionService, UserProfileView) => {
    isRealmChanged = isRealmChanged.default;

    const obj = {};
    const setRequireMapConfig = function (serverInfo) {
        if (serverInfo.kbaEnabled === "true") {
            require(["org/forgerock/commons/ui/user/profile/UserProfileKBATab"], (tab) => {
                UserProfileView.registerTab(tab);
            });
        }
        return serverInfo;
    };

    /**
     * Makes a HTTP request to the server to get its configuration
     * @param {Function} successCallback Success callback function
     * @param {Function} errorCallback   Error callback function
     */
    obj.getConfiguration = function (successCallback, errorCallback) {
        ServerService.getConfiguration({ suppressEvents: true }).then((response) => {
            setRequireMapConfig(response);
            successCallback(response);
        }, errorCallback);
    };

    /**
     * Checks if realm has changed. Redirects to switch realm page if so.
     * @returns {Promise} promise empty promise
     */
    obj.checkForDifferences = function () {
        const sessionToken = SessionToken.get();

        if (sessionToken) {
            return SessionService.updateSessionInfo(sessionToken).then(() => {
                if (isRealmChanged()) {
                    location.href = "#confirmLogin/";
                }
            });
        } else {
            return $.Deferred().resolve();
        }
    };

    return obj;
});
