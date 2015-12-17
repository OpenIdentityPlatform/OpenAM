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


define("org/forgerock/openam/ui/dashboard/views/DeviceManagementView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/dashboard/delegates/DeviceManagementDelegate",
    "org/forgerock/openam/ui/dashboard/views/DeviceDetailsDialog",
    "org/forgerock/openam/ui/dashboard/views/DevicesSettingsDialog"
], function ($, _, Messages, AbstractView, DeviceManagementDelegate, DeviceDetailsDialog,
             DevicesSettingsDialog) {
    var DeviceManagementView = AbstractView.extend({
        template: "templates/openam/dashboard/DeviceManagementTemplate.html",
        noBaseTemplate: true,
        element: "#deviceManagementSection",
        events: {
            "click .delete-device-btn":  "deleteDevice",
            "click .recovery-codes-btn": "showDeviceDetails",
            "click .device-details": "showDeviceDetails",
            "click .devices-settings-btn" : "showDevicesSettings"
        },

        deleteDevice: function (event) {
            event.preventDefault();

            var target = $(event.currentTarget),
                card = $(target).closest("div[data-device-uuid]"),
                uuid = card.attr("data-device-uuid");

            DeviceManagementDelegate.deleteDevice(uuid).then(function () {
                card.parent().remove();
            }, function (response) {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response: response
                });
            });
        },

        showDeviceDetails: function (event) {
            event.preventDefault();

            var uuid = $(event.currentTarget).closest("div[data-device-uuid]").attr("data-device-uuid"),
                device = _.find(this.data.devices, { uuid: uuid });

            DeviceDetailsDialog(uuid, device);
        },

        showDevicesSettings: function (event) {
            event.preventDefault();
            DevicesSettingsDialog();
        },

        render: function (callback) {
            var self = this;
            DeviceManagementDelegate.getDevices().then(function (devicesData) {
                self.data.devices = devicesData.result;
                self.parentRender(function () {
                    if (callback) {
                        callback();
                    }
                });
            }, function (response) {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response: response
                });
            });
        }
    });

    return new DeviceManagementView();
});
