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

/*global, define*/
define('org/forgerock/openam/ui/dashboard/DeviceManagementView', [
    'jquery',
    'underscore',
    'org/forgerock/commons/ui/common/main/AbstractView',
    'bootstrap-dialog',
    'org/forgerock/openam/ui/dashboard/DeviceManagementDelegate'
], function ($, _, AbstractView, BootstrapDialog, DeviceManagementDelegate) {
    var DeviceManagementView = AbstractView.extend({
        template: 'templates/openam/DeviceManagementTemplate.html',
        noBaseTemplate: true,
        element: '#deviceManagementSection',
        events: {
            'click #delete': 'deleteDevice',
            'click #recoveryCodes': 'showRecoveryCodes'
        },
        deleteDevice: function (event) {
            event.preventDefault();

            var target = $(event.currentTarget),
                card = $(target).closest('div[data-device-uuid]'),
                uuid = card.attr('data-device-uuid');

            DeviceManagementDelegate.deleteDevice(uuid).done(function (data) {
                card.parent().remove();
            });
        },
        showRecoveryCodes: function (event) {
            event.preventDefault();

            var uuid = $(event.currentTarget).closest('div[data-device-uuid]').attr('data-device-uuid'),
                device = _.findWhere(this.data.devices, {uuid: uuid});

            BootstrapDialog.show({
                type: BootstrapDialog.TYPE_DEFAULT,
                title: $.t('openam.deviceManagement.recoveryCodes.title'),
                message: device.recoveryCodes.join('\n'),
                buttons: [{
                    label: $.t('openam.deviceManagement.recoveryCodes.download'),
                    icon: 'fa fa-download',
                    action: function (dialog) {
                        location.href = 'data:text/plain,' + encodeURIComponent(device.recoveryCodes.join('\r\n'));
                    }
                }, {
                    label: $.t('openam.deviceManagement.recoveryCodes.close'),
                    cssClass: 'btn-primary',
                    action: function (dialog) {
                        dialog.close();
                    }
                }]
            });
        },
        render: function () {
            var self = this;

            DeviceManagementDelegate.getDevices().done(function (data) {
                self.data.devices = data.result;
                self.parentRender();
            })
            .fail(function (error) {
                // TODO:
            });
        }
    });

    return new DeviceManagementView();
});
