/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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

/*global $, define, _, console */

define("UserDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/util/Mime",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function(constants, AbstractDelegate, configuration, eventManager, cookieHelper, mime, realmHelper) {

    var obj = new AbstractDelegate(constants.host + "/"+ constants.context + "/json");

    obj.getUserResourceName = function (user) {
        return user.resourceName;
    };

    obj.getUserById = function(id, realm, successCallback, errorCallback, errorsHandlers) {

        var resourceName = realmHelper.cleanRealm(realm) + "/users/" + id;

        obj.serviceCall({
            url: resourceName,
            type: "GET",
            // needed to prevent reads from getting cached
            headers: {"Cache-Control": "no-cache", "Accept-API-Version": "protocol=1.0,resource=2.0"},
            success: function(user) {
                var user_cleaned = {},i=0;
                for (i in user) {
                    if (_.isArray(user[i])) {
                        user_cleaned[i] = user[i].join(",");
                    } else {
                        user_cleaned[i] = user[i];
                    }
                }
                if (!user_cleaned.roles) {
                    user_cleaned.roles = "ui-user";
                } else {
                    user_cleaned.roles += ",ui-user";
                }

                user_cleaned.resourceName = resourceName;

                successCallback(user_cleaned);
            },
            error: errorCallback,
            errorsHandlers: errorsHandlers
        });
    };

    /**
     * Checks if logged in and returns users id
     */
    obj.getProfile = function(successCallback, errorCallback, errorsHandlers) {
        var realm = realmHelper.cleanRealm(configuration.globalData.auth.realm);
        obj.serviceCall({
            url: realm + "/users?_action=idFromSession",
            data: "{}",
            type: "POST",
            headers: {"Accept-API-Version": "protocol=1.0,resource=2.0"},
            success: function (data) {
                configuration.globalData.auth.successURL = data.successURL;
                configuration.globalData.auth.fullLoginURL = data.fullLoginURL;
                successCallback({userid: {id : data.id, realm: data.realm}, username: data.dn});
            },
            error: errorCallback,
            errorsHandlers: errorsHandlers
        });
    };

    obj.updateUser = function(oldUserData, objectParam, successCallback, errorCallback) {

        var headers = {"Accept-API-Version": "protocol=1.0,resource=2.0"},
            picked = _.pick(objectParam, ["givenName","sn","mail","postalAddress","telephoneNumber"]);

        if(objectParam._rev) {
            headers["If-Match"] = '"' + objectParam._rev + '"';
        } else {
            headers["If-Match"] = '"' + "*" + '"';
        }

        if(objectParam.currentpassword ) {
            headers[constants.OPENAM_HEADER_PARAM_CUR_PASSWORD] = mime.encodeHeader(objectParam.currentpassword);
        }

        this.serviceCall({url: this.getUserResourceName(oldUserData),
            type: "PUT",
            success: successCallback,
            error: errorCallback,
            data: JSON.stringify(picked, function(key, value) { return value === "" ? [] : value; }),
            headers: headers,
            errorsHandlers : { "error": { status: "400" } }
        });

    };

    obj.changePassword = function(oldUserData, postData, successCallback, errorCallback, errorsHandlers) {
        return this.serviceCall({url: this.getUserResourceName(oldUserData) + "?_action=changePassword",
            data: JSON.stringify(postData),
            headers: {"Accept-API-Version": "protocol=1.0,resource=2.0"},
            type: "POST",
            success: successCallback,
            error: errorCallback,
            errorsHandlers: errorsHandlers
        });
    };

    obj.doAction = function(action, postData, successCallback, errorCallback, errorsHandlers) {
        var realm = realmHelper.cleanRealm(configuration.globalData.auth.realm);

        return obj.serviceCall({
            url: realm + "/users?_action=" + action,
            headers: {"Accept-API-Version": "protocol=1.0,resource=2.0"},
            data: JSON.stringify(postData),
            type: "POST",
            success: function (data) {
                successCallback(data);
            },
            error: errorCallback,
            errorsHandlers: errorsHandlers
        });
    };

    return obj;
});
