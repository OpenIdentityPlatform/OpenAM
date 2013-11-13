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
        baseTemplate: "templates/user/LoginBaseTemplate.html",
        
        data: {},
        events: {
            "click #continue": "continueProcess",
            "click #registerButton" : "register",
            "onValidate": "onValidate",
            "customValidate": "customValidate",
            "click .cancelButton": "cancel"
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
            var _this = this,
                postData = {
                        email: $('#email').val(),
                        subject: $.t("templates.user.UserRegistrationTemplate.emailSubject"),
                        message: $.t("templates.user.UserRegistrationTemplate.emailMessage")
                },
                success = function() {
                    _this.$el.find("#step1").hide();
                    _this.$el.find("#emailSent").show();
                },
                error = function(e) {
                    var response = JSON.parse(e.responseText);
                    _this.$el.find("input[type=submit]").removeClass('inactive').addClass('active');
                    if(response.message.indexOf("Email not sent") === 0) {
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "emailNotSent");
                    }
                };
            
            this.$el.find("input[type=submit]").addClass('inactive').removeClass('active');
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
                _this.$el.find("#step2").hide();
                _this.$el.find("#registerSuccess").show();
            },
            error = function(e) {
                var response = JSON.parse(e.responseText);
                _this.$el.find("input[type=submit]").removeClass('inactive').addClass('active');
                if(response.message.indexOf("ldap exception") > -1) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "userAlreadyExists");
                }
            };
            
            this.$el.find("input[type=submit]").addClass('inactive').removeClass('active');
            
            userDelegate.doAction("confirm",this.data.urlParams,function(d){
                _.extend(postData,d);
                userDelegate.doAction("anonymousCreate",postData,success,error);
            });
        },
        cancel: function(e) {
            e.preventDefault();
            var loginUrlParams = cookieHelper.getCookie("loginUrlParams");
            cookieHelper.deleteCookie("loginUrlParams");
            location.href = "#login" + ((loginUrlParams) ? loginUrlParams : "");
        },
        customValidate: function () {
            if(validatorsManager.formValidated(this.$el.find("#registration")) || validatorsManager.formValidated(this.$el.find("#forgotPassword"))) {
                this.$el.find("input[type=submit]").removeClass('inactive').addClass('active');
            }
            else {
                this.$el.find("input[type=submit]").addClass('inactive').removeClass('active');
            }
        }
    }); 
    
    return new RegisterView();
});


