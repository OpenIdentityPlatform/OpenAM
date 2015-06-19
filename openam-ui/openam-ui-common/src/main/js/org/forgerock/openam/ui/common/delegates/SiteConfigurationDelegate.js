/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
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

/*global $, define, _, location */

define("org/forgerock/openam/ui/common/delegates/SiteConfigurationDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function(constants, AbstractDelegate, configuration, eventManager, uiUtils, RealmHelper) {
    var obj = new AbstractDelegate(constants.host + "/" + constants.context ),
        lastKnownSubRealm,
        lastKnownOverrideRealm;

    /**
     * Makes a HTTP request to the server to get its configuration
     * @param {Function} successCallback Success callback function
     * @param {Function} errorCallback   Error callback function
     */
    obj.getConfiguration = function(successCallback, errorCallback) {
        if(!configuration.globalData.auth.subRealm) {
            try {
                console.debug("No current SUB REALM was detected. Applying from current URI values...");
                var subRealm = RealmHelper.getSubRealm();
                console.debug("Changing SUB REALM to '" + subRealm + "'");

                configuration.globalData.auth.subRealm = RealmHelper.getSubRealm();

                lastKnownSubRealm = RealmHelper.getSubRealm();
                lastKnownOverrideRealm = RealmHelper.getOverrideRealm();
            } catch(error) {
                console.debug("Unable to applying sub realm from URI values");
            }
        }

        obj.serviceCall({
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.1" },
            url: RealmHelper.decorateURIWithRealm("/json/__subrealm__/serverinfo/*"),
            success: function(response) {
                var hostname = location.hostname,
                    fqdn = response.FQDN;

                if (fqdn !== null && hostname !== fqdn) {
                    // Redirect browser back to the server using the FQDN to ensure cookies are set correctly
                    location.href = uiUtils.getUrl().replace(hostname, fqdn);
                } else {
                    successCallback(response);
                }
            },
            error: errorCallback
        });
    };

    /**
     * Checks for a change of realm
     */
    obj.checkForDifferences = function(route, params) {
        if(lastKnownSubRealm !== RealmHelper.getSubRealm() || lastKnownOverrideRealm !== RealmHelper.getOverrideRealm()) {
            var currentSubRealm = RealmHelper.getSubRealm(),
                currentOverrideRealm = RealmHelper.getOverrideRealm();

            if(currentSubRealm !== lastKnownSubRealm) {
                console.debug("Changing SUB REALM from '" + lastKnownSubRealm + "' to '" + currentSubRealm + "'");
                configuration.globalData.auth.subRealm = currentSubRealm;
                lastKnownSubRealm = currentSubRealm;
            }

            lastKnownOverrideRealm = RealmHelper.getOverrideRealm();

            return obj.serviceCall({
                type: "GET",
                headers: {"Accept-API-Version": "protocol=1.0,resource=1.1"},
                url: RealmHelper.decorateURIWithRealm("/json/__subrealm__/serverinfo/*"),
                errorsHandlers: {
                    "unauthorized": { status: "401"},
                    "Bad Request": {
                        status: "400",
                        event: constants.EVENT_INVALID_REALM
                    }
                }
            });
        } else {
            return $.Deferred().resolve();
        }
    };

    return obj;
});
