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

/*global define, $, _*/
define("org/forgerock/openam/ui/admin/views/console/realms/Authentication", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/models/FormCollection",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/delegates/SMSDelegate"
], function(AbstractView, BootstrapDialog, Configuration, Constants, EventManager, Form, FormCollection, FormHelper, Router, SMSDelegate) {
    var Authentication = AbstractView.extend({
        template: "templates/admin/views/console/realms/AuthenticationTemplate.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        events: {
            'click #addModule':   'addModule',
            'click #addChain':    'addChain',
            'click #saveChanges': 'save'
        },
        addChain: function(e) {
            e.preventDefault();
            // This is mock code, please swap out
            var href = $(e.currentTarget).attr('href'),
                chainName = '';
            BootstrapDialog.show({
                title: "Enter the chain name",
                type: BootstrapDialog.TYPE_DEFAULT,
                message: '<p>Some helpful text here explaining that you need to name your chain before you can configure it</p><br/><input type="text" id="newName" class="form-control" placeholder="Enter Name"  value="">',
                buttons: [{
                    label: "Next",
                    cssClass: "btn-primary",
                    action: function(dialog) {
                        // on success
                        dialog.close();
                        Router.navigate( href + $('#newName').val(), { trigger: true });

                        // on failure - display error, dont close the dialog.
                    }
                }, {
                    label: "Cancel",
                    action: function(dialog) {
                        dialog.close();
                    }
                }]
            });
        },

        addModule: function(e) {
            e.preventDefault();
            // This is mock code, please swap out
            var href = $(e.currentTarget).attr('href');
            BootstrapDialog.show({
                title: "Create new Module",
                type: BootstrapDialog.TYPE_DEFAULT,
                message: '<label>Name</label> <input type="text" id="newModuleNme" class="form-control" placeholder=""  value=""> <br/> <label>Type</label> <select class="form-control">  <option disabled selected>Select Module type</option> <option>Active Directory</option> <option>Adaptive Risk </option> <option>Anonymous</option> <option>Certificate</option> <option>Data Store</option> <option>Device Id (Match)</option> <option>Device Id (Save)</option> <option>Federation</option> <option>HOTP</option> <option>HTTP Basic</option> <option>JDBC</option> <option>LDAP</option> <option>Membership</option> <option>MSISDN</option> <option>OATH</option> <option>OAuth 2.0 / OpenID Connect</option> <option>OpenID Connect id_token bearer</option> <option>Persistent Cookie</option> <option>RADIUS</option> <option>SAE</option> <option>Scripted Module</option> <option>Windows Desktop SSO</option> <option>Windows NT</option> <option>WSSAuth</option> </select>',
                buttons: [{
                    label: "Next",
                    cssClass: "btn-primary",
                    action: function(dialog) {

                        // on success
                        dialog.close();
                        Router.navigate( href + $('#newModuleNme').val(), { trigger: true });

                        // on failure - display error, dont close the dialog.
                    }
                }, {
                    label: $.t("common.form.cancel"),
                    action: function(dialog) {
                        dialog.close();
                    }
                }]
            });
        },

        editChain: function(e) {
            e.preventDefault();
            Router.navigate( '#console/chaining/', {trigger: true});
        },

        render: function(args, callback) {
            var self = this;

            this.data.realm = Configuration.globalData.auth.subRealm || "Top level Realm";
            this.data.consolePath = Constants.CONSOLE_PATH;

            this.parentRender(function() {
                self.renderSettingsTab();

                if(callback) {
                    callback();
                }
            });
        },
        renderSettingsTab: function() {
            var self = this;

            SMSDelegate.Realm.Authentication.get()
            .done(function(data) {
                self.data.form = new FormCollection();
                self.data.form.add(new Form(self.$el.find('#settings .col-md-6:first').get(0), {
                    type: 'object',
                    properties: {
                        adminAuthModule: data.schema.properties["null"].properties.adminAuthModule,
                        loginSuccessUrl: data.schema.properties.postauthprocess.properties.loginSuccessUrl
                    }
                }, data.values));
                self.data.form.add(new Form(self.$el.find('#settings .col-md-6:last').get(0), {
                    type: 'object',
                    properties: {
                        orgConfig: data.schema.properties["null"].properties.orgConfig
                    }
                }, data.values));
            })
            .fail(function() {
                // TODO: Add failure condition
            });
        },
        renderChainsTab: function(event) {
            // Not Implemented
        },
        renderModulesTab: function(event) {
            // Not Implemented
        },
        save: function(event) {
            var promise = SMSDelegate.Realm.Authentication.save(this.data.form.data());

            FormHelper.bindSavePromiseToElement(promise, event.target);
        }
    });

    return new Authentication();
});
