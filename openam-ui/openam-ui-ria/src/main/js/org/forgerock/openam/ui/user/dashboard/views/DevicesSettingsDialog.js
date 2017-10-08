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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/user/dashboard/services/DeviceManagementService"
], ($, _, BootstrapDialog, Messages, UIUtils, DeviceManagementService) => {
    function closeDialog (dialog) {
        dialog.close();
    }

    return function () {
        const template = "templates/user/dashboard/DevicesSettingsDialogTemplate.html";
        let authSkip = DeviceManagementService.checkDevicesOathSkippable();

        BootstrapDialog.show({
            title: $.t("openam.authDevices.devicesSettingDialog.title"),
            cssClass: "devices-settings",
            message: $("<div></div>"),
            buttons: [{
                label: $.t("common.form.cancel"),
                action: closeDialog
            }, {
                label: $.t("common.form.save"),
                cssClass: "btn-primary",
                action (dialog) {
                    authSkip = !dialog.$modalBody.find("#oathStatus").is(":checked");
                    DeviceManagementService.setDevicesOathSkippable(authSkip).then(() => {
                        dialog.close();
                    }, (response) => {
                        Messages.addMessage({
                            type: Messages.TYPE_DANGER,
                            response
                        });
                    });
                }
            }
            ],
            onshown (dialog) {
                $.when(authSkip).then((skip) => {
                    return UIUtils.compileTemplate(template, { authNeeded: !skip });
                }, (response) => {
                    Messages.addMessage({
                        type: Messages.TYPE_DANGER,
                        response
                    });
                }).then((tpl) => {
                    dialog.$modalBody.append(tpl);
                });
            }
        });
    };
});
