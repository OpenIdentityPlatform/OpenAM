/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Portions copyright 2011-2015 ForgeRock AS.
 */

/*global define, window*/
define("org/forgerock/openam/ui/user/login/RESTLoginView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/user/delegates/AuthNDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/main/EventManager",
    "form2js",
    "handlebars",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/util/ModuleLoader",
    "org/forgerock/openam/ui/user/login/RESTLoginHelper",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/SessionManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/URIUtils"
], function ($, _, AbstractView, AuthNDelegate, Configuration, Constants, CookieHelper, EventManager, Form2js,
             Handlebars, i18nManager, Messages, ModuleLoader, RESTLoginHelper, Router, SessionManager, UIUtils,
             URIUtils) {

    var LoginView = AbstractView.extend({

        template: "templates/openam/RESTLoginTemplate.html",
        genericTemplate: "templates/openam/RESTLoginTemplate.html",
        unavailableTemplate: "templates/openam/RESTLoginUnavailableTemplate.html",
        baseTemplate: "templates/common/LoginBaseTemplate.html",

        data: {},
        events: {
            "click input[type=submit]": "formSubmit",
            "click #forgotPassword": "selfServiceClick",
            "click #register": "selfServiceClick"
        },
        selfServiceClick: function (e) {
            e.preventDefault();
            /**
             *  Save the login params in a cookie for use with the cancel button on forgotPassword/register page
             *  and also the "proceed to login" link once password has been successfully changed or registration
             *  is complete.
             */
            var expire = new Date(),
                cookieVal = "/" + Configuration.globalData.auth.subRealm,
                href = e.target.href + "/";

            if (Configuration.globalData.auth.urlParams) {
                cookieVal += RESTLoginHelper.filterUrlParams(Configuration.globalData.auth.urlParams);
            }
            expire.setDate(expire.getDate() + 1);
            CookieHelper.setCookie("loginUrlParams",cookieVal,expire);
            if (Configuration.globalData.auth.subRealm) {
                href += Configuration.globalData.auth.subRealm;
            }
            location.href = href;
        },
        autoLogin: function () {
            var index,
                submitContent = {},
                auth = Configuration.globalData.auth;

            _.each(_.keys(auth.urlParams),function (key) {
                if (key.indexOf("IDToken") > -1) {
                    index = parseInt(key.substring(7),10) - 1;
                    submitContent["callback_" + index] = auth.urlParams["IDToken" + key.substring(7)];
                }
            });
            auth.autoLoginAttempts = 1;
            EventManager.sendEvent(Constants.EVENT_LOGIN_REQUEST, submitContent);
        },
        isZeroPageLoginAllowed: function () {
            var referer = document.referrer,
                whitelist = Configuration.globalData.zeroPageLogin.refererWhitelist;

            if (!Configuration.globalData.zeroPageLogin.enabled) {
                return false;
            }

            if (!referer) {
                return Configuration.globalData.zeroPageLogin.allowedWithoutReferer;
            }

            return !whitelist || !whitelist.length || whitelist.indexOf(referer) > -1;
        },
        formSubmit: function (e) {
            var submitContent, expire;
            e.preventDefault();

            submitContent = new Form2js(this.$el[0]);
            submitContent[$(e.target).attr("name")] = $(e.target).attr("index");

            // START CUSTOM STAGE-SPECIFIC LOGIC HERE

            // Known to be used by DataStore1.html
            if (this.$el.find("[name=loginRemember]:checked").length !== 0) {
                expire = new Date();
                expire.setDate(expire.getDate() + 20);
                // An assumption that the login name is the first text input box
                CookieHelper.setCookie("login", this.$el.find("input[type=text]:first").val(), expire);
            } else if (this.$el.find("[name=loginRemember]").length !== 0) {
                CookieHelper.deleteCookie("login");
            }

            // END CUSTOM STAGE-SPECIFIC LOGIC HERE

            EventManager.sendEvent(Constants.EVENT_LOGIN_REQUEST, submitContent);
        },
        render: function (args, callback) {
            var urlParams = {}, // Deserialized querystring params
                promise = $.Deferred(),
                auth = Configuration.globalData.auth;

            if (args && args.length) {
                auth.additional = args[1]; // May be "undefined"
                auth.urlParams = urlParams;

                if (args[1]) {
                    urlParams = this.handleUrlParams();
                }

                // If there are IDTokens try to login with the provided credentials
                if (urlParams.IDToken1 && this.isZeroPageLoginAllowed() && !auth.autoLoginAttempts) {
                    this.autoLogin();
                }
            }

            AuthNDelegate.getRequirements().done(_.bind(function (reqs) {

                var auth = Configuration.globalData.auth;

                // Clear out existing session if instructed
                if (reqs.hasOwnProperty("tokenId") && urlParams.arg === "newsession") {
                    RESTLoginHelper.removeSession();
                    Configuration.setProperty("loggedUser", null);
                }

                // If simply by asking for the requirements, we end up with a token,
                // then we must have auto-logged-in somehow
                if (reqs.hasOwnProperty("tokenId")) {
                    // Set a variable for the realm passed into the browser so there can be a
                    // check to make sure it is the same as the current user's realm
                    auth.passedInRealm = auth.subRealm;
                    // If we have a token, let's see who we are logged in as....
                    SessionManager.getLoggedUser(function (user) {

                        if (String(auth.passedInRealm).toLowerCase() === auth.subRealm.toLowerCase()
                            && urlParams.ForceAuth !== "true")
                        {
                            Configuration.setProperty("loggedUser", user);
                            delete auth.passedInRealm;

                            RESTLoginHelper.setSuccessURL(reqs.tokenId).then(function () {

                                if (auth.urlParams && auth.urlParams.goto) {
                                    window.location.href = auth.urlParams.goto;
                                    $("body").empty();
                                    return false;
                                }
                                EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, {
                                    anonymousMode: false
                                });

                                // Copied from EVENT_LOGIN_REQUEST handler
                                if (Configuration.gotoURL
                                    && _.indexOf(["#", "", "#/", "/#"], Configuration.gotoURL) === -1)
                                {
                                    console.log("Auto redirect to " + Configuration.gotoURL);
                                    Router.navigate(Configuration.gotoURL, { trigger: true });
                                    delete Configuration.gotoURL;
                                } else {
                                    Router.navigate("", { trigger: true });
                                }
                            });
                        } else {
                            location.href = "#confirmLogin/";
                        }
                    }, function () {
                        // There is a tokenId but it is invalid so kill it
                        RESTLoginHelper.removeSession();
                        Configuration.setProperty("loggedUser", null);
                    });

                } else { // We aren't logged in yet, so render a form...
                    this.renderForm(reqs, urlParams);
                    promise.resolve();

                }
            }, this))
            .fail(_.bind(function (error) {
                // If we can't render a login form, then the user must not be able to login
                this.template = this.unavailableTemplate;
                this.parentRender(function () {
                    if (error) {
                        Messages.messages.addMessage(error);
                    }
                });

            }, this));

            promise.done(function () {
                if (CookieHelper.getCookie("invalidRealm")) {
                    CookieHelper.deleteCookie("invalidRealm");
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidRealm");
                }
            });
        },
        renderForm: function (reqs, urlParams) {
            var cleaned = _.clone(reqs),
                implicitConfirmation = true,
                promise = $.Deferred();

            cleaned.callbacks = [];
            _.each(reqs.callbacks, function (element) {

                if (element.type === "RedirectCallback") {
                    window.location.replace(element.output.object[0].value);
                }

                if (element.type === "ConfirmationCallback") {
                    implicitConfirmation = false;
                }

                cleaned.callbacks.push({
                    input: {
                        index: cleaned.callbacks.length,
                        name: element.input ? element.input[0].name : null,
                        value: element.input ? element.input[0].value : null
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
                    output: [{
                        name: "options",
                        value: [ $.t("common.user.login") ]
                    }],
                    type: "ConfirmationCallback",
                    isSubmit: true
                });
            }

            this.reqs = reqs;
            this.data.reqs = cleaned;

            // Is there an attempt at autologin happening?
            // if yes then don't render the form until it fails one time
            if (urlParams.IDToken1 && Configuration.globalData.auth.autoLoginAttempts === 1) {
                Configuration.globalData.auth.autoLoginAttempts++;
            } else {
                // Attempt to load a stage-specific template to render this form.  If not found, use the generic one.
                UIUtils.fillTemplateWithData(
                    "templates/openam/authn/" + reqs.stage + ".html",
                    _.extend(Configuration.globalData, this.data),
                     _.bind(function (populatedTemplate)
                {
                    // A rendered template will be a string; an error will be an object
                    if (typeof populatedTemplate === "string") {
                        this.template = "templates/openam/authn/" + reqs.stage + ".html";
                    } else {
                        this.template = this.genericTemplate;
                    }

                    this.data.showForgotPassword = false;
                    this.data.showRegister = false;
                    this.data.showSpacer = false;

                    if (Configuration.globalData.forgotPassword === "true") {
                        this.data.showForgotPassword = true;
                    }
                    if (Configuration.globalData.selfRegistration === "true") {
                        if (this.data.showForgotPassword) {
                            this.data.showSpacer = true;
                        }
                        this.data.showRegister = true;
                    }

                    if (Configuration.backgroundLogin) {
                        this.reloadData();
                        var self = this,
                            args = {
                                title: $.t("common.form.sessionExpired"),
                                cssClass: "loginDialog",
                                closable: false,
                                message: $(populatedTemplate),
                                onshow: function (dialog) {
                                    self.element = dialog.$modal;
                                    dialog.$modalBody.find("form").removeClass("col-sm-6 col-sm-offset-3");
                                    self.rebind();
                                }
                            };
                        ModuleLoader.load("bootstrap-dialog").then(function (BootstrapDialog) {
                            BootstrapDialog.show(args);
                        });
                        return;
                    }

                    this.parentRender(_.bind(function () {
                        this.reloadData();
                        // Resolve a promise when all templates will be loaded
                        promise.resolve();
                    }, this));
                }, this));
            }
            return promise;
        },
        reloadData: function () {
            // This function is useful for adding logic that is used by stage-specific custom templates.

            var login = CookieHelper.getCookie("login");

            if (this.$el.find("[name=loginRemember]").length !== 0 && login) {
                this.$el.find("input[type=text]:first").val(login);
                this.$el.find("[name=loginRemember]").attr("checked","true");
                this.$el.find("[type=password]").focus();
            } else {
                this.$el.find(":input:not([type='radio']):not([type='checkbox'])"
                    + ":not([type='submit']):not([type='button']):first").focus();
            }
        },
        handleUrlParams: function () {
            var urlParams = URIUtils.parseQueryString(URIUtils.getCurrentCompositeQueryString());

            // Rest does not accept the params listed in the array below as is
            // they must be transformed into the "authIndexType" and "authIndexValue" params
            _.each(["authlevel","module","service","user"],function (p) {
                if (urlParams[p]) {
                    urlParams.authIndexType = ((p === "authlevel") ? "level" : p);
                    urlParams.authIndexValue = urlParams[p];
                    //***note special case for authLevel
                    Configuration.globalData.auth.additional += "&authIndexType=" + ((p === "authlevel") ? "level" : p)
                        + "&authIndexValue=" + urlParams[p];
                }
            });

            // Special case for SSORedirect
            if (urlParams.goto && urlParams.goto.indexOf("/SSORedirect") === 0) {
                urlParams.goto = "/" + Constants.context + urlParams.goto;
                Configuration.globalData.auth.additional.replace("&goto=","&goto=" + "/" + Constants.context);
            }

            Configuration.globalData.auth.urlParams = urlParams;
            return urlParams;
        }
    });

    Handlebars.registerHelper("callbackRender", function () {
        var result = "", self = this, prompt = "", options, defaultOption, btnClass = "", renderContext;

        _.find(this.output, function (obj) {
            if (obj.name === "prompt" && obj.value !== undefined && obj.value.length) {
                prompt = obj.value.replace(/:$/, "");
            }
        });

        renderContext = {
            index: this.input.index,
            value: this.input.value,
            prompt: prompt
        };

        function renderPartial (name, context) {
            return _.find(Handlebars.partials, function (code, templateName) {
                return templateName.indexOf("login/_" + name) !== -1;
            })(_.merge(renderContext, context));
        }

        switch (this.type) {
            case "PasswordCallback": result += renderPartial("Password"); break;
            case "TextInputCallback": result += renderPartial("TextInput"); break;
            case "TextOutputCallback":
                options = {
                    message: _.find(this.output, { name: "message" }),
                    type: _.find(this.output, { name: "messageType" })
                };

                // Magic number 4 is for a <script>, taken from ScriptTextOutputCallback.java
                if (options.type.value === "4") {
                    result += renderPartial("ScriptTextOutput", {
                        messageValue: options.message.value
                    });
                } else {
                    result += renderPartial("TextOutput", {
                        typeValue: options.type.value,
                        messageValue: options.message.value
                    });
                }
                break;
            case "ConfirmationCallback":
                options = _.find(this.output, { name: "options" });

                if (options && options.value !== undefined) {
                    // if there is only one option then mark it as default.
                    defaultOption = options.value.length > 1 ?
                        _.find(this.output, { name: "defaultOption" }) : { "value": 0 };

                    _.each(options.value, function (option, key) {
                        btnClass = (defaultOption && defaultOption.value === key) ? "btn-primary" : "btn-default";
                        result += renderPartial("Confirmation", {
                            btnClass: btnClass,
                            key: key,
                            option: option
                        });
                    });
                }
                break;
            case "ChoiceCallback":
                options = _.find(this.output, { name: "choices" });

                // FIXME: If more than two then maybe a vertical radio list.
                if (options && options.value !== undefined) {
                    result += renderPartial("Choice", {
                        values: _.map(options.value, function (option, key) {
                            var checked = (self.input.value === key) ? "checked" : "", // Default option
                                active = checked ? "active" : "";

                            return {
                                active: active,
                                checked: checked,
                                key: key,
                                value: option
                            };
                        })
                    });
                }
                break;
            case "HiddenValueCallback": result += renderPartial("HiddenValue"); break;
            case "RedirectCallback": result += renderPartial("Redirect"); break;
            default: result += renderPartial("Default"); break;
        }

        return new Handlebars.SafeString(result);
    });

    return new LoginView();
});
