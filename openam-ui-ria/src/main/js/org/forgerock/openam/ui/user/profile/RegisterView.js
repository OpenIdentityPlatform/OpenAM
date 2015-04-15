/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS.
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
], function(AbstractView, ValidatorsManager, UserDelegate, Configuration, CookieHelper, EventManager, Constants, UiUtils) {

    var RegisterView = AbstractView.extend({
        template: "templates/openam/RegisterTemplate.html",
        baseTemplate: "templates/common/LoginBaseTemplate.html",
        data: {},
        events: {
            "click #continue": "continueProcess",
            "click #submit" : "register",
            "onValidate": "onValidate",
            "customValidate": "customValidate",
            "click #gotoLogin": "gotoLogin",
            "click #cancel": "gotoLogin"
        },
        errorsHandlers: {
            "Bad Request":  { status: "400" },
            "Not found":    { status: "404" },
            "Conflict":     { status: "409" }
        },
        render: function(args, callback) {
            this.data.urlParams = UiUtils.convertCurrentUrlToJSON().params;
            this.data.isStageOne = _.keys(_.pick(this.data.urlParams, 'confirmationId', 'tokenId')).length !== 2;

            this.parentRender(function() {
                ValidatorsManager.bindValidators(this.$el);
            });
        },
        continueProcess: function(e) {
            e.preventDefault();

            var self = this,
                emailInput = this.$el.find('#stageOne #mail'),
                submitButton = this.$el.find("#continue"),
                postData = {
                        email: emailInput.val(),
                        subject: $.t("templates.user.UserRegistrationTemplate.emailSubject"),
                        message: $.t("templates.user.UserRegistrationTemplate.emailMessage")
                },
                success = function() {
                    self.$el.find("#step2").slideDown();
                    self.$el.find("#step1").slideUp();
                },
                error = function(e) {
                    var response = JSON.parse(e.responseText);
                    submitButton.prop('disabled', true);
                    emailInput.prop('readonly', false);
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unableToRegister");
                };

            UserDelegate.doAction("register",postData,success,error);
            emailInput.prop('readonly', true);
            submitButton.prop('disabled', true);

        },
        register: function(e) {
            e.preventDefault();

            var confirmParams,
                self = this,
            postData = _.extend(
                        form2js(this.$el.find("#stageTwo")[0]),
                    {userpassword:this.$el.find("#password").val()}
            ),
                submitButton = self.$el.find("#submit"),
            success = function() {

                    switch(Configuration.globalData.successfulUserRegistrationDestination){

                    case 'login':
                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, 'afterRegistration');
                            self.gotoLogin();

                    break;
                    case 'autologin':
                            EventManager.sendEvent(Constants.EVENT_USER_SUCCESSFULLY_REGISTERED, { user: {userName:postData.username, password:postData.password}, autoLogin: true });
                    break;
                    default:
                            self.$el.find("#step3").slideUp();
                            self.$el.find("#step4").fadeIn();
                    break;
                }

            },
            error = function(e) {
                var responseMessage = JSON.parse(e.responseText).message,
                    responseCode = JSON.parse(e.responseText).code;

                    submitButton.prop('disabled', false);
                if (responseMessage.indexOf("ldap exception") > -1) {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unableToRegister");
                } else if (responseMessage.indexOf("Identity names may not have a space character" )> -1) {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "identityNoSpace");
                } else if (responseCode === 400) {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "selfRegistrationDisabled");
                        submitButton.prop('disabled', true);
                } else if (responseCode === 409) {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "userAlreadyExists");
                }
            };

            UserDelegate.doAction("confirm", this.data.urlParams,
                function(data){
                    _.extend(postData,data);
                    UserDelegate.doAction("anonymousCreate",postData,success,error,self.errorsHandlers);
                },
                function(e){
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unableToRegister");
                },
                self.errorsHandlers
            );

            submitButton.prop('disabled', true);

        },
        gotoLogin: function(e) {
            if(e){ e.preventDefault();}
            var loginUrlParams = CookieHelper.getCookie("loginUrlParams");
            CookieHelper.deleteCookie("loginUrlParams");
            location.href = "#login" + ((loginUrlParams) ? loginUrlParams : "/" + Configuration.globalData.auth.subRealm);
        },
        customValidate: function () {
            if(ValidatorsManager.formValidated(this.$el.find("#stageTwo")) || ValidatorsManager.formValidated(this.$el.find("#stageOne"))) {
                this.$el.find("input[type=submit]").prop('disabled', false);
            }
            else {
                this.$el.find("input[type=submit]").prop('disabled', true);
            }
        }
    });

    return new RegisterView();
});
