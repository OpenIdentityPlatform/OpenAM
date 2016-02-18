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
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/services/realm/sms/ServicesService",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/admin/views/realms/services/SubSchemaListView",

    // jquery dependencies
    "bootstrap-tabdrop"
], ($, _, Messages, AbstractView, EventManager, Router, Constants, Form, ServicesService, FormHelper,
    SubschemaListView) => {

    function deleteService () {
        ServicesService.instance.remove(this.data.realmPath, this.data.type).then(() => {
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");

            Router.routeTo(Router.configuration.routes.realmsServices, {
                args: [encodeURIComponent(this.data.realmPath)],
                trigger: true
            });
        }, (model, response) => {
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

            ServicesService.instance.get(this.data.realmPath, this.data.type).then((data) => {
                this.data.schema = data.schema;
                this.data.values = data.values;
                this.data.name = data.name;
                this.data.subSchemaPresent = data.subSchemaTypes.length;
                this.data.tabbed = this.data.schema.grouped || this.data.subSchemaPresent;

                this.parentRender(function () {
                    if (this.data.tabbed) {
                        this.$el.find("ul.nav a:first").tab("show");
                        this.$el.find(".tab-menu .nav-tabs").tabdrop();

                        if (this.data.subSchemaPresent) {
                            SubschemaListView.element = this.$el.find("#tabpanel");
                        }
                    } else {
                        this.form = new Form(
                            this.$el.find("#tabpanel")[0],
                            this.data.schema,
                            this.data.values
                        );
                    }
                    if (callback) { callback(); }
                });
            });
        },

        onSave: function () {
            ServicesService.instance.update(this.data.realmPath, this.data.type, this.form.data())
                .then((data) => {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                    this.data.values = data;
                }, (response) => {
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
            }, _.bind(deleteService, this, e));
        },

        renderTab: function (event) {
            const tabId = $(event.target).data("tabId"),
                schema = this.data.schema.grouped ? this.data.schema.properties[tabId] : this.data.schema,
                element = this.$el.find("#tabpanel").empty().get(0);

            if (tabId === "subschema") {
                SubschemaListView.render(this.data);
            } else {
                this.form = new Form(element, schema, this.data.values);
            }
        }
    });
});
