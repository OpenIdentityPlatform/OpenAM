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

    var obj = new AbstractDelegate(constants.host + "/"+ constants.context + "/json/users");

    /**
     * Starting session. Sending username and password to authenticate and returns user's id.
     */
    obj.login = function(uid, password, successCallback, errorCallback, errorsHandlers) {

        obj.serviceCall({
            serviceUrl: constants.host + "/"+ constants.context + "/identity/authenticate?" + $.param({username: uid, password: password}),
            url: "",
            dataType: "text",
            type: "POST",
            success: _.bind(function (data) {
                var token = "";
                if(!data.length || !data.match(/^token\.id=/)) {
                    if(errorCallback) {
                        errorCallback();
                    }
                } else {
                    token = data.split("=")[1].replace("\n", "");
                    cookieHelper.setCookie("iPlanetDirectoryPro", token, "", "/", constants.host); // the name of this cookie should probably be in the config as well 
                    this.getProfile(successCallback, errorCallback, errorsHandlers);    
                }
            }, this),
            error: function () { 
                errorCallback.apply(this, arguments); 
            },
            errorsHandlers: errorsHandlers
        });
    };
    
    obj.getUserById = function(id, component, successCallback, errorCallback) {

        obj.serviceCall({
            url: "/" + id, 
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
            serviceUrl: constants.host + "/"+ constants.context + "/json/users/?_action=idFromSession",
            url: "",
            data: "{}",
            type: "POST",
            success: function (data) {
                successCallback({userid: {id : data.id, component: data.realm}, username: data.dn});
            },
            error: errorCallback,
            errorsHandlers: errorsHandlers
        });
    };
    
    obj.getForUserID = function(uid, successCallback, errorCallback) {
        obj.serviceCall({
            url: "/" + uid, 
            success: successCallback,
            error: errorCallback
        });
    };

    obj.logout = function() {

        obj.serviceCall({
            serviceUrl: constants.host + "/"+ constants.context + "/identity/logout?" + $.param({subjectid: cookieHelper.getCookie('iPlanetDirectoryPro')}),
            url: "",
            dataType: "text",
            type: "GET",
            success: function () { 
                console.debug("Successfully logged out");
                cookieHelper.deleteCookie("iPlanetDirectoryPro", "", _.last(document.domain.split('.'), 2).join('.'));
            },
            error: function () {
                console.debug("Error during logging out");
            }
        });
    };

    return obj;
});



