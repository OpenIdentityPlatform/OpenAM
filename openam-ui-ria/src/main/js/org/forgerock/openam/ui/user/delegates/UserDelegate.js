/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

/**
 * @author yaromin
 */
define("UserDelegate", [
	"org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/CookieHelper"
], function(constants, AbstractDelegate, configuration, eventManager, cookieHelper) {

    var obj = new AbstractDelegate(constants.host + "/"+ constants.context + "/json");

    obj.getUserById = function(id, realm, successCallback, errorCallback) {

        obj.serviceCall({
            url: realm + "/users/" + id, 
            type: "GET", 
            headers: {"Cache-Control": "no-cache"}, // needed to prevent reads from getting cached
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
                    user_cleaned.roles = "authenticated";
                } else {
                    user_cleaned.roles += ",authenticated";
                }
                
                successCallback(user_cleaned);
            }, 
            error: errorCallback
        });
    };
    
    /**
     * Checks if logged in and returns users id
     */
    obj.getProfile = function(successCallback, errorCallback, errorsHandlers) {
        obj.serviceCall({
            url: "/users/?_action=idFromSession",
            data: "{}",
            type: "POST",
            success: function (data) {
                successCallback({userid: {id : data.id, realm: data.realm}, username: data.dn});
            },
            error: errorCallback,
            errorsHandlers: errorsHandlers
        });
    };
    
    obj.updateUser = function(user, realm, objectParam, successCallback, errorCallback) {
        var headers = {};
        
        if(objectParam._rev) {
            headers["If-Match"] = '"' + objectParam._rev + '"';
        } else {
            headers["If-Match"] = '"' + "*" + '"';
        }
        
        if(objectParam.oldPassword) {
            headers[constants.OPENAM_HEADER_PARAM_OLD_PASSWORD] = objectParam.oldPassword;
        }

        this.serviceCall({url: realm + "/users/" +user,
            type: "PUT",
            success: successCallback, 
            error: errorCallback, 
            data: JSON.stringify(_.pick(objectParam, ["givenname","sn","mail","postaladdress","telephonenumber","userpassword"])),
            headers: headers
        });

    };


    obj.forgotPassword = function(postData, successCallback, errorCallback, errorsHandlers) {
        var realm = configuration.globalData.auth.realm;
        if(realm === "/"){
            realm = "";
        }
        return obj.serviceCall({
            url: realm + "/users/?_action=forgotPassword",
            data: JSON.stringify(postData),
            type: "POST",
            success: function (data) {
                successCallback(data);
            },
            error: errorCallback,
            errorsHandlers: errorsHandlers
        });
    };

    obj.forgotPasswordReset = function(postData, successCallback) {
        var realm = configuration.globalData.auth.realm;
        if(realm === "/"){
            realm = "";
        }
        return obj.serviceCall({
            url: realm + "/users/?_action=forgotPasswordReset",
            data: JSON.stringify(postData),
            type: "POST",
            success: function (data) {
                successCallback(data);
            }
        });
    };
    
    return obj;
});



