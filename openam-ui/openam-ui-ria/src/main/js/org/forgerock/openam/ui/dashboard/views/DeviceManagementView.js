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

define("org/forgerock/openam/ui/dashboard/views/DeviceManagementView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/openam/ui/dashboard/delegates/DeviceManagementDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, _, AbstractView, BootstrapDialog, DeviceManagementDelegate, UIUtils) {
    var DeviceManagementView = AbstractView.extend({
        template: "templates/openam/dashboard/DeviceManagementTemplate.html",
        noBaseTemplate: true,
        element: "#deviceManagementSection",
        events: {
            "click .delete-device-btn":  "deleteDevice",
            "click .recovery-codes-btn": "showDeviceDetails",
            "click .device-details": "showDeviceDetails"
        },

        deleteDevice: function (event) {
            event.preventDefault();

            var target = $(event.currentTarget),
                card = $(target).closest("div[data-device-uuid]"),
                uuid = card.attr("data-device-uuid");

            DeviceManagementDelegate.deleteDevice(uuid).done(function (data) {
                card.parent().remove();
            });
        },

        showDeviceDetails: function (event) {
            event.preventDefault();

            var self = this,
                statusDevice,
                uuid = $(event.currentTarget).closest("div[data-device-uuid]").attr("data-device-uuid"),
                device = _.find(this.data.devices, {uuid: uuid});

            UIUtils.fillTemplateWithData("templates/openam/dashboard/EditDeviceDialogTemplate.html", device, function(html) {
                BootstrapDialog.show({
                    title: device.deviceName,
                    message: $(html),
                    cssClass: "device-details",
                    closable: false,
                    buttons: [{
                        label: $.t("common.form.save"),
                        action: function (dialog) {
                            statusDevice = dialog.$modalBody.find("[name=\"deviceSkip\"]").is(":checked");
                            DeviceManagementDelegate.setDeviceSkippable(statusDevice).done(function (data) {
                                self.render();
                                dialog.close();
                            });
                        }
                    }, {
                        label: $.t("common.form.close"),
                        cssClass: "btn-primary",
                        action: function (dialog) {
                            dialog.close();
                        }
                    }],
                    onshown: function(dialog) {
                        dialog.$modalBody.find(".recovery-codes-download").click(function() {
                            location.href = "data:text/plain," + encodeURIComponent(device.recoveryCodes.join("\r\n"));
                        });

                        dialog.$modalBody.find("[data-toggle=\"popover\"]").popover({
                            content: $.t("openam.deviceManagement.deviceDetailsDialog.help"),
                            placement: "bottom",
                            title: $.t("openam.deviceManagement.deviceDetailsDialog.skip"),
                            trigger: "focus"
                        });
                    }
                });

            });
        },

        render: function (callback) {
            var self = this;
            DeviceManagementDelegate.getDevices().done(function (devicesData) {
                self.data.devices = devicesData.result;
                self.parentRender(function() {
                    if (callback) {
                        callback();
                    }
                });
            }).fail(function (error) {
                // TODO: add failure condition
            });
        }
    });

    return new DeviceManagementView();
});
