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

define("org/forgerock/openam/ui/common/delegates/SiteConfigurationDelegate", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "org/forgerock/openam/ui/common/delegates/ServerDelegate",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function ($, AbstractDelegate, Configuration, Constants, URIUtils, ServerDelegate, RealmHelper) {
    var obj = new AbstractDelegate(Constants.host + "/" + Constants.context),
        lastKnownSubRealm,
        lastKnownOverrideRealm,
        setRequireMapConfig = function (serverInfo) {
            require.config({ "map": { "*": {
                "UserProfileView" : (serverInfo.kbaEnabled === "true"
                    ? "org/forgerock/commons/ui/user/profile/UserProfileKBAView"
                    : "org/forgerock/commons/ui/user/profile/UserProfileView")
            } } });

            return serverInfo;
        };

    /**
     * Makes a HTTP request to the server to get its configuration
     * @param {Function} successCallback Success callback function
     * @param {Function} errorCallback   Error callback function
     */
    obj.getConfiguration = function (successCallback, errorCallback) {
        if (!Configuration.globalData.auth.subRealm) {
            try {
                console.log("No current SUB REALM was detected. Applying from current URI values...");
                var subRealm = RealmHelper.getSubRealm();
                console.log("Changing SUB REALM to '" + subRealm + "'");

                Configuration.globalData.auth.subRealm = RealmHelper.getSubRealm();

                lastKnownSubRealm = RealmHelper.getSubRealm();
                lastKnownOverrideRealm = RealmHelper.getOverrideRealm();
            } catch (error) {
                console.log("Unable to applying sub realm from URI values");
            }
        }

        ServerDelegate.getConfiguration({ suppressEvents: true }).then(function (response) {
            setRequireMapConfig(response);
            successCallback(response);
        }, errorCallback);
    };

    /**
     * Checks for a change of realm
     * @returns {Promise} If the realm has changed then a promise that will contain the response from the
     * serverinfo/* REST call, otherwise an empty successful promise.
     */
    obj.checkForDifferences = function () {
        var currentSubRealm = RealmHelper.getSubRealm(),
            currentOverrideRealm = RealmHelper.getOverrideRealm(),
            subRealmChanged = lastKnownSubRealm !== currentSubRealm,
            overrideRealmChanged = lastKnownOverrideRealm !== currentOverrideRealm;
        if (subRealmChanged || overrideRealmChanged) {
            if (currentSubRealm !== lastKnownSubRealm) {
                console.log("Changing SUB REALM from '" + lastKnownSubRealm + "' to '" + currentSubRealm + "'");
                Configuration.globalData.auth.subRealm = currentSubRealm;
                lastKnownSubRealm = currentSubRealm;
            }

            lastKnownOverrideRealm = RealmHelper.getOverrideRealm();

            return ServerDelegate.getConfiguration({
                errorsHandlers: {
                    "unauthorized": { status: "401" },
                    "Bad Request": {
                        status: "400",
                        event: Constants.EVENT_INVALID_REALM
                    }
                }
            }).then(setRequireMapConfig);
        } else {
            return $.Deferred().resolve();
        }
    };

    return obj;
});
