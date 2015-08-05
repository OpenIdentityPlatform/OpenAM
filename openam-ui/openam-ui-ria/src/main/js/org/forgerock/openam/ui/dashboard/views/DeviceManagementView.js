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
    "bootstrap-dialog",
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

            var uuid = $(event.currentTarget).closest("div[data-device-uuid]").attr("data-device-uuid"),
                device = _.findWhere(this.data.devices, {uuid: uuid});

            UIUtils.fillTemplateWithData("templates/openam/dashboard/EditDeviceDialogTemplate.html", device, function(html) {

                BootstrapDialog.show({
                    type: BootstrapDialog.TYPE_DEFAULT,
                    title: device.deviceName,
                    message: $(html),
                    cssClass: "device-details",
                    closable: false,
                    buttons: [{
                        label: $.t("common.form.save"),
                        action: function (dialog) {
                            //TODO: add save functionality
                            dialog.close();
                        }
                    }, {
                        label: $.t("common.form.close"),
                        cssClass: "btn-primary",
                        action: function (dialog) {
                            dialog.close();
                        }
                    }],
                    onshown: function(dialog){
                        dialog.$modalBody.find(".recovery-codes-download").click(function(){
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

        render: function () {
            var self = this;

            DeviceManagementDelegate.getDevices().done(function (data) {
                // MOCK DATA
                data.result = [{
                    "sharedSecret": "6A5C27FAACB0890F",
                    "deviceName": "OATH Device",
                    "lastLogin": 1433156184430,
                    "counter": 1,
                    "checksumDigit": false,
                    "truncationOffset": 0,
                    "recoveryCodes": [
                        "4b344d30-9511-4c4e-b562-9445420422c5",
                        "afc644cd-1b8c-40c8-a9a5-0ff86b702bc1",
                        "92021597-d046-4744-9ddb-77878e5732e3",
                        "e6194a04-1029-49df-8cd8-f93d029ddf64",
                        "d3da46df-2137-4587-8ca8-b262e905a45f",
                        "a3bc9cee-41de-4793-a1c6-1f8f33308987",
                        "beecc0b6-a0ff-44ad-b82f-a376e63b4f79",
                        "4094d628-8d97-4a69-8ef4-db81a9c24f32",
                        "cd475e90-459c-4e84-9536-fcdc8ff2fabd",
                        "03e86514-e941-4c47-a597-398140249d6d"
                    ],
                    "uuid": "24553be4-a356-47a8-9dc4-a973caa27b85"
                }];
                self.data.devices = data.result;
                // TODO: get "skip" value from backend and preselect it if necessary
                self.parentRender();
            })
            .fail(function (error) {
                // TODO: add failure condition
            });
        }
    });

    return new DeviceManagementView();
});
