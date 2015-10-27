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
 * Copyright 2015 ForgeRock AS.
 */

define("org/forgerock/openam/ui/admin/views/realms/authentication/ModulesView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "handlebars",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/delegates/SMSRealmDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",

    // jquery dependencies
    "selectize"
], function ($, _, AbstractView, BootstrapDialog, Configuration, Constants, EventManager, Form, FormHelper, Handlebars,
             MessageManager, Router, SMSRealmDelegate, UIUtils) {
    var ModulesView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/ModulesTemplate.html",
        events: {
            "click #addModule": "addModule",
            "change input.select-module": "moduleSelected",
            "click button.delete-module-button": "deleteModule",
            "click #deleteModules": "deleteModules",
            "click .check-before-edit": "editModule"
        },
        partials: [
            "partials/alerts/_Alert.html"
        ],
        data: {},
        addModule: function (e) {
            e.preventDefault();

            var self = this;

            SMSRealmDelegate.authentication.modules.types.all(this.data.realmPath).done(function (data) {
                self.data.moduleTypes = data.result;
                UIUtils.fillTemplateWithData(
                    "templates/admin/views/realms/authentication/modules/AddModuleTemplate.html", self.data,
                    function (html) {
                        BootstrapDialog.show({
                            title: $.t("console.authentication.modules.addModuleDialogTitle"),
                            message: $(html),
                            buttons: [{
                                label: $.t("common.form.cancel"),
                                action: function (dialog) {
                                    dialog.close();
                                }
                            }, {
                                id: "nextButton",
                                label: $.t("common.form.create"),
                                cssClass: "btn-primary",
                                action: function (dialog) {
                                    if (self.addModuleDialogValidation(dialog)) {
                                        var moduleName = dialog.getModalBody().find("#newModuleName").val(),
                                            moduleType = dialog.getModalBody().find("#newModuleType").val();

                                        SMSRealmDelegate.authentication.modules.exists(self.data.realmPath, moduleName)
                                            .done(function (result) {
                                                var authenticationModules = SMSRealmDelegate.authentication.modules;
                                                if (!result) {
                                                    authenticationModules.create(self.data.realmPath, {
                                                        _id: moduleName
                                                    }, moduleType).done(function () {
                                                        dialog.close();
                                                        Router.routeTo(
                                                            Router.configuration.routes.realmsAuthenticationModuleEdit,
                                                            {
                                                                args: [
                                                                    encodeURIComponent(self.data.realmPath),
                                                                    encodeURIComponent(moduleName),
                                                                    encodeURIComponent(moduleType)],
                                                                trigger: true
                                                            });
                                                    }).fail(function (e) {
                                                        //TODO
                                                        console.error(e);
                                                    });
                                                } else {
                                                    MessageManager.messages.addMessage({
                                                        message:
                                                            $.t("console.authentication.modules.addModuleDialogError"),
                                                        type: "error"
                                                    });
                                                }
                                            });
                                    }
                                }
                            }],
                            onshow: function (dialog) {
                                dialog.getButton("nextButton").disable();
                                dialog.$modalBody.find("#newModuleType").selectize();
                                self.enableOrDisableNextButton(dialog);
                            }
                        });
                    });
            });
        },
        addModuleDialogValidation: function (dialog) {
            var valid = true,
                alert = "",
                nameValid,
                typeValid;

            if (dialog.$modalBody.find("#newModuleName").val().indexOf(" ") !== -1) {
                valid = false;
                alert = Handlebars.compile("{{> alerts/_Alert type='warning' " +
                    "text='console.authentication.modules.moduleNameValidationError'}}");
            }

            dialog.$modalBody.find("#alertContainer").html(alert);
            nameValid = dialog.$modalBody.find("#newModuleName").val().length > 0;
            typeValid = dialog.$modalBody.find("#newModuleType")[0].selectize.getValue().length > 0;
            return (nameValid && typeValid && valid);
        },
        enableOrDisableNextButton: function (dialog) {
            var self = this;

            dialog.$modalBody.on("change keyup", "#newModuleName, #newModuleType", function () {
                if (self.addModuleDialogValidation(dialog)) {
                    dialog.getButton("nextButton").enable();
                } else {
                    dialog.getButton("nextButton").disable();
                }
            });
        },
        moduleSelected: function (event) {
            var hasModuleSelected = this.$el.find("input[type=checkbox]").is(":checked"),
                row = $(event.currentTarget).closest("tr"),
                checked = $(event.currentTarget).is(":checked");

            this.$el.find("#deleteModules").prop("disabled", !hasModuleSelected);
            if (checked) {
                row.addClass("selected");
            } else {
                row.removeClass("selected");
            }
        },
        editModule: function (event) {
            event.preventDefault();
            var data = $(event.currentTarget).closest("tr").data();

            BootstrapDialog.show({
                title: $.t("console.authentication.modules.inUse.title"),
                message: $.t("console.authentication.modules.inUse.message",
                    {
                        usedChains: data.moduleChains, moduleName: data.moduleName
                    }),
                buttons: [{
                    label: $.t("common.form.cancel"),
                    cssClass: "btn-default",
                    action: function (dialog) {
                        dialog.close();
                    }
                }, {
                    label: $.t("common.form.yes"),
                    cssClass: "btn-primary",
                    action: function (dialog) {
                        Router.setUrl(event.currentTarget.href);
                        dialog.close();
                    }
                }]
            });
        },

        deleteModule: function (event) {
            var self = this,
                data = $(event.currentTarget).closest("tr").data();

            SMSRealmDelegate.authentication.modules.remove(
                self.data.realmPath,
                data.moduleName,
                data.moduleType
            ).done(function () {
                self.render(self.data.args);
            }).fail(function (e) {
                // TODO: Add failure condition
                console.error(e);
            });

        },
        deleteModules: function () {
            var self = this,
                promises = self.$el.find("input[type=checkbox]:checked").closest("tr").toArray().map(
                    function (element) {
                        var dataset = $(element).data(),
                            name = dataset.moduleName,
                            type = dataset.moduleType;

                        return SMSRealmDelegate.authentication.modules.remove(self.data.realmPath, name, type);
                    });

            $.when(promises).then(function () {
                self.render(self.data.args);
            }).fail(function (e) {
                // TODO: Add failure condition
                console.error(e);
            });
        },
        render: function (args, callback) {
            var self = this,
                chainsPromise,
                modulesPromise;

            this.data.args = args;
            this.data.realmPath = args[0];

            chainsPromise = SMSRealmDelegate.authentication.chains.all(this.data.realmPath);
            modulesPromise = SMSRealmDelegate.authentication.modules.all(this.data.realmPath);

            $.when(chainsPromise, modulesPromise).then(function (chainData, modulesData) {

                _.each(modulesData[0].result, function (module) {
                    _.each(chainData.values.result, function (chain) {
                        _.each(chain.authChainConfiguration, function (link) {
                            if (link.module === module._id) {
                                module.chains = module.chains || [];
                                module.chains.push(chain._id);
                            }
                        });
                    });
                    module.chains = _.uniq(module.chains);
                });

                self.data.formData = modulesData[0].result;

                self.parentRender(function () {
                    if (callback) {
                        callback();
                    }
                });
            });
        },
        save: function (event) {
            var promise = SMSRealmDelegate.authentication.update(this.data.form.data());

            FormHelper.bindSavePromiseToElement(promise, event.currentTarget);
        }
    });

    return ModulesView;
});
