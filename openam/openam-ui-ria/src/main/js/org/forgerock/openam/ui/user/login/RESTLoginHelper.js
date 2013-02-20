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

/*global define, _ */

define("org/forgerock/openam/ui/user/login/RESTLoginHelper", [
    "./AuthNDelegate",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/ViewManager",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/commons/ui/common/main/Configuration"
], function (authNDelegate, userDelegate, viewManager, AbstractConfigurationAware, conf) {
    var obj = new AbstractConfigurationAware();

    obj.login = function(params, successCallback, errorCallback) {
        
        authNDelegate.getRequirements().done(function (requirements) {
        
            // populate the current set of requirements with the values we have from params
            var populatedRequirements = _.clone(requirements);
            
            _.each(requirements.callbacks, function (obj, i) {
                if (params.hasOwnProperty(obj.input[0].name)) {
                    populatedRequirements.callbacks[i].input[0].value = params[obj.input[0].name];
                }
            });
            
            authNDelegate
                .submitRequirements(populatedRequirements)
                .done(function (result) {
                        if (result.hasOwnProperty("tokenId")) {
                            obj.getLoggedUser(successCallback, errorCallback);
                        } else if (result.hasOwnProperty("authId")) {
                            // re-render login form for next set of required inputs
                            viewManager.refresh();
                        }
                    })
                .fail(errorCallback);
        
        });
    };

    obj.logout = function() {
        userDelegate.logout();
    };
    
    obj.getLoggedUser = function(successCallback, errorCallback) {
        try{
            userDelegate.getProfile(function(user) {
                conf.globalData.userComponent = user.userid.component;
                
                userDelegate.getUserById(user.userid.id, user.userid.component, successCallback, errorCallback);
            }, function() {
                errorCallback();
            }, {"serverError": {status: "503"}, "unauthorized": {status: "401"}});
        } catch(e) {
            console.log(e);
            errorCallback();
        }
    };
    return obj;
});
