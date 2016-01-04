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

define("org/forgerock/openam/ui/admin/views/realms/authentication/modules/EditModuleView", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/delegates/SMSRealmDelegate",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/utils/FormHelper",

    // jquery dependencies
    "bootstrap-tabdrop"
], function ($, AbstractView, Configuration, EventManager, Router, Constants,
             SMSRealmDelegate, Form, FormHelper) {
    var EditModuleView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/modules/EditModuleViewTemplate.html",
        events: {
            "click #revert": "revert",
            "click #save": "save",
            "show.bs.tab ul.nav.nav-tabs a": "renderTab"
        },
        render: function (args, callback) {
            var self = this;

            this.data.realmPath = args[0];
            this.data.type = args[1];
            this.data.name = args[2];

            $.when(
                SMSRealmDelegate.authentication.modules.schema(this.data.realmPath, this.data.type),
                SMSRealmDelegate.authentication.modules.get(this.data.realmPath, this.data.name, this.data.type),
                SMSRealmDelegate.authentication.modules.types.get(this.data.realmPath, this.data.type)
            ).done(function (schemaData, valuesData, moduleType) {
                self.data.schema = schemaData;
                self.data.values = valuesData;
                self.data.typeDescription = moduleType.name;

                self.parentRender(function () {
                    if (!self.data.schema.grouped) {
                        self.data.form = new Form(self.$el.find("#tabpanel")[0], self.data.schema, self.data.values);
                    }

                    self.$el.find("ul.nav a:first").tab("show");
                    self.$el.find(".tab-menu .nav-tabs").tabdrop();

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
            FormHelper.bindSavePromiseToElement(promise, event.currentTarget);
        },
        revert: function () {
            this.data.form.reset();
        },
        renderTab: function (event) {
            var id = $(event.target).attr("href").slice(1),
                schema = this.data.schema.properties[id],
                element = this.$el.find("#tabpanel").empty().get(0);

            this.data.form = new Form(element, schema, this.data.values);
        }
    });

    return EditModuleView;
});
