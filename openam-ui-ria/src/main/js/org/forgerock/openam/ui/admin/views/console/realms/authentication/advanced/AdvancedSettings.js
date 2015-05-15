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
define("org/forgerock/openam/ui/admin/views/console/realms/authentication/advanced/AdvancedSettings", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/admin/delegates/SMSDelegate"
], function(AbstractView, Configuration, Constants, Form, FormHelper, SMSDelegate) {
    var AdvancedSettings = AbstractView.extend({
        template: "templates/admin/views/console/realms/authentication/advanced/AdvancedSettingsTemplate.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        events: {
            'click #revert': 'revert',
            'click #saveChanges': 'save',
            'show.bs.tab ul.nav.nav-tabs a': 'renderTab'
        },
        data: {},

        render: function(args, callback) {
            var self = this;

            this.data.realm = Configuration.globalData.auth.subRealm || "Top level Realm";
            this.data.name = args[0];
            this.data.consolePath = Constants.CONSOLE_PATH;

            SMSDelegate.RealmAuthentication.get()
            .done(function(data) {
                self.data.formData = data;

                self.parentRender(function() {
                    self.$el.find("div.tab-pane").show(); // FIXME: To remove
                    self.$el.find('ul.nav a:first').tab('show');

                    self.$el.find('.console-tabs .nav-tabs').tabdrop();

                    if (callback) {
                        callback();
                    }
                });
            })
            .fail(function() {
                // TODO: Add failure condition
            });
        },
        renderTab: function(event) {
            this.$el.find("div.panel-body").empty(); // FIXME: Improve

            var id = $(event.target).attr('href').slice(1),
                schema = this.data.formData.schema.properties[id],
                element = $("div.panel-body").get(0);

            this.data.form = new Form(element, schema, this.data.formData.values);
        },
        revert: function() {
            this.data.form.reset();
        },
        save: function(event) {
            var promise = SMSDelegate.RealmAuthentication.save(this.data.form.data());

            FormHelper.bindSavePromiseToElement(promise, event.target);
        }
    });

    return new AdvancedSettings();
});
