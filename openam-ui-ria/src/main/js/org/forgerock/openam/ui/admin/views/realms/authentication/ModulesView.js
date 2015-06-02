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
define("org/forgerock/openam/ui/admin/views/realms/authentication/ModulesView", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/delegates/SMSDelegate",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/utils/FormHelper"
], function ($, AbstractView, Configuration, EventManager, Router, Constants, SMSDelegate, Form, FormHelper) {
    var ModulesView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/ModulesTemplate.html",
        events: {
            'click #revertChanges': 'revert',
            'click #saveChanges': 'save',
            'show.bs.tab ul.nav.nav-tabs a': 'renderTab'
        },
        render: function (args, callback) {
            var self = this;

            SMSDelegate.RealmAuthenticationModule.get()
            .done(function (data) {
                self.data.formData = data;
                self.parentRender(function () {
                    self.$el.find('ul.nav a:first').tab('show');
                    self.$el.find('.console-tabs .nav-tabs').tabdrop();

                    if (callback) {
                        callback();
                    }
                });
            })
            .fail(function () {
                // TODO: Add failure condition
            });
        },

        save: function (event) {
            var promise = SMSDelegate.RealmAuthenticationModule.save(this.data.form.data());
            FormHelper.bindSavePromiseToElement(promise, event.target);
        },
        revert: function () {
            this.data.form.reset();
        },

        renderTab: function (event) {
            var tabId = $(event.target).attr("href"),
                schema = this.data.formData.schema.properties[tabId.slice(1)],
                element = $(tabId).get(0);

            this.$el.find(tabId).empty();
            this.data.form = new Form(element, schema, this.data.formData.values);
        }

    });

    return ModulesView;
});
