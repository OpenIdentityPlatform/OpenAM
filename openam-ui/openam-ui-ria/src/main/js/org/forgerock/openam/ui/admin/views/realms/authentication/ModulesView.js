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

/*global define*/
define("org/forgerock/openam/ui/admin/views/realms/authentication/ModulesView", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/delegates/SMSRealmDelegate",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/util/UIUtils",

    // jquery dependencies
    "selectize"
], function ($, AbstractView, BootstrapDialog, Configuration, EventManager, Router, Constants, SMSRealmDelegate, Form,
             FormHelper, MessageManager, UIUtils) {
    var ModulesView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/ModulesTemplate.html",
        events: {
            "click #addModule": "addModule",
            "change input[data-module-name]": "moduleSelected",
            "click button[data-module-name]:not([data-active])": "deleteModule",
            "click #deleteModules": "deleteModules"
        },
        data: {},
        addModule: function (e) {
            e.preventDefault();

            var self = this;

            SMSRealmDelegate.authentication.modules.types.all(this.data.realmPath).done(function (data) {
                self.data.moduleTypes = data.result;
                UIUtils.fillTemplateWithData("templates/admin/views/realms/authentication/modules/AddModuleTemplate.html", self.data, function (html) {
                    BootstrapDialog.show({
                        title: $.t("console.authentication.modules.addModuleDialogTitle"),
                        message: $(html),
                        buttons: [{
                            id: "nextButton",
                            label: $.t("common.form.create"),
                            cssClass: "btn-primary",
                            action: function (dialog) {
                                if (self.addModuleDialogValidation(dialog)) {
                                    var moduleName = dialog.getModalBody().find("#newModuleName").val(),
                                        moduleType = dialog.getModalBody().find("#newModuleType").val();
                                    SMSRealmDelegate.authentication.modules.exists(self.data.realmPath, moduleName).done(function (result) {
                                        if (!result) {
                                            SMSRealmDelegate.authentication.modules.create(self.data.realmPath, {
                                                _id: moduleName
                                            }, moduleType).done(function () {
                                                dialog.close();
                                                Router.routeTo(Router.configuration.routes.realmsAuthenticationModuleEdit, {
                                                    args: [encodeURIComponent(self.data.realmPath), encodeURIComponent(moduleName), encodeURIComponent(moduleType)],
                                                    trigger: true
                                                });
                                            }).fail(function (error) {
                                                //TODO
                                            });
                                        } else {
                                            MessageManager.messages.addMessage({
                                                message: $.t("console.authentication.modules.addModuleDialogError"),
                                                type: "error"
                                            });
                                        }
                                    });
                                }
                            }
                        }, {
                            label: $.t("common.form.cancel"),
                            action: function (dialog) {
                                dialog.close();
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
            var nameValid = dialog.$modalBody.find("#newModuleName").val().length > 0,
                typeValid = dialog.$modalBody.find("#newModuleType")[0].selectize.getValue().length > 0;
            return (nameValid && typeValid);
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
        deleteModule: function (event) {
            var self = this,
                moduleName = $(event.currentTarget).attr("data-module-name"),
                moduleType = $(event.currentTarget).attr("data-module-type");

            SMSRealmDelegate.authentication.modules.remove(self.data.realmPath, moduleName, moduleType).done(function () {
                $(event.currentTarget).parents("tr").remove();
            }).fail(function () {
                // TODO: Add failure condition
            });
        },
        deleteModules: function () {
            var self = this,
                promises = self.$el.find("input[type=checkbox]:checked").toArray().map(function (element) {
                    var dataset = $(element).data(),
                        name = dataset.moduleName,
                        type = dataset.moduleType;
                    return SMSRealmDelegate.authentication.modules.remove(self.data.realmPath, name, type);
                });

            $.when(promises).then(function () {
                self.render(self.data.args);
            }).fail(function () {
                // TODO: Add failure condition
            });
        },
        render: function (args, callback) {
            var self = this;

            this.data.args = args;
            this.data.realmPath = args[0];

            SMSRealmDelegate.authentication.modules.all(this.data.realmPath).done(function (data) {
                self.data.formData = data.result;
                self.$el.find("[data-toggle='tooltip']").tooltip();
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
