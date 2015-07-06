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
define("org/forgerock/openam/ui/admin/views/realms/authentication/modules/EditModuleView", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/delegates/SMSRealmDelegate",
    "org/forgerock/openam/ui/admin/delegates/SMSGlobalDelegate",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/utils/FormHelper"
], function ($, AbstractView, Configuration, EventManager, Router, Constants, SMSRealmDelegate, SMSGlobalDelegate, Form, FormHelper) {
    var EditModuleView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/modules/EditModuleViewTemplate.html",
        events: {
            'click #revertChanges': 'revert',
            'click #saveChanges': 'save',
            'show.bs.tab ul.nav.nav-tabs a': 'renderTab'
        },
        render: function (args, callback) {
            var self = this;

            this.data.realmPath = args[0];
            this.data.name = args[1];
            this.data.type = args[2];

            $.when(
                SMSGlobalDelegate.authentication.modules.schema(args[2]),
                SMSRealmDelegate.authentication.modules.get(this.data.realmPath, args[1], args[2])
            ).done(function (schemaData, valuesData) {
                self.data.schemaData = schemaData[0];
                self.data.valuesData = valuesData[0];
                self.parentRender(function () {
                    self.$el.find('ul.nav a:first').tab('show');
                    self.$el.find('.tab-menu .nav-tabs').tabdrop();

                    //TODO either add this to the server generated schema (preferred) or this get's moved into the JS sanitising functions for JSON Schemas
                    self.data.schemaData.type = "object";
                    self.data.form = new Form(self.$el.find("#moduleContent")[0], self.data.schemaData, self.data.valuesData);

                    if (callback) {
                        callback();
                    }
                });
            }).fail(function () {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "notFoundError");
            });
        },
        save: function (event) {
            var promise = SMSRealmDelegate.authentication.modules.update(this.data.realmPath,
                                                                         this.data.name,
                                                                         this.data.type,
                                                                         this.data.form.data());
            FormHelper.bindSavePromiseToElement(promise, event.target);
        },
        revert: function () {
            this.data.form.reset();
        },
        renderTab: function (event) {
            var tabId = $(event.target).attr("href"),
                element = $(tabId).get(0);

            this.$el.find(tabId).empty();
            //TODO either add this to the server generated schema (preferred) or this get's moved into the JS sanitising functions for JSON Schemas
            this.data.schemaData.type = "object";
            this.data.form = new Form(element, this.data.schemaData, this.data.valuesData);
        }

    });

    return EditModuleView;
});
