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
 * Copyright 2016 ForgeRock AS.
 */

define("org/forgerock/openam/ui/admin/views/realms/services/EditServiceView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/SMSRealmService",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/views/realms/services/SubSchemaListView",

    // jquery dependencies
    "bootstrap-tabdrop"
], function ($, _, Messages, AbstractView, EventManager, Router, Constants, SMSRealmService, FormHelper, Form,
             SubschemaListView) {

    function deleteService () {
        SMSRealmService.services.instance.remove().then(_.bind(function () {
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");

            Router.routeTo(Router.configuration.routes.realmsServices, {
                args: [encodeURIComponent(this.data.realmPath)],
                trigger: true
            });
        }, this), function (model, response) {
            Messages.addMessage({
                response: response,
                type: Messages.TYPE_DANGER
            });
        });
    }

    return AbstractView.extend({
        template: "templates/admin/views/realms/services/EditServiceTemplate.html",
        partials: [
            "templates/admin/views/realms/partials/_HeaderDeleteButton.html"
        ],
        events: {
            "click [data-save]": "onSave",
            "click [data-delete]": "onDelete",
            "show.bs.tab ul.nav.nav-tabs a": "renderTab"
        },

        render: function (args, callback) {
            this.data.realmPath = args[0];
            this.data.type = args[1];

            var self = this;

            SMSRealmService.services.instance.get(this.data.realmPath, this.data.type).then(function (data) {

                self.data.schema = data.schema;
                self.data.values = data.values;
                self.data.name = data.name;
                self.data.subschema = data.subschema;

                self.parentRender(function () {

                    if (self.data.schema.grouped) {
                        self.$el.find("ul.nav a:first").tab("show");
                        self.$el.find(".tab-menu .nav-tabs").tabdrop();
                        SubschemaListView.element = this.$el.find("#tabpanel");
                    } else {
                        self.data.form = new Form(
                            self.$el.find("#tabpanel")[0],
                            self.data.schema,
                            self.data.values
                        );
                    }
                    if (callback) { callback(); }
                });
            });
        },

        onSave: function () {
            SMSRealmService.services.instance.update().then(function () {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
            });
        },

        onDelete: function (e) {
            e.preventDefault();

            FormHelper.showConfirmationBeforeDeleting({
                message: $.t("console.services.list.confirmDeleteSelected")
            }, _.bind(deleteService, this, e));
        },
        renderTab: function (event) {
            var tabId = $(event.target).data("tabid"),
                schema = this.data.schema.properties[tabId],
                element = this.$el.find("#tabpanel").empty().get(0);

            if (this.data.schema.grouped && tabId === "subschema") {
                SubschemaListView.render(this.data.subschema);
            } else {
                this.data.form = new Form(element, schema, this.data.values);
            }
        }
    });
});
