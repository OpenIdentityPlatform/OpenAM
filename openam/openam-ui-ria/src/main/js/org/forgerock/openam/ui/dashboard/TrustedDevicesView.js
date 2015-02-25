/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define, $, form2js, _ */

define("org/forgerock/openam/ui/dashboard/TrustedDevicesView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/dashboard/TrustedDevicesDelegate"
], function(AbstractView, TrustedDevicesDelegate) {
    
    var TrustedDevices = AbstractView.extend({
        template: "templates/openam/TrustedDevicesTemplate.html",
        noBaseTemplate: true,
        element: '#myTrustedDevices',
        events: { 'click  a.deleteDevice' : 'deleteDevice' },
        render: function() {

            var self = this;
            TrustedDevicesDelegate.getTrustedDevices()
                .then(function (data) {
                    self.data.devices = data.result;
                    self.parentRender();
                });
        },

        deleteDevice: function(e) {
            e.preventDefault();
            var self = this;
            TrustedDevicesDelegate.deleteTrustedDevice(e.currentTarget.id)
                .then(function() {
                    console.log('Deleted trusted device');
                    self.render();
                }, function() {
                    console.error("Failed to delete trusted device");
                });
        }
    });

    return new TrustedDevices();
});


