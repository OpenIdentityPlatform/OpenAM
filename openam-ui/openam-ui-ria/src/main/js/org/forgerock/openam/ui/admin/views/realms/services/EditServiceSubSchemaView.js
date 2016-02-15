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

define("org/forgerock/openam/ui/admin/views/realms/services/EditServiceSubSchemaView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/services/realm/sms/ServicesService",
    "org/forgerock/openam/ui/admin/utils/FormHelper",

    // jquery dependencies
    "bootstrap-tabdrop"
], function ($, _, Messages, AbstractView, EventManager, Router, Constants, Form, ServicesService, FormHelper) {

    function deleteInstance () {
        var self = this;

        ServicesService.type.subSchema.instance.remove(
            this.data.realmPath,
            this.data.serviceType,
            this.data.subSchemaType,
            this.data.id
        ).then(function () {
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");

            Router.routeTo(Router.configuration.routes.realmsServiceEdit, {
                args: _.map([self.data.realmPath, self.data.serviceType], encodeURIComponent),
                trigger: true
            });
        }, function (model, response) {
            Messages.addMessage({
                response: response,
                type: Messages.TYPE_DANGER
            });
        });
    }

    return AbstractView.extend({
        template: "templates/admin/views/realms/services/EditServiceSubSchemaTemplate.html",
        partials: [
            "templates/admin/views/realms/partials/_HeaderDeleteButton.html",
            "partials/breadcrumb/_Breadcrumb.html"
        ],
        events: {
            "click [data-save]": "onSave",
            "click [data-delete]": "onDelete",
            "show.bs.tab ul.nav.nav-tabs a": "renderTab"
        },

        render: function (args, callback) {
            this.data.realmPath = args[0];
            this.data.serviceType = args[1];
            this.data.subSchemaType = args[2];
            this.data.id = args[3];

            this.data.backLink = {
                href: "#" + Router.getLink(Router.configuration.routes.realmsServiceEdit,
                    _.map([this.data.realmPath, this.data.serviceType], encodeURIComponent)),
                text: this.data.serviceType,
                icon: "fa-plug"
            };

            var self = this;

            ServicesService.type.subSchema.instance.get(
                this.data.realmPath,
                this.data.serviceType,
                this.data.subSchemaType,
                this.data.id
            ).then(function (data) {
                self.data.schema = data.schema;
                self.data.values = data.values;

                self.parentRender(function () {

                    if (self.data.schema.grouped) {
                        self.$el.find("ul.nav a:first").tab("show");
                        self.$el.find(".tab-menu .nav-tabs").tabdrop();
                    } else {
                        self.form = new Form(
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
            var self = this;
            ServicesService.type.subSchema.instance.update(
                this.data.realmPath,
                this.data.serviceType,
                this.data.subSchemaType,
                this.data.id,
                this.form.data()
            ).then(function (data) {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                self.data.values = data;
            }, function (response) {
                Messages.addMessage({
                    response: response,
                    type: Messages.TYPE_DANGER
                });
            });
        },

        onDelete: function (e) {
            e.preventDefault();

            FormHelper.showConfirmationBeforeDeleting({
                message: $.t("console.services.list.confirmDeleteSelected")
            }, _.bind(deleteInstance, this, e));
        },

        renderTab: function (event) {
            var tabId = $(event.target).data("tabId"),
                schema = this.data.schema.grouped ? this.data.schema.properties[tabId] : this.data.schema,
                element = this.$el.find("#tabpanel").empty().get(0);

            this.form = new Form(element, schema, this.data.values);
        }
    });
});
