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
    "org/forgerock/openam/ui/admin/services/realm/sms/ServicesService",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/admin/views/realms/services/SubSchemaListView",
    "org/forgerock/openam/ui/common/components/PartialBasedView",
    "org/forgerock/openam/ui/common/components/TabComponent",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView"
], ($, _, Messages, AbstractView, EventManager, Router, Constants, ServicesService, FormHelper,
    SubSchemaListView, PartialBasedView, TabComponent, JSONSchema, JSONValues, FlatJSONSchemaView) => {
    const PSEUDO_TAB = { id: _.uniqueId("pseudo_tab_"), title: $.t("console.common.configuration") };
    const SUBSCHEMA_TAB = { id: "subschema", title: $.t("console.services.edit.secondaryConfigurations") };

    function deleteService () {
        ServicesService.instance.remove(this.data.realmPath, this.data.type).then(() => {
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");

            Router.routeTo(Router.configuration.routes.realmsServices, {
                args: [encodeURIComponent(this.data.realmPath)],
                trigger: true
            });
        }, (model, response) => {
            Messages.addMessage({
                response,
                type: Messages.TYPE_DANGER
            });
        });
    }
    function createTabs (schema, subSchemaTypes) {
        let tabs = [];
        const hasSubSchema = subSchemaTypes.length > 0;

        if (schema.isCollection()) {
            tabs = tabs.concat(_(schema.raw.properties)
                .map((value, key) => ({ id: key, order: value.propertyOrder, title: value.title }))
                .sortBy("order")
                .value());

        } else {
            tabs.push(PSEUDO_TAB);
        }

        if (hasSubSchema) {
            tabs.push(SUBSCHEMA_TAB);
        }

        return tabs;
    }

    return AbstractView.extend({
        template: "templates/admin/views/realms/services/EditServiceTemplate.html",
        partials: [
            "partials/form/_JSONSchemaFooter.html",
            "templates/admin/views/realms/partials/_HeaderDeleteButton.html"
        ],
        events: {
            "click [data-save]": "onSave",
            "click [data-delete]": "onDelete"
        },
        createTabComponent (tabs) {
            return new TabComponent({
                tabs,
                createBody: (id) => {
                    if (id === SUBSCHEMA_TAB.id) {
                        return new SubSchemaListView({
                            realmPath: this.data.realmPath,
                            type: this.data.type
                        });
                    } else if (id === PSEUDO_TAB.id) {
                        return new FlatJSONSchemaView({
                            schema: this.data.schema,
                            values: this.data.values
                        });
                    } else {
                        return new FlatJSONSchemaView({
                            schema: new JSONSchema(this.data.schema.raw.properties[id]),
                            values: new JSONValues(this.data.values.raw[id])
                        });
                    }
                },
                createFooter: (id) => {
                    if (id !== SUBSCHEMA_TAB.id) {
                        return new PartialBasedView({ partial: "form/_JSONSchemaFooter" });
                    }
                }
            });
        },
        getJSONSchemaView () {
            return this.subview instanceof TabComponent ? this.subview.getBody() : this.subview;
        },
        render (args) {
            this.data.realmPath = args[0];
            this.data.type = args[1];

            ServicesService.instance.get(this.data.realmPath, this.data.type).then((response) => {
                this.data.schema = response.schema;
                this.data.values = response.values;
                this.data.name = response.name;

                const tabs = createTabs(response.schema, response.subSchemaTypes);
                const hasTabs = tabs.length > 1;
                this.data.hasTabs = hasTabs;

                this.parentRender(() => {
                    if (hasTabs) {
                        this.subview = this.createTabComponent(tabs);
                    } else {
                        this.subview = new FlatJSONSchemaView({
                            schema: response.schema,
                            values: response.values
                        });
                    }

                    this.subview.setElement("[data-json-form]");
                    this.subview.render();
                });
            });
        },
        getCurrentValues () {
            if (this.data.schema.isCollection()) {
                return this.data.values.extend({
                    [this.subview.getTabId()]: this.getJSONSchemaView().getData()
                }).raw;
            } else {
                return this.getJSONSchemaView().getData();
            }
        },
        onSave () {
            ServicesService.instance.update(this.data.realmPath, this.data.type, this.getCurrentValues())
                .then(() => {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
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
            }, _.bind(deleteService, this, e));
        }
    });
});
