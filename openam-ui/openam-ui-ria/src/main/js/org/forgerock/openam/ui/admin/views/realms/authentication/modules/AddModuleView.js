/*
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
 * Copyright 2016 ForgeRock AS.
 */
define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/admin/services/realm/AuthenticationService",
    "org/forgerock/openam/ui/common/components/SelectComponent"
], function ($, _, AbstractView, Router, Messages, AuthenticationService, SelectComponent) {

    SelectComponent = SelectComponent.default;

    function validateModuleProps () {
        var moduleName = this.$el.find("#newModuleName").val(),
            moduleType = this.moduleType,
            isValid;

        if (moduleName.indexOf(" ") !== -1) {
            moduleName = false;
            Messages.addMessage({
                type: Messages.TYPE_DANGER,
                message: $.t("console.authentication.modules.moduleNameValidationError")
            });
        }
        isValid = moduleName && moduleType;
        this.$el.find("[data-save]").attr("disabled", !isValid);
    }


    return AbstractView.extend({
        template: "templates/admin/views/realms/authentication/modules/AddModuleTemplate.html",
        events: {
            "change [data-module-name]": "onValidateModuleProps",
            "keyup  [data-module-name]": "onValidateModuleProps",
            "change [data-module-type]": "onValidateModuleProps",
            "click [data-save]"        : "save"
        },
        render (args, callback) {
            var self = this;
            this.data.realmPath = args[0];

            AuthenticationService.authentication.modules.types.all(this.data.realmPath).then(function (modulesData) {
                self.parentRender(function () {
                    const selectComponent = new SelectComponent({
                        options: modulesData.result,
                        onChange: (option) => {
                            self.moduleType = option._id;
                            self.onValidateModuleProps();
                        },
                        searchFields: ["name"],
                        labelField: "name",
                        placeholderText: $.t("console.authentication.modules.selectModuleType")
                    });
                    self.$el.find("[data-module-type]").append(selectComponent.render().el);
                    self.$el.find("[autofocus]").focus();
                    if (callback) {
                        callback();
                    }
                });
            });
        },
        save () {
            var self = this,
                moduleName = self.$el.find("#newModuleName").val(),
                moduleType = this.moduleType,
                modulesService = AuthenticationService.authentication.modules;

            modulesService.exists(self.data.realmPath, moduleName).then(function (result) {
                var authenticationModules = modulesService;
                if (result) {
                    Messages.addMessage({
                        type: Messages.TYPE_DANGER,
                        message: $.t("console.authentication.modules.addModuleError")
                    });
                } else {
                    authenticationModules.create(self.data.realmPath, { _id: moduleName }, moduleType)
                    .then(function () {
                        Router.routeTo(Router.configuration.routes.realmsAuthenticationModuleEdit, {
                            args: _.map([self.data.realmPath, moduleType, moduleName], encodeURIComponent),
                            trigger: true
                        });
                    }, function (response) {
                        Messages.addMessage({
                            type: Messages.TYPE_DANGER,
                            response
                        });
                    });
                }
            }, function (response) {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
            });
        },
        onValidateModuleProps () {
            validateModuleProps.call(this);
        }
    });
});
