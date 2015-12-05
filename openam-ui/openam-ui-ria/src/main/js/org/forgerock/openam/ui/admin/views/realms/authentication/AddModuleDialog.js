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
 * Copyright 2015 ForgeRock AS.
 */

define("org/forgerock/openam/ui/admin/views/realms/authentication/AddModuleDialog", [
    "jquery",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "handlebars",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/delegates/SMSRealmDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, BootstrapDialog, Handlebars, Messages, Router, SMSRealmDelegate, UIUtils) {
    function addModuleDialogValidation (dialog) {
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
    }
    function closeDialog (dialog) {
        dialog.close();
    }
    function validateAndCreate (dialog) {
        if (addModuleDialogValidation(dialog)) {
            var moduleName = dialog.getModalBody().find("#newModuleName").val(),
                moduleType = dialog.getModalBody().find("#newModuleType").val(),
                modulesDelegate = SMSRealmDelegate.authentication.modules;

            modulesDelegate.exists(dialog.options.data.realmPath, moduleName).then(function (result) {
                var authenticationModules = modulesDelegate;
                if (result) {
                    Messages.addMessage({
                        type: Messages.TYPE_DANGER,
                        message: $.t("console.authentication.modules.addModuleDialogError")
                    });
                } else {
                    authenticationModules.create(dialog.options.data.realmPath, { _id: moduleName }, moduleType)
                    .then(function () {
                        dialog.close();
                        Router.routeTo(Router.configuration.routes.realmsAuthenticationModuleEdit, {
                            args: [
                                encodeURIComponent(dialog.options.data.realmPath),
                                encodeURIComponent(moduleType),
                                encodeURIComponent(moduleName)],
                            trigger: true
                        });
                    }, function (response) {
                        Messages.addMessage({
                            type: Messages.TYPE_DANGER,
                            response: response
                        });
                    });
                }
            }, function (response) {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response: response
                });
            });
        }
    }

    return function (realmPath, types) {
        UIUtils.fillTemplateWithData("templates/admin/views/realms/authentication/modules/AddModuleTemplate.html",
            {
                types: types
            },
        function (html) {
            BootstrapDialog.show({
                title: $.t("console.authentication.modules.addModuleDialogTitle"),
                message: $(html),
                buttons: [{
                    label: $.t("common.form.cancel"),
                    action: closeDialog
                }, {
                    id: "createButton",
                    label: $.t("common.form.create"),
                    cssClass: "btn-primary",
                    action: validateAndCreate
                }],
                data: {
                    realmPath: realmPath
                },
                onshow: function (dialog) {
                    dialog.getButton("createButton").disable();
                    dialog.$modalBody.find("#newModuleType").selectize();

                    dialog.$modalBody.on("change keyup", "#newModuleName, #newModuleType", function () {
                        if (addModuleDialogValidation(dialog)) {
                            dialog.getButton("createButton").enable();
                        } else {
                            dialog.getButton("createButton").disable();
                        }
                    });
                }
            });
        });
    };
});
