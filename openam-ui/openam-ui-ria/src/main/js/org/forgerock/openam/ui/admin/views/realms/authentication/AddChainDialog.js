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

define("org/forgerock/openam/ui/admin/views/realms/authentication/AddChainDialog", [
    "jquery",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "handlebars",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/delegates/SMSRealmDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, BootstrapDialog, Handlebars, Messages, Router, SMSRealmDelegate, UIUtils) {
    var buttons = [{
        label: $.t("common.form.create"),
        cssClass: "btn-primary",
        action: function (dialog) {
            var chainName = dialog.getModalBody().find("#newName").val().trim();

            if (dialog.options.isNameValid(chainName)) {
                SMSRealmDelegate.authentication.chains.create(
                    dialog.options.realmPath,
                    { _id: chainName }
                ).then(function () {
                    dialog.close();
                    Router.routeTo(Router.configuration.routes.realmsAuthenticationChainEdit, {
                        args: [
                            encodeURIComponent(dialog.options.realmPath),
                            encodeURIComponent(chainName)
                        ],
                        trigger: true
                    });
                }, function (event) {
                    Messages.addMessage({
                        type: Messages.TYPE_DANGER,
                        response: event
                    });
                });
            } else {
                dialog.getModalBody().find("#alertContainer").html(Handlebars.compile(
                    "{{> alerts/_Alert type='warning' text='console.authentication.chains.duplicateChain'}}"
                ));
            }
        }
    }, {
        label: $.t("common.form.cancel"),
        action: function (dialog) {
            dialog.close();
        }
    }];

    return function (realmPath, isNameValid) {
        UIUtils.fillTemplateWithData("templates/admin/views/realms/authentication/chains/AddChainTemplate.html",
        this.data,
        function (html) {
            BootstrapDialog.show({
                title: $.t("console.authentication.chains.createNewChain"),
                message: $(html),
                buttons: buttons,
                isNameValid: isNameValid || function () { return true; },
                realmPath: realmPath
            });
        });
    };
});
