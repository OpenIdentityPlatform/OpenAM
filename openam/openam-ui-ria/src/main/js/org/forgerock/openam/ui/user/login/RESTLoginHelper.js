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

/*global define, _, window, $ */

define("org/forgerock/openam/ui/user/login/RESTLoginHelper", [
    "./AuthNDelegate",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/ViewManager",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/Configuration"
], function (authNDelegate, userDelegate, viewManager, AbstractConfigurationAware, router, conf) {
    var obj = new AbstractConfigurationAware();

    obj.login = function(params, successCallback, errorCallback) {
        
        authNDelegate.getRequirements().done(function (requirements) {
        
            // populate the current set of requirements with the values we have from params
            var populatedRequirements = _.clone(requirements);
            
            _.each(requirements.callbacks, function (obj, i) {
                if (params.hasOwnProperty("callback_" + i)) {
                    populatedRequirements.callbacks[i].input[0].value = params["callback_" + i];
                }
            });
            
            authNDelegate
                .submitRequirements(populatedRequirements)
                .then(function (result) {
                        if (result.hasOwnProperty("tokenId")) {
                            obj.getLoggedUser(function(user){
                                conf.setProperty('loggedUser', user);
                                authNDelegate.setSuccessURL().then(function(){
                                    successCallback(user);
                                    authNDelegate.resetProcess();
                                });
                            }, errorCallback);
                        } else if (result.hasOwnProperty("authId")) {
                            // re-render login form for next set of required inputs
                            viewManager.refresh();
                        }
                    },
                    function (failedStage) {
                        if (failedStage > 1) {
                            // re-render login form, sending back to the start of the process.
                            viewManager.refresh();
                        }
                        errorCallback();
                    });
        
        });
    };

    obj.logout = function() {
        authNDelegate.getLoginUrlParams().done(function(p){
            authNDelegate.logout().then(function(){
                window.location.hash += obj.filterUrlParams(p);
            });
        });
    };
    
    obj.getLoggedUser = function(successCallback, errorCallback) {
        try{
            userDelegate.getProfile(function(user) {
                conf.globalData.auth.realm = user.userid.realm;
                
                // keep track of the current realm as a future default value, following logout:
                router.configuration.routes.login.defaults = [user.userid.realm];
                
                userDelegate.getUserById(user.userid.id, user.userid.realm, successCallback, errorCallback);
            }, function() {

                if (!conf.globalData.auth.realm) {
                    conf.globalData.auth.realm = router.configuration.routes.login.defaults[0];
                }

                errorCallback();
            }, {"serverError": {status: "503"}, "unauthorized": {status: "401"}});
        } catch(e) {
            console.log(e);
            errorCallback();
        }
    };
    
    obj.filterUrlParams = function(params){
        var paramsToSave = ['arg','authIndexType','authIndexValue','goto','gotoOnFail','ForceAuth','locale'],
            filteredParams = {};
        
        _.each(paramsToSave, function(p){
            if(params[p]){
                filteredParams[p] = params[p];
            }
        });
        
        return (!$.isEmptyObject(filteredParams)) ? '&' + $.param(filteredParams) : '';
    };
    
    return obj;
});
