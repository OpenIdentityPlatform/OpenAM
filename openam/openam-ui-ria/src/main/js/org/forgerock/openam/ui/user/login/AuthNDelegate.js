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

/**
 * "Portions Copyrighted 2011-2013 ForgeRock Inc"
 */

/*global document, $, define, _ */

define("org/forgerock/openam/ui/user/login/AuthNDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/CookieHelper"
], function(constants, AbstractDelegate, configuration, eventManager, cookieHelper) {

    var obj = new AbstractDelegate(constants.host + "/"+ constants.context + "/rest/auth/1/authenticate"),
        requirementList = [],
        cookieName = "";
    
    
    obj.begin = function (successCallback) {
        if (cookieName === "") {
            obj.serviceCall({
                    serviceUrl: constants.host + "/"+ constants.context + "/identity/json/getcookienamefortoken",
                    url: ""
                })
                .done(function (foundCookieName) {
                    cookieName = foundCookieName.string;
                });
        }
        
        return obj.serviceCall({
                    type: "GET",
                    url: "",
                    success: successCallback
                });
        
    };
    
    obj.submitRequirements = function (requirements) {
        return obj.serviceCall({
                    type: "POST",
                    data: JSON.stringify(requirements),
                    url: "/submitReqs"
                })
                .done(function (result) {
                    if (result.hasOwnProperty("authId")) {
                        requirementList.push(result);
                    } else if (result.hasOwnProperty("tokenId")) {
                        cookieHelper.setCookie(cookieName, result.tokenId, "", "/", document.domain.split(".").splice(1).join("."));
                    }
                })
                .fail(function () {
                    obj.resetProcess();
                });
    };
    
    obj.resetProcess = function () {
        requirementList = [];
    };
    
    obj.getRequirements = function () {
        var ret = $.Deferred();
        if (requirementList.length === 0) {
            
            ret = obj.begin();
            ret.done(function (requirements) {
                requirementList.push(requirements);
            });
            
        } else {
            ret.resolve(requirementList[requirementList.length-1]);
        }
        return ret;
    };
    
    return obj;
});    
