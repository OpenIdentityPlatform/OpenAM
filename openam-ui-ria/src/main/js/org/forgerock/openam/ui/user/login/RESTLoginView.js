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

/*global define, $, form2js, _, js2form, Handlebars, window */

define("org/forgerock/openam/ui/user/login/RESTLoginView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/user/login/AuthNDelegate",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/SessionManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "org/forgerock/openam/ui/user/login/RESTLoginHelper"
], function(AbstractView, authNDelegate, validatorsManager, eventManager, constants, conf, sessionManager, router, cookieHelper, uiUtils,i18nManager,restLoginHelper) {
    
    var LoginView = AbstractView.extend({
        template: "templates/openam/RESTLoginTemplate.html",
        genericTemplate: "templates/openam/RESTLoginTemplate.html",
        unavailableTemplate: "templates/openam/RESTLoginUnavailableTemplate.html",
        baseTemplate: "templates/user/LoginBaseTemplate.html",
        
        data: {},
        events: {
            "click input[type=submit]": "formSubmit",
            "click .SelfService": "selfServiceClick"
        },
        selfServiceClick: function(e) {
            e.preventDefault();
            //save the login params in a cookie for use with the cancel button on forgotPassword/register page 
            //and also the "proceed to login" link once password has been successfully changed or registration is complete
            var expire = new Date(),
                cookieVal = conf.globalData.auth.realm;
            if(conf.globalData.auth.urlParams){
                cookieVal += restLoginHelper.filterUrlParams(conf.globalData.auth.urlParams);
            }
            expire.setDate(expire.getDate() + 1);
            cookieHelper.setCookie("loginUrlParams",cookieVal,expire);
            location.href = e.target.href;
        },
        autoLogin: function() {
              var index,
                  submitContent = {};
              _.each(_.keys(conf.globalData.auth.urlParams),function(key){
                  if(key.indexOf('IDToken') > -1){
                      index = parseInt(key.substring(7),10) - 1;
                      submitContent['callback_' + index] = conf.globalData.auth.urlParams['IDToken' + key.substring(7)]; 
                  }
              });
              conf.globalData.auth.autoLoginAttempts = 1;
              eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, submitContent);
        },
        formSubmit: function (e) {
            var submitContent,expire;
            
            e.preventDefault();
            submitContent = form2js(this.$el[0]);
            submitContent[$(e.target).attr('name')] = $(e.target).attr('index');
            
            // START CUSTOM STAGE-SPECIFIC LOGIC HERE
            
            // known to be used by DataStore1.html
            if (this.$el.find("[name=loginRemember]:checked").length !== 0) {
                expire = new Date();
                expire.setDate(expire.getDate + 365*20);
                // cheesy assumption that the login name is the first text input box
                cookieHelper.setCookie("login", this.$el.find("input[type=text]:first").val(), expire);
            } else if (this.$el.find("[name=loginRemember]").length !== 0) {
                cookieHelper.deleteCookie("login");
            }
            
            // END CUSTOM STAGE-SPECIFIC LOGIC HERE
            
            eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, submitContent);
        },
        render: function(args, callback) {
            var urlParams = {};//deserialized querystring params
            
            if (args && args.length) {
                conf.globalData.auth.realm = args[0];
                conf.globalData.auth.additional = args[1]; // may be "undefined"
                conf.globalData.auth.urlParams = urlParams;
                
                if(args[1]){
                    urlParams = this.handleUrlParams();
                }
                
                if(urlParams.realm && args[0] === "/"){
                    if(urlParams.realm.substring(0,1) !== "/"){
                        urlParams.realm = "/" + urlParams.realm;
                    }
                    conf.globalData.auth.realm = urlParams.realm;
                }
                
                //if there are IDTokens try to login with the provided credentials
                if(urlParams.IDToken1 && !conf.globalData.auth.autoLoginAttempts){
                    this.autoLogin();
                }
                
                if(urlParams.locale){
                    i18nManager.setLanguage(urlParams.locale);
                }
            }
            
            authNDelegate.getRequirements()
            .done(_.bind(function (reqs) {
                var cleaned = _.clone(reqs),
                    implicitConfirmation = true;
                
                //clear out existing session if instructed 
                if (reqs.hasOwnProperty("tokenId") && urlParams.arg === 'newsession') {
                    sessionManager.logout();
                    conf.setProperty('loggedUser', null);
                }
                
                // if simply by asking for the requirements, we end up with a token, then we must have auto-logged-in somehow
                if (reqs.hasOwnProperty("tokenId") && urlParams.ForceAuth !== 'true') {
                        //set a variable for the realm passed into the browser so there can be a check to make sure it is the same as the current user's realm
                        conf.globalData.auth.passedInRealm = conf.globalData.auth.realm;
                        // if we have a token, let's see who we are logged in as....
                        sessionManager.getLoggedUser(function(user) {
                            if(conf.globalData.auth.passedInRealm === conf.globalData.auth.realm){
                                conf.setProperty('loggedUser', user);
                                delete conf.globalData.auth.passedInRealm;
                                restLoginHelper.setSuccessURL();
                                //eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loggedIn");
                                if(conf.globalData.auth.urlParams && conf.globalData.auth.urlParams.goto){
                                    window.location.href = conf.globalData.auth.urlParams.goto;
                                    $('body').empty();
                                    return false;
                                }
                                eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false});
                                
                                // copied from EVENT_LOGIN_REQUEST handler
                                if(conf.gotoURL && _.indexOf(["#","","#/","/#"], conf.gotoURL) === -1) {
                                    console.log("Auto redirect to " + conf.gotoURL);
                                    router.navigate(conf.gotoURL, {trigger: true});
                                    delete conf.gotoURL;
                                } else {
                                    router.navigate("", {trigger: true});
                                }
                            }
                            else{
                                location.href = "#confirmLogin/";
                            }
                        },function(){
                            //there is a tokenId but it is invalid so kill it
                            sessionManager.logout();
                            conf.setProperty('loggedUser', null);
                        });
                    
                } else { // we aren't logged in yet, so render a form...
                    
                    cleaned.callbacks = [];
                    _.each(reqs.callbacks, function(element) {
                        
                        if (element.type === "ConfirmationCallback") {
                            implicitConfirmation = false;
                        }
                        
                        cleaned.callbacks.push({
                            input: {
                                index: cleaned.callbacks.length,
                                name: element.input[0].name,
                                value: element.input[0].value
                            },
                            output: element.output,
                            type: element.type,
                            isSubmit: element.type === "ConfirmationCallback"
                        });
                    });
                    
                    if (implicitConfirmation) {
                        cleaned.callbacks.push({
                            "input": {
                                index: cleaned.callbacks.length,
                                name: "loginButton",
                                value: 0
                            },
                            output: [
                                {
                                    name: "options",
                                    value: [ $.t("common.user.login") ]
                                }
                            ],
                            type: "ConfirmationCallback",
                            isSubmit: true
                        });
                    }

                    this.reqs = reqs;
                    this.data.reqs = cleaned;
                    
                    //is there an attempt at autologin happening?
                    //if yes then don't render the form until it fails one time
                    if(urlParams.IDToken1 && conf.globalData.auth.autoLoginAttempts === 1){
                        conf.globalData.auth.autoLoginAttempts++;
                    }
                    else{
                        // attempt to load a stage-specific template to render this form.  If not found, use the generic one.
                        uiUtils
                            .fillTemplateWithData("templates/openam/authn/" + reqs.stage + ".html", 
                                _.extend(conf.globalData, this.data),
                                _.bind(function (populatedTemplate) {
                                    if (typeof populatedTemplate === "string") { // a rendered template will be a string; an error will be an object
                                        this.template = "templates/openam/authn/" + reqs.stage + ".html";
                                    } else {
                                        this.template = this.genericTemplate;
                                    }

                                    this.data.showForgotPassword = false;
                                    this.data.showRegister = false;
                                    this.data.showSpacer = false;

                                    if(conf.globalData.auth.forgotPassword === "true"){
                                        this.data.showForgotPassword = true;
                                    }
                                    if(conf.globalData.auth.selfRegistration === "true"){
                                        if(this.data.showForgotPassword){
                                            this.data.showSpacer = true;
                                        }
                                        this.data.showRegister = true;
                                    }
                                    this.parentRender(_.bind(function() {
                                        this.reloadData();
                                    }, this));
                                }, this)
                            );
                    }
                
                }
            }, this))
            .fail(_.bind(function () {
                // If we can't render a login form, then the user must not be able to login
                this.template = this.unavailableTemplate;
                this.parentRender();
            }, this));

        },
        reloadData: function () {
            // This function is useful for adding logic that is used by stage-specific custom templates.
            
            var login = cookieHelper.getCookie("login");
            
            if(this.$el.find("[name=loginRemember]").length !== 0 && login) {
                this.$el.find("input[type=text]:first").val(login);
                this.$el.find("[name=loginRemember]").attr("checked","true");
                this.$el.find("[type=password]").focus();
            } else {
                this.$el.find(":input:first").focus();
            }
        },
        handleUrlParams: function() {
            var urlParams = uiUtils.convertCurrentUrlToJSON().params;
            
            //rest does not accept the params listed in the array below as is
            //they must be transformed into the 'authIndexType' and 'authIndexValue' params
            _.each(['authlevel','module','service','user'],function(p){
                if(urlParams[p]){
                    urlParams.authIndexType = p;
                    urlParams.authIndexValue = urlParams[p];
                    //***note special case for authLevel
                    conf.globalData.auth.additional += '&authIndexType=' + ((p === 'authlevel') ? 'level' : p) + '&authIndexValue=' + urlParams[p];
                }
            });
            
            //special case for SSORedirect
            if(urlParams.goto && urlParams.goto.indexOf('/SSORedirect') === 0){
                urlParams.goto = "/" + constants.context + urlParams.goto;
                conf.globalData.auth.additional.replace("&goto=","&goto=" + "/" + constants.context);
            }
            
            conf.globalData.auth.urlParams = urlParams;
            return urlParams;
        }
    }); 
    
    Handlebars.registerHelper("callbackRender", function () {
        var result = "",
            cb = this,
            prompt,
            options;
        
        prompt = _.find(cb.output, function (o) { return o.name === "prompt"; });
        if (prompt && prompt.value !== undefined && prompt.value.length) {
            if (cb.type === "ChoiceCallback") {
                result = '<label>' + prompt.value + '</label>';
            } else {
                result = '<label class="short">' + prompt.value + '</label>';
            }
        }
        
        switch (cb.type) {
            case "PasswordCallback" :
                result += '<input type="password" name="callback_' + cb.input.index + '" value="' + cb.input.value + '" data-validator="required" data-validator-event="keyup" />';
            break;
            
            case "TextInputCallback" :
                result += '<textarea name="callback_' + cb.input.index + '" data-validator="required" data-validator-event="keyup">' + cb.input.value + '</textarea>';
            break;
            case "TextOutputCallback" :
                result += '<div id="callback_' + cb.input.index + '" class="textOutputCallback ' + 
                            _.find(cb.output, function (o) { return o.name === "messageType"; }) + '">' + 
                          _.find(cb.output, function (o) { return o.name === "message"; }) + 
                          '</div>';
            break;
            
            case "ConfirmationCallback" : 
                options = _.find(cb.output, function (o) { return o.name === "options"; });
                if (options && options.value !== undefined) {
                    _.each(options.value, function (option, index) {
                        result += '<input name="callback_' + cb.input.index + '" type="submit" class="button active" index="'+ index +'" value="'+ option +'">';
                    });
                }
            break;
            case "ChoiceCallback" : 
                options = _.find(cb.output, function (o) { return o.name === "choices"; });
                if (options && options.value !== undefined) {
                    result += "<ul>";
                    _.each(options.value, function (option, index) {
                        var checked = (cb.input.value === index) ? " checked" : "";
                        result += '<li><label class="short light" for="callback_' + cb.input.index + '_'+ index +'">'+option+': </label><input '+ checked +' id="callback_' + cb.input.index + '_'+ index +'" name="callback_' + cb.input.index + '" type="radio" value="'+ index +'"></li>';
                    });
                    result += "</ul>";
                }
            break;
            default: 
                result += '<input type="text" name="callback_' + cb.input.index + '" value="' + cb.input.value + '" data-validator="required" data-validator-event="keyup" />';
            break;
        }
        
        return new Handlebars.SafeString(result);
    });
    
    return new LoginView();
});


