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
    "org/forgerock/openam/ui/admin/services/realm/ServicesService",
    "org/forgerock/openam/ui/admin/utils/FormHelper",

    // jquery dependencies
    "bootstrap-tabdrop"
], ($, _, Messages, AbstractView, EventManager, Router, Constants, Form, ServicesService, FormHelper) => {

    function deleteInstance () {
        var self = this;

        ServicesService.type.subSchema.instance.remove(
            this.data.realmPath,
            this.data.serviceType,
            this.data.subSchemaType,
            this.data.id
        ).then(() => {
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");

            Router.routeTo(Router.configuration.routes.realmsServiceEdit, {
                args: _.map([self.data.realmPath, self.data.serviceType], encodeURIComponent),
                trigger: true
            });
        }, (model, response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    return AbstractView.extend({
        template: "templates/admin/views/realms/services/EditServiceSubSchemaTemplate.html",
        events: {
            "click [data-save]": "onSave",
            "click [data-delete]": "onDelete",
            "show.bs.tab ul.nav.nav-tabs a": "renderTab"
        },

        render (args, callback) {
            this.data.realmPath = args[0];
            this.data.serviceType = args[1];
            this.data.subSchemaType = args[2];
            this.data.id = args[3];
            this.data.headerActions = [
                { actionPartial: "form/_Button", data:"delete", title:"common.form.delete", icon:"fa-times" }
            ];

            var self = this;

            ServicesService.type.subSchema.instance.get(
                this.data.realmPath,
                this.data.serviceType,
                this.data.subSchemaType,
                this.data.id
            ).then((data) => {
                self.data.schema = data.schema;
                self.data.values = data.values;

                self.parentRender(() => {
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

        onSave () {
            var self = this;
            ServicesService.type.subSchema.instance.update(
                this.data.realmPath,
                this.data.serviceType,
                this.data.subSchemaType,
                this.data.id,
                this.form.data()
            ).then((data) => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                self.data.values = data;
            }, (response) => {
                Messages.addMessage({
                    response,
                    type: Messages.TYPE_DANGER
                });
            });
        },

        onDelete (e) {
            e.preventDefault();

            FormHelper.showConfirmationBeforeDeleting({
                message: $.t("console.services.list.confirmDeleteSelected")
            }, _.bind(deleteInstance, this, e));
        },

        renderTab (event) {
            const tabId = $(event.target).data("tabId");
            const schema = this.data.schema.grouped ? this.data.schema.properties[tabId] : this.data.schema;
            const element = this.$el.find("#tabpanel").empty().get(0);

            this.form = new Form(element, schema, this.data.values[tabId]);
            this.$el.find("[data-header]").parent().hide();
        }
    });
});
