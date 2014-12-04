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

/*global define, $, form2js, _, js2form, Handlebars, window */

define("org/forgerock/openam/ui/user/profile/RegisterView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function(AbstractView, validatorsManager, userDelegate, conf, cookieHelper, eventManager, constants, uiUtils) {
    
    var RegisterView = AbstractView.extend({
        template: "templates/openam/RegisterTemplate.html",
        baseTemplate: "templates/common/MediumBaseTemplate.html",
        
        data: {},
        events: {
            "click #continue": "continueProcess",
            "click #registerButton" : "register",
            "onValidate": "onValidate",
            "customValidate": "customValidate",
            "click .cancelButton": "cancel"
        },
        errorsHandlers: { 
            "Bad Request":  { status: "400" },
            "Not found":    { status: "404" },
            "Conflict":     { status: "409" }
        },
        render: function(args, callback) {
            this.data.urlParams = uiUtils.convertCurrentUrlToJSON().params;
            this.data.isStageOne = true;
            if (this.data.urlParams) {
                this.data.isStageOne = false;
            }
            this.parentRender(function() {
               validatorsManager.bindValidators(this.$el);
            });
        },
        continueProcess: function(e) {
            e.preventDefault();
            $('#email').prop('readonly', true);
            var _this = this,
                postData = {
                        email: $('#email').val(),
                        subject: $.t("templates.user.UserRegistrationTemplate.emailSubject"),
                        message: $.t("templates.user.UserRegistrationTemplate.emailMessage")
                },
                success = function() {
                    _this.$el.find("#emailSent").slideDown();
                    _this.$el.find("#step1").slideUp();   
                },
                error = function(e) {
                    var response = JSON.parse(e.responseText);
                    _this.$el.find("input[type=submit]").prop('disabled', true);
                    $('#email').prop('readonly', false);
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unableToRegister");
                };
            
            this.$el.find("input[type=submit]").prop('disabled', true);
            userDelegate.doAction("register",postData,success,error);
        },
        register: function(e) {
            e.preventDefault();
            
            var confirmParams,
            _this = this,
            postData = _.extend(
                    form2js(this.$el.find("#registration")[0]),
                    {userpassword:this.$el.find("#password").val()}
            ),
            success = function() {

                switch(conf.globalData.successfulUserRegistrationDestination){
                    
                    case 'login':
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, 'afterRegistration');
                        _this.cancel();
                        
                    break;
                    case 'autologin':
                        eventManager.sendEvent(constants.EVENT_USER_SUCCESSFULLY_REGISTERED, { user: {userName:postData.username, password:postData.password}, autoLogin: true });  
                    break;
                    default: 
                        _this.$el.find("#step2").slideUp();
                        _this.$el.find("#registerSuccess").fadeIn();
                    break;
                }

            },
            error = function(e) {
                var responseMessage = JSON.parse(e.responseText).message,
                    responseCode = JSON.parse(e.responseText).code;
                _this.$el.find("input[type=submit]").prop('disabled', false);
                if (responseMessage.indexOf("ldap exception") > -1) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "userAlreadyExists");
                } else if (responseMessage.indexOf("Identity names may not have a space character" )> -1) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "identityNoSpace");
                } else if (responseCode === 400) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "selfRegistrationDisabled");
                    _this.$el.find("input[type=submit]").prop('disabled', true);
                } else if (responseCode === 409) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "userAlreadyExists");
                }
            };

            userDelegate.doAction("confirm", this.data.urlParams,
                function(d){
                    _.extend(postData,d);
                    userDelegate.doAction("anonymousCreate",postData,success,error,_this.errorsHandlers);
                },
                function(e){
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unableToRegister");
                },
                _this.errorsHandlers
            );

           this.$el.find("input[type=submit]").prop('disabled', true);

        },
        cancel: function(e) {
            if(e){
                e.preventDefault();
            }
            var loginUrlParams = cookieHelper.getCookie("loginUrlParams");
            cookieHelper.deleteCookie("loginUrlParams"); 
            location.href = "#login" + ((loginUrlParams) ? loginUrlParams : conf.globalData.auth.realm);
        },
        customValidate: function () {
            if(validatorsManager.formValidated(this.$el.find("#registration")) || validatorsManager.formValidated(this.$el.find("#forgotPassword"))) {
                this.$el.find("input[type=submit]").prop('disabled', false);
            }
            else {
                this.$el.find("input[type=submit]").prop('disabled', true);
            }
        }
    }); 
    
    return new RegisterView();
});


