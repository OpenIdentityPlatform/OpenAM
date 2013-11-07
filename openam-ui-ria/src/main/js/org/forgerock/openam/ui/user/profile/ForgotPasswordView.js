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

define("org/forgerock/openam/ui/user/profile/ForgotPasswordView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function(AbstractView, validatorsManager, userDelegate, conf, cookieHelper, eventManager, constants, uiUtils) {
    
    var ForgottenPasswordView = AbstractView.extend({
        template: "templates/openam/ForgotPasswordTemplate.html",
        
        data: {},
        events: {
            "click #continue": "continueProcess",
            "click #changePasswordButton" : "changePassword",
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
               $('#menu').hide();
               this.$el.find("input[type=submit]").addClass('inactive').removeClass('active');
               validatorsManager.bindValidators(this.$el);
            });
        },
        continueProcess: function(e) {
            e.preventDefault();
            var _this = this,
                postData = {
                        username: $('#username').val(),
                        subject: $.t("templates.user.ForgottenPasswordTemplate.emailSubject"),
                        message: $.t("templates.user.ForgottenPasswordTemplate.emailMessage")
                },
                success = function() {
                    $("#step1").hide();
                    $("#emailSent").show();
                },
                error = function(e) {
                    var response = JSON.parse(e.responseText);
                    _this.$el.find("input[type=submit]").removeClass('inactive').addClass('active');
                    if(response.message.indexOf("No email provided in profile.") === 0) {
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "noEmailProvided");
                    }
                    else if(response.reason.indexOf("Not Found") === 0) {
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "usernameNotFound");
                    } 
                };
            
            this.$el.find("input[type=submit]").addClass('inactive').removeClass('active');
            userDelegate.forgotPassword(postData,success,error);
        },
        changePassword: function(e) {
            e.preventDefault();
            var postData = {
                    userpassword: $('#password').val()
            },
            success = function() {
                $("#step2").hide();
                $("#passwordChangeSuccess").show();
            };
            _.extend(postData,this.data.urlParams);
            this.$el.find("input[type=submit]").addClass('inactive').removeClass('active');
            userDelegate.forgotPasswordReset(postData,success);
        },
        cancel: function(e) {
            e.preventDefault();
            var loginUrlParams = cookieHelper.getCookie("loginUrlParams");
            location.href = "#login" + loginUrlParams;
        },
        customValidate: function () {
            if(validatorsManager.formValidated(this.$el.find("#passwordChange")) || validatorsManager.formValidated(this.$el.find("#forgotPassword"))) {
                this.$el.find("input[type=submit]").removeClass('inactive').addClass('active');
            }
            else {
                this.$el.find("input[type=submit]").addClass('inactive').removeClass('active');
            }
        }
    }); 
    
    return new ForgottenPasswordView();
});


