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

/*global define */
define("org/forgerock/openam/ui/user/profile/ForgotPasswordView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function ($, _, AbstractView, validatorsManager, userDelegate, conf,
             cookieHelper, eventManager, constants, Router, RealmHelper) {
    var ForgottenPasswordView = AbstractView.extend({
        template: "templates/openam/ForgotPasswordTemplate.html",
        baseTemplate: "templates/common/LoginBaseTemplate.html",
        data: {},
        events: {
            "click #continue": "continueProcess",
            "click #changePasswordButton" : "changePassword",
            "onValidate": "onValidate",
            "customValidate": "customValidate",
            "click #cancel": "redirectToLogin",
            "click #gotoLogin": "redirectToLogin"
        },
        errorsHandlers: {
            "Bad Request":              { status: "400" },
            "Not found":                { status: "404" },
            "Gone":                     { status: "410" },
            "Internal Server Error":    { status: "500" },
            "Service Unavailable":      { status: "503" }
        },
        render: function (args, callback) {
            this.data.urlParams = Router.convertCurrentUrlToJSON().params;
            this.data.isStageOne = _.keys(_.pick(this.data.urlParams, "confirmationId", "tokenId")).length !== 2;

            this.parentRender (function () {
                validatorsManager.bindValidators(this.$el);
            });
        },
        continueProcess: function (e) {
            e.preventDefault();
            $("#username").prop("readonly", true);
            var self = this,
                postData = {
                    username: $("#username").val(),
                    subject: $.t("templates.user.ForgottenPasswordTemplate.emailSubject"),
                    message: $.t("templates.user.ForgottenPasswordTemplate.emailMessage")
                },
                success = function () {
                    self.$el.find("#step1").slideUp();
                    self.$el.find("#emailSent").slideDown();
                },
                error = function (e) {
                    self.$el.find("input[type=submit]").prop("disabled", true);
                    self.$el.find("#username").prop("readonly", false).parent().addClass("has-error");

                    self.displayError(e);
                };

            this.$el.find("input[type=submit]").prop("disabled", true);

            userDelegate.doAction("forgotPassword",postData,success,error,self.errorsHandlers);
        },
        changePassword: function (e) {
            e.preventDefault();
            this.$el.find("#password").prop("readonly", true);
            this.$el.find("#passwordConfirm").prop("readonly", true);
            this.$el.find("input[type=submit]").prop("disabled", true);

            var self = this,
                postData = {
                    userpassword: this.$el.find("#password").val()
                },
                success = function () {
                    self.$el.find("#step2").slideUp();
                    self.$el.find("#passwordChangeSuccess").fadeIn();
                },
                error = function (e) {
                    self.$el.find("#password").prop("readonly", false);
                    self.$el.find("#passwordConfirm").prop("readonly", false);
                    self.displayError(e);
                };
            _.extend(postData,this.data.urlParams);
            userDelegate.doAction("forgotPasswordReset",postData,success,error,self.errorsHandlers);
        },
        redirectToLogin: function (event) {
            event.preventDefault();

            var loginUrlParams = cookieHelper.getCookie("loginUrlParams"),
                // In "Stage One" realm is specified via #getSubRealm, in "Stage Two" it's via #getOverrideRealm
                realm = RealmHelper.getSubRealm() || (RealmHelper.getOverrideRealm() ? RealmHelper.getOverrideRealm().substr(1) : "");
            cookieHelper.deleteCookie("loginUrlParams");

            location.href = "#login" + ((loginUrlParams) ? loginUrlParams : "/" + realm);
        },
        customValidate: function () {
            this.$el.find("#username").parent().removeClass("has-error");
            if (validatorsManager.formValidated(this.$el.find("#passwordChange")) ||
                validatorsManager.formValidated(this.$el.find("#forgotPassword"))) {
                this.$el.find("input[type=submit]").prop("disabled", false);
            } else {
                this.$el.find("input[type=submit]").prop("disabled", true);
            }
        },
        displayError: function (e) {
            var message = "notFoundError", responseMessage = JSON.parse(e.responseText).message;
            switch (e.status) {
                case 400:
                    /* //not required as the XUI does not allow it
                    else if (responseMessage === "Username not provided") {
                        message = "noUserNameProvided";
                    }*/
                    break;

                case 404:
                    message = "usernameNotFound";
                    break;

                case 410: //Forgotten Password Link Expired
                    message = "tokenNotFound";
                    break;

                case 500: //Invalid Server Configuration
                    if (responseMessage === "No email provided in profile.") {
                        message = "noEmailProvided";
                    } else {
                        message = "internalError";
                    }
                    break;

                case 503: //503 - Forgot password is not accessible.
                    message = "serviceUnavailable";
                    break;
            }

            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, message);
        }
    });

    return new ForgottenPasswordView();
});
