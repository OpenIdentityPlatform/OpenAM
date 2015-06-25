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
define('org/forgerock/openam/ui/admin/views/realms/authentication/SettingsView', [
    'org/forgerock/commons/ui/common/main/AbstractView',
    'org/forgerock/openam/ui/admin/models/Form',
    'org/forgerock/openam/ui/admin/utils/FormHelper',
    'org/forgerock/openam/ui/admin/delegates/SMSRealmDelegate'
], function(AbstractView, Form, FormHelper, SMSRealmDelegate) {
    var SettingsView = AbstractView.extend({
        template: 'templates/admin/views/realms/authentication/SettingsTemplate.html',
        events: {
            'click #saveChanges': 'save'
        },
        render: function(args, callback) {
            var self = this;

            this.data.realmLocation = args[0];

            this.parentRender( function () {
                SMSRealmDelegate.authentication.get(this.data.realmLocation).done(function(data) {
                    self.data.form = new Form(self.$el.find('#tabpanel').get(0), {
                        type: 'object',
                        properties: {
                            adminAuthModule: data.schema.properties.core.properties.adminAuthModule,
                            loginSuccessUrl: data.schema.properties.postauthprocess.properties.loginSuccessUrl,
                            orgConfig: data.schema.properties.core.properties.orgConfig
                        }
                    }, data.values);
                });

                if(callback) {
                    callback();
                }
            });
        },
        save: function(event) {
            var promise = SMSRealmDelegate.authentication.save(this.data.realmLocation, this.data.form.data());

            FormHelper.bindSavePromiseToElement(promise, event.target);
        }
    });

    return SettingsView;
});
