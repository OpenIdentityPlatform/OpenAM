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

define("org/forgerock/openam/ui/user/login/RESTLoginView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/user/delegates/AuthNDelegate",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/main/EventManager",
    "form2js",
    "handlebars",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/user/login/RESTLoginHelper",
    "org/forgerock/openam/ui/common/util/RealmHelper",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/SessionManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/URIUtils"
], function ($, _, AbstractView, AuthNDelegate, BootstrapDialog, Configuration, Constants, CookieHelper, EventManager,
            Form2js, Handlebars, i18nManager, Messages, RESTLoginHelper, RealmHelper, Router, SessionManager, UIUtils,
            URIUtils) {

    function populateTemplate () {
        var self = this,
            firstAuthStage = Configuration.globalData.auth.currentStage === 1;

        // self-service links should be shown only on the first stage
        this.data.showForgotPassword = firstAuthStage && Configuration.globalData.forgotPassword === "true";
        this.data.showRegister = firstAuthStage && Configuration.globalData.selfRegistration === "true";
        this.data.showRememberLogin = firstAuthStage;

        if (Configuration.backgroundLogin) {
            this.prefillLoginData();

            BootstrapDialog.show({
                title: $.t("common.form.sessionExpired"),
                cssClass: "login-dialog",
                closable: false,
                message: $("<div></div>"),
                onshow: function () {
                    var dialog = this;
                    // change the target element of the view
                    self.noBaseTemplate = true;
                    self.element = dialog.message;
                },
                onshown: function () {
                    // return back to the default state
                    delete self.noBaseTemplate;
                    self.element = "#content";
                }
            });
        }
    }

    var LoginView = AbstractView.extend({
        template: "templates/openam/RESTLoginTemplate.html",
        genericTemplate: "templates/openam/RESTLoginTemplate.html",
        unavailableTemplate: "templates/openam/RESTLoginUnavailableTemplate.html",
        baseTemplate: "templates/common/LoginBaseTemplate.html",

        data: {},
        events: {
            "click input[type=submit]": "formSubmit",
            "click #passwordReset": "selfServiceClick",
            "click #register": "selfServiceClick"
        },

        selfServiceClick: function (e) {
            e.preventDefault();

            var overrideRealmParameter = RealmHelper.getOverrideRealm(),
                realmPath;

            if (overrideRealmParameter) {
                realmPath = overrideRealmParameter.substring(0, 1) === "/"
                    ? overrideRealmParameter.slice(1)
                    : overrideRealmParameter;
            } else {
                realmPath = RealmHelper.getSubRealm();
            }

            location.href = e.target.href + realmPath;
        },

        autoLogin: function () {
            var index,
                submitContent = {},
                auth = Configuration.globalData.auth;

            _.each(_.keys(auth.urlParams), function (key) {
                if (key.indexOf("IDToken") > -1) {
                    index = parseInt(key.substring(7), 10) - 1;
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

            // Known to be used by username/password based authn stages
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

        render: function (args) {
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

            AuthNDelegate.getRequirements().then(_.bind(function (reqs) {
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

                        if (String(auth.passedInRealm).toLowerCase() === auth.subRealm.toLowerCase() &&
                            urlParams.ForceAuth !== "true") {
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
                                if (Configuration.gotoURL &&
                                    _.indexOf(["#", "", "#/", "/#"], Configuration.gotoURL) === -1) {
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
            }, this), _.bind(function (error) {
                // If we can't render a login form, then the user must not be able to login
                this.template = this.unavailableTemplate;
                this.parentRender(function () {
                    if (error) {
                        Messages.addMessage({
                            type: Messages.TYPE_DANGER,
                            message: error.message
                        });
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
                promise = $.Deferred(),
                usernamePasswordStages = ["DataStore1", "AD1", "JDBC1", "LDAP1", "Membership1", "RADIUS1"],
                template,
                self = this;

            cleaned.callbacks = [];
            _.each(reqs.callbacks, function (element) {

                var redirectForm,
                    redirectCallback;

                if (element.type === "RedirectCallback") {

                    redirectCallback = _.object(_.map(element.output, function (o) {
                        return [o.name, o.value];
                    }));

                    redirectForm = $("<form action='" + redirectCallback.redirectUrl + "' method='POST'></form>");

                    if (redirectCallback.redirectMethod === "POST") {

                        _.each(redirectCallback.redirectData, function (v, k) {
                            redirectForm.append("<input type='hidden' name='" + k + "' value='" + v + "' />");
                        });

                        redirectForm.appendTo("body").submit();
                    } else {
                        window.location.replace(redirectCallback.redirectUrl);
                    }
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
                        value: [$.t("common.user.login")]
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
                template = usernamePasswordStages.indexOf(reqs.stage) !== -1
                    ? "templates/openam/authn/UsernamePasswordStage.html"
                    : "templates/openam/authn/" + reqs.stage + ".html";

                UIUtils.compileTemplate(template, _.extend(Configuration.globalData, this.data))
                    .always(function (compiledTemplate) {
                        // A rendered template will be a string; an error will be an object
                        self.template = typeof compiledTemplate === "string"
                            ? template : self.genericTemplate;

                        populateTemplate.call(self);
                        self.parentRender(function () {
                            self.prefillLoginData();
                            // Resolve a promise when all templates will be loaded
                            promise.resolve();
                        });
                    });
            }
            return promise;
        },
        prefillLoginData: function () {
            var login = CookieHelper.getCookie("login");

            if (this.$el.find("[name=loginRemember]").length !== 0 && login) {
                this.$el.find("input[type=text]:first").val(login);
                this.$el.find("[name=loginRemember]").attr("checked", "true");
                this.$el.find("[type=password]").focus();
            } else {
                this.$el.find(":input:not([type='radio']):not([type='checkbox'])" +
                    ":not([type='submit']):not([type='button']):first").focus();
            }
        },

        handleUrlParams: function () {
            var urlParams = URIUtils.parseQueryString(URIUtils.getCurrentCompositeQueryString());

            // Rest does not accept the params listed in the array below as is
            // they must be transformed into the "authIndexType" and "authIndexValue" params
            _.each(["authlevel", "module", "service", "user", "resource", "resourceURL"], function (param) {
                if (urlParams[param]) {
                    urlParams.authIndexType = ((param === "authlevel") ? "level" : param);
                    urlParams.authIndexValue = urlParams[param];
                    //*** Note special case for authLevel
                    Configuration.globalData.auth.additional += "&authIndexType=" +
                        ((param === "authlevel") ? "level" : param) + "&authIndexValue=" + urlParams[param];
                }
            });

            // Special case for SSORedirect
            if (urlParams.goto && urlParams.goto.indexOf("/SSORedirect") === 0) {
                urlParams.goto = "/" + Constants.context + urlParams.goto;
                Configuration.globalData.auth.additional.replace("&goto=", "&goto=" + "/" + Constants.context);
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
                    defaultOption = options.value.length > 1
                        ? _.find(this.output, { name: "defaultOption" }) : { "value": 0 };

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
