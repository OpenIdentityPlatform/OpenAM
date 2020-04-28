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
 * Portions copyright 2011-2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/user/services/AuthNService",
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
    "org/forgerock/openam/ui/common/util/isRealmChanged",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/SessionManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "org/forgerock/openam/ui/user/login/logout",
    "org/forgerock/openam/ui/common/util/uri/query",
    "org/forgerock/openam/ui/user/login/gotoUrl",
    "store/index"
], ($, _, AbstractView, AuthNService, BootstrapDialog, Configuration, Constants, CookieHelper, EventManager, Form2js,
    Handlebars, i18nManager, Messages, RESTLoginHelper, isRealmChanged, Router, SessionManager, UIUtils,
    URIUtils, logout, query, gotoUrl, store) => {
    isRealmChanged = isRealmChanged.default;

    function hasSsoRedirectOrPost (goto) {
        let decodedGoto;
        if (goto) {
            decodedGoto = decodeURIComponent(goto);
        }
        return goto && (_.startsWith(decodedGoto, "/SSORedirect") || _.startsWith(decodedGoto, "/SSOPOST"));
    }

    function populateTemplate () {
        var self = this;

        // self-service links should be shown only on the first stage of the username/password stages
        this.data.firstUserNamePassStage = Configuration.globalData.auth.currentStage === 1 && this.userNamePasswordStage;
        this.data.showForgotPassword = this.userNamePasswordStage && Configuration.globalData.forgotPassword === "true";
        this.data.showForgotUserName = this.userNamePasswordStage && Configuration.globalData.forgotUsername === "true";
        this.data.showForgotten = this.data.showForgotPassword || this.data.showForgotUserName;
        this.data.showSelfRegistration = this.userNamePasswordStage && Configuration.globalData.selfRegistration === "true";
        this.data.showRememberLogin = this.userNamePasswordStage;
        // socialImplementations links should be shown only on the first stage of the username/password stages
        // and should not show on the upgrade session page
        this.data.showSocialLogin = firstUserNamePassStage && !Configuration.loggedUser &&
                                        !_.isEmpty(Configuration.globalData.socialImplementations);

        if (Configuration.backgroundLogin) {
            this.prefillLoginData();

            BootstrapDialog.show({
                title: $.t("common.form.sessionExpired"),
                cssClass: "login-dialog",
                closable: false,
                message: $("<div></div>"),
                onshow () {
                    var dialog = this;
                    // change the target element of the view
                    self.noBaseTemplate = true;
                    self.element = dialog.message;
                },
                onshown () {
                    // return back to the default state
                    delete self.noBaseTemplate;
                    self.element = "#content";
                }
            });
        }
    }

    function routeToLoginUnavailable (fragmentParams) {

        // We cannot use the Router.getLink() method here and simply apply the subrealm to the route because
        // Router.getLink() does more than its title suggests. It also applies the default properties to the route and
        // these are not always correct if there has been a previous successful login request.
        // FIXME: Remove any session specific properties from the UI upon session end.
        Router.routeTo(Router.configuration.routes.loginFailure, {
            args: [fragmentParams],
            trigger: true
        });
    }

    /**
     * Checks if the "PollingWaitCallback" is present on the current stage.
     * @param {Object} requirements Information about current stage
     * @returns {Boolean} if the "PollingWaitCallback" is present on the current stage
     */
    function hasPollingCallback (requirements) {
        return _.some(requirements.callbacks, "type", "PollingWaitCallback");
    }

    /**
     * Checks if the "ConfirmationCallback" is present on the current stage.
     * @param {Object} requirements Information about current stage
     * @returns {Boolean} if the "ConfirmationCallback" is present on the current stage
     */
    function hasConfirmationCallback (requirements) {
        return _.some(requirements.callbacks, "type", "ConfirmationCallback");
    }

    function getFragmentParamString () {
        const params = URIUtils.getCurrentFragmentQueryString();
        return _.isEmpty(params) ? "" : `&${params}`;
    }

    var LoginView = AbstractView.extend({
        template: "templates/openam/RESTLoginTemplate.html",
        genericTemplate: "templates/openam/RESTLoginTemplate.html",
        baseTemplate: "templates/common/LoginBaseTemplate.html",

        data: {},
        events: {
            "click input[type=submit]": "formSubmit"
        },

        handleExistingSession (requirements) {
            // If we have a token, let's see who we are logged in as....
            SessionManager.getLoggedUser((user) => {
                if (isRealmChanged()) {
                    location.href = "#confirmLogin/";
                } else {
                    Configuration.setProperty("loggedUser", user);
                    RESTLoginHelper.setSuccessURL(requirements.tokenId, requirements.successUrl).then(() => {

                        if (gotoUrl.exists()) {
                            window.location.href = gotoUrl.toHref();
                            $("body").empty();
                            return false;
                        }

                        EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, {
                            anonymousMode: false
                        });

                        // Copied from EVENT_LOGIN_REQUEST handler
                        if (Configuration.gotoURL &&
                            _.indexOf(["#", "", "#/", "/#"], Configuration.gotoURL) === -1) {
                            console.log(`Auto redirect to ${Configuration.gotoURL}`);
                            Router.navigate(Configuration.gotoURL, { trigger: true });
                            delete Configuration.gotoURL;
                        } else {
                            Router.navigate("", { trigger: true });
                        }
                    });
                }
            }, () => {
                logout.default();
            });
        },

        autoLogin () {
            var index,
                submitContent = {},
                auth = Configuration.globalData.auth;

            _.each(_.keys(auth.urlParams), function (key) {
                if (key.indexOf("IDToken") > -1) {
                    index = parseInt(key.substring(7), 10) - 1;
                    submitContent[`callback_${index}`] = auth.urlParams[`IDToken${key.substring(7)}`];
                }
            });
            auth.autoLoginAttempts = 1;
            EventManager.sendEvent(Constants.EVENT_LOGIN_REQUEST, submitContent);
        },

        isZeroPageLoginAllowed () {
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

        formSubmit (e) {
            var submitContent,
                expire;

            e.preventDefault();
            // disabled button before login
            $(e.currentTarget).prop("disabled", true);

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

            EventManager.sendEvent(Constants.EVENT_LOGIN_REQUEST, {
                submitContent,
                failureCallback () {
                    // enabled the login button if login failure
                    $(e.currentTarget).prop("disabled", false);
                    // If its not the first stage then render the Login Unavailable view with link back to login screen.
                    if (Configuration.globalData.auth.currentStage > 1) {
                        let fragmentParams = URIUtils.getCurrentFragmentQueryString();
                        if (fragmentParams) {
                            fragmentParams = `&${fragmentParams}`;
                        }
                        // Go to the Login Unavailable view with all the original fragment parameters.
                        routeToLoginUnavailable(fragmentParams);
                    }
                }
            });
        },

        render (args) {
            const addtionalArguments = args ? args[1] : undefined;
            let params = {};
            const auth = Configuration.globalData.auth;

            // TODO: The first undefined argument is the deprecated realm which is defined in the
            // CommonRoutesConfig login route. This needs to be removed as part of AME-11109.
            this.data.args = [undefined, getFragmentParamString()];


            if (args) {
                auth.additional = addtionalArguments;
                auth.urlParams = {};
                params = this.handleParams();

                // If there are IDTokens try to login with the provided credentials
                if (params.IDToken1 && this.isZeroPageLoginAllowed() && !auth.autoLoginAttempts) {
                    this.autoLogin();
                }
            }

            AuthNService.getRequirements().then(_.bind(function (reqs) {

                // Clear out existing session if instructed
                if (reqs.hasOwnProperty("tokenId") && params.arg === "newsession") {
                    logout.default();
                }

                // If simply by asking for the requirements, we end up with a token,
                // then we must have already had a session
                if (reqs.hasOwnProperty("tokenId")) {
                    this.handleExistingSession(reqs);
                } else { // We aren't logged in yet, so render a form...
                    this.renderForm(reqs, params);

                    if (CookieHelper.getCookie("invalidRealm")) {
                        CookieHelper.deleteCookie("invalidRealm");
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidRealm");
                    }
                }
            }, this), _.bind(function (error) {
                if (error) {
                    Messages.addMessage({
                        type: Messages.TYPE_DANGER,
                        message: error.message
                    });
                }

                /**
                 * We havent managed to get a successful responce from the server
                 * This could be due to many reasons, including that the params are incorrect
                 * For example requesting service=thewrongname. So here we use the RESTLoginHelper.filterUrlParams
                 * function to only return the params we which to save. The authIndexType and authIndexValue
                 * would normally only be applied when the user has logged in, so they should not contain invalid values
                 */

                const paramString = URIUtils.getCurrentFragmentQueryString();
                routeToLoginUnavailable(RESTLoginHelper.filterUrlParams(query.parseParameters(paramString)));

            }, this));
        },
        renderForm (reqs, urlParams) {
            var requirements = _.clone(reqs),
                promise = $.Deferred(),
                usernamePasswordStages = ["DataStore1", "AD1", "JDBC1", "LDAP1", "Membership1", "RADIUS1"],
                template,
                self = this;

            this.userNamePasswordStage = _.contains(usernamePasswordStages, reqs.stage);

            requirements.callbacks = [];

            _.each(reqs.callbacks, (element) => {
                let redirectForm;
                let redirectCallback;

                if (element.type === "RedirectCallback") {
                    redirectCallback = _.object(_.map(element.output, (o) => {
                        return [o.name, o.value];
                    }));

                    redirectForm = $(`<form action='${redirectCallback.redirectUrl}' method='POST'></form>`);

                    if (redirectCallback.redirectMethod === "POST") {
                        _.each(redirectCallback.redirectData, (v, k) => {
                            redirectForm.append(
                                `<input type='hidden' name='${k}' value='${v}' aria-hidden='true' />`);
                        });
                        redirectForm.appendTo("body").submit();
                    } else {
                        window.location.replace(redirectCallback.redirectUrl);
                    }
                } else if (element.type === "PollingWaitCallback") {
                    const pollingWaitTimeoutMs = _.find(element.output, { name: "waitTime" }).value;

                    _.delay(() => {
                        this.pollingInProgress = true;

                        if (hasPollingCallback(this.reqs)) {
                            EventManager.sendEvent(Constants.EVENT_LOGIN_REQUEST, { suppressSpinner: true });
                        }
                    }, pollingWaitTimeoutMs);
                }

                requirements.callbacks.push({
                    input: {
                        index: requirements.callbacks.length,
                        name: element.input ? element.input[0].name : null,
                        value: element.input ? element.input[0].value : null
                    },
                    output: element.output,
                    type: element.type
                });
            });

            if (!hasConfirmationCallback(reqs) && !hasPollingCallback(reqs)) {
                requirements.callbacks.push({
                    "input": {
                        index: requirements.callbacks.length,
                        name: "loginButton",
                        value: 0
                    },
                    output: [{
                        name: "options",
                        value: [$.t("common.user.login")]
                    }],
                    type: "ConfirmationCallback"
                });
            }

            this.reqs = reqs;
            this.data.reqs = requirements;

            const pollingInProgress = this.pollingInProgress && hasPollingCallback(reqs);

            // Is there an attempt at autologin happening?
            // if yes then don't render the form until it fails one time
            if (urlParams.IDToken1 && Configuration.globalData.auth.autoLoginAttempts === 1) {
                Configuration.globalData.auth.autoLoginAttempts++;
            } else if (!pollingInProgress) {
                // Attempt to load a stage-specific template to render this form.  If not found, use the generic one.
                template = `templates/openam/authn/${reqs.stage}.html`;
                UIUtils.compileTemplate(template, _.extend({}, Configuration.globalData, this.data))
                    .always(function (compiledTemplate) {
                        // A rendered template will be a string; an error will be an object
                        self.template = typeof compiledTemplate === "string" ? template : self.genericTemplate;

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
        prefillLoginData () {
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

        handleParams () {
            // TODO: Remove support for fragment params and change to URIUtils.getCurrentQueryString()
            // as currently we are checking both the framgent and query with framgent over-riding.
            const paramString = URIUtils.getCurrentCompositeQueryString();
            const params = query.parseParameters(paramString);
            // Rest does not accept the params listed in the array below as is
            // they must be transformed into the "authIndexType" and "authIndexValue" params
            // but if composite_advice set that must be adhered to
            if (!params.authIndexType || params.authIndexType !== "composite_advice") {
                _.each(["authlevel", "module", "service", "user", "resource"], function (param) {
                    if (params[param]) {
                        params.authIndexType = ((param === "authlevel") ? "level" : param);
                        params.authIndexValue = params[param];
                        //*** Note special case for authLevel
                        Configuration.globalData.auth.additional += `&authIndexType=${
                            ((param === "authlevel") ? "level" : param)
                        }&authIndexValue=${params[param]}`;
                    }
                });
            }
            // Special case for SSORedirect and SSOPOST
            if (hasSsoRedirectOrPost(params.goto)) {
                params.goto = `/${Constants.context}${params.goto}`;
                Configuration.globalData.auth.additional.replace("&goto=", `&goto=/${Constants.context}`);
            }

            Configuration.globalData.auth.urlParams = params;
            return params;
        }
    });

    Handlebars.registerHelper("callbackRender", function () {
        var result = "", self = this, prompt = "", options, defaultOption, btnClass = "", renderContext;

        _.find(this.output, function (obj) {
            if (obj.name === "prompt" && obj.value !== undefined && obj.value.length) {
                prompt = obj.value.replace(/:$/, "");
            }
        });

        function generateId (name) {
            return _.isEmpty(name) ? "" : _.camelCase(name);
        }

        renderContext = {
            id: generateId(this.input.name),
            index: this.input.index,
            value: this.input.value,
            prompt
        };

        function renderPartial (name, context) {
            return _.find(Handlebars.partials, (code, templateName) => {
                return templateName.indexOf(`login/_${name}`) !== -1;
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
                            btnClass,
                            key,
                            option
                        });
                    });
                }
                break;
            case "ChoiceCallback":
                options = _.find(this.output, { name: "choices" });

                if (options && options.value !== undefined) {
                    result += renderPartial("Choice", {
                        values: _.map(options.value, function (option, key) {
                            return {
                                active: self.input.value === key,
                                key,
                                value: option
                            };
                        })
                    });
                }
                break;
            case "HiddenValueCallback": result += renderPartial("HiddenValue"); break;
            case "RedirectCallback": result += renderPartial("Redirect"); break;
            case "PollingWaitCallback": result += renderPartial("PollingWait"); break;
            default: result += renderPartial("Default"); break;
        }

        return new Handlebars.SafeString(result);
    });

    Handlebars.registerHelper("intendedRealmParameter", () => {
        const sessionInfoIntendedRealm = store.default.getState().server.realm;
        return sessionInfoIntendedRealm ? `&realm=${sessionInfoIntendedRealm}` : "";
    });

    Handlebars.registerHelper("gotoParameter", () => {
        return gotoUrl.exists() ? `&goto=${gotoUrl.get()}` : "";
    });

    return new LoginView();
});
