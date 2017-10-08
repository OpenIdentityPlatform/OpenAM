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
 * Copyright 2015-2016 ForgeRock AS.
 */


define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/user/dashboard/services/TrustedDevicesService"
], function ($, _, AbstractView, TrustedDevicesService) {
    var TrustedDevices = AbstractView.extend({
        template: "templates/user/dashboard/TrustedDevicesTemplate.html",
        noBaseTemplate: true,
        element: "#myTrustedDevicesSection",
        events: {
            "click  a.deleteDevice" : "deleteDevice"
        },

        render () {
            var self = this;

            TrustedDevicesService.getTrustedDevices().then(function (data) {
                self.data.devices = data.result;
                self.parentRender(function () {
                    self.$el.find("[data-toggle=\"tooltip\"]").tooltip();
                });
            });
        },

        deleteDevice (event) {
            event.preventDefault();
            var self = this;

            TrustedDevicesService.deleteTrustedDevice(event.currentTarget.id).then(function () {
                console.log("Deleted trusted device");
                self.render();
            }, function () {
                console.error("Failed to delete trusted device");
            });
        }
    });

    return new TrustedDevices();
});
