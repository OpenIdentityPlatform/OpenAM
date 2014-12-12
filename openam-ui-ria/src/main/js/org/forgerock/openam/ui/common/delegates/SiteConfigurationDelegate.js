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

/*global $, define, _, location */

/**
 * @author mbilski
 */
define("org/forgerock/openam/ui/common/delegates/SiteConfigurationDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function(constants, AbstractDelegate, configuration, eventManager, uiUtils, realmHelper) {

    var obj = new AbstractDelegate(constants.host + "/"+ constants.context ),
        lastKnownRealm = "/";

    obj.getConfiguration = function(successCallback, errorCallback) {
        
        console.info("Getting configuration");
        obj.serviceCall({
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.1"},
            url: "/json/serverinfo/*",
            success: function(response) {
                var hostname = location.hostname,
                    fqdn = response.FQDN,
                    currentUrl = uiUtils.getUrl();
                if (hostname !== fqdn) {
                    location.href = currentUrl.replace(hostname,fqdn);
                } else {
                    successCallback(response);
                }
            },
            error: errorCallback
        });

    };


    obj.checkForDifferences = function(route,params) {

        var index = _.indexOf(route.argumentNames, "realm"),
            urlParams = uiUtils.convertCurrentUrlToJSON().params;

        if (urlParams && typeof urlParams.realm === "string" && urlParams.realm !== "") {
            configuration.globalData.auth.realm = urlParams.realm;
            if (configuration.globalData.auth.realm[0] !== "/") {
                configuration.globalData.auth.realm = "/" + configuration.globalData.auth.realm; 
            }
        } else if (index !== -1 && params[index] !== undefined && params[index] !== null ) {
            configuration.globalData.auth.realm = params[index];
        }

        if (lastKnownRealm !== configuration.globalData.auth.realm) {
            lastKnownRealm = realmHelper.cleanRealm(configuration.globalData.auth.realm);

            return obj.serviceCall({
                type: "GET",
                headers: {"Accept-API-Version": "protocol=1.0,resource=1.1"},
                url: "/json" + lastKnownRealm + "/serverinfo/*",
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
