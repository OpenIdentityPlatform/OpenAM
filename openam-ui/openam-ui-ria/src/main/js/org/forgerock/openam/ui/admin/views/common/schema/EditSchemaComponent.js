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

/**
 * @module org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent
 */
define([
    "jquery",
    "lodash",
    "backbone",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/admin/views/common/TabSearch",
    "org/forgerock/openam/ui/admin/views/common/schema/SubSchemaListComponent",
    "org/forgerock/openam/ui/common/components/PartialBasedView",
    "org/forgerock/openam/ui/common/components/TabComponent",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/common/util/Promise",
    "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView"
], ($, _, Backbone, Messages, AbstractView, EventManager, Router, Constants, UIUtils, FormHelper, TabSearch,
    SubSchemaListComponent, PartialBasedView, TabComponent, JSONSchema, JSONValues, Promise, FlatJSONSchemaView) => {

    const PSEUDO_TAB = { id: _.uniqueId("pseudo_tab_"), title: $.t("console.common.configuration") };
    const SUBSCHEMA_TAB = { id: "subschema", title: $.t("console.common.secondaryConfigurations") };

    const createTabs = (schema, subSchemaTypes) => {
        let tabs = [];
        const hasSubSchema = subSchemaTypes && subSchemaTypes.length > 0;

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
    };

    return Backbone.View.extend({
        partials: [
            "partials/form/_JSONSchemaFooter.html"
        ],
        events: {
            "click [data-save]": "onSave",
            "click [data-delete]": "onDelete"
        },

        initialize ({
            data,
            listRoute,
            listRouteArgs,
            template,
            subSchemaTemplate,
            getInstance,
            updateInstance,
            deleteInstance,
            getSubSchemaTypes,
            getSubSchemaCreatableTypes,
            getSubSchemaInstances,
            deleteSubSchemaInstance
        }) {
            this.data = data;
            this.listRoute = listRoute;
            this.listRouteArgs = listRouteArgs;
            this.template = template;
            this.subSchemaTemplate = subSchemaTemplate;

            this.getInstance = getInstance;
            this.updateInstance = updateInstance;
            this.deleteInstance = deleteInstance;

            this.getSubSchemaTypes = getSubSchemaTypes;
            this.getSubSchemaCreatableTypes = getSubSchemaCreatableTypes;
            this.getSubSchemaInstances = getSubSchemaInstances;
            this.deleteSubSchemaInstance = deleteSubSchemaInstance;
        },

        createTabComponent (tabs) {
            return new TabComponent({
                tabs,
                createBody: (id) => {
                    if (id === SUBSCHEMA_TAB.id) {
                        return new SubSchemaListComponent({
                            data: this.data,
                            subSchemaTemplate: this.subSchemaTemplate,
                            getSubSchemaCreatableTypes: this.getSubSchemaCreatableTypes,
                            getSubSchemaInstances: this.getSubSchemaInstances,
                            deleteSubSchemaInstance: this.deleteSubSchemaInstance
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

        render () {
            const serviceCalls =
                _([this.getInstance, this.getSubSchemaTypes])
                .compact()
                .map((serviceCall) => serviceCall())
                .value();

            Promise.all(serviceCalls).then((response) => {
                const instance = response[0];
                const subSchema = response[1];

                this.data.schema = instance.schema;
                this.data.values = instance.values;

                this.data.name = instance.name;

                const tabs = createTabs(instance.schema, subSchema);
                const hasTabs = tabs.length > 1;

                this.data.hasTabs = hasTabs;

                UIUtils.fillTemplateWithData(this.template, this.data, (html) => {
                    this.$el.html(html);

                    if (hasTabs) {
                        this.subview = this.createTabComponent(tabs);
                        if (this.data.schema.isCollection()) {
                            const options = {
                                properties: this.data.schema.raw.properties,
                                onChange: (tabId, value) => {
                                    this.subview.setTabId(tabId);
                                    this.$el.find(`[data-schemapath="root.${value}"]`).find("input").focus();
                                }
                            };
                            this.$el.find("[data-tab-search]").append(new TabSearch(options).render().$el);
                        }
                    } else {
                        this.subview = new FlatJSONSchemaView({
                            schema: instance.schema,
                            values: instance.values
                        });
                    }

                    this.subview.setElement(this.$el.find("[data-json-form]"));
                    this.subview.render();
                });
            });

            return this;
        },

        updateValues () {
            if (this.data.schema.isCollection()) {
                this.data.values = this.data.values.extend({
                    [this.subview.getTabId()]: this.getJSONSchemaView().getData()
                });
            } else {
                this.data.values = this.data.values.extend(this.getJSONSchemaView().getData());
            }
        },

        onSave () {
            if (!this.getJSONSchemaView().isValid()) {
                Messages.addMessage({
                    message: $.t("common.form.validation.errorsNotSaved"),
                    type: Messages.TYPE_DANGER
                });
                return;
            }
            this.updateValues();
            this.updateInstance(this.data.values.raw).then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
            }, (response) => {
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
            });
        },

        onDelete (e) {
            e.preventDefault();

            FormHelper.showConfirmationBeforeDeleting({
                message: $.t("console.common.confirmDeleteSelected")
            }, () => {
                this.deleteInstance().then(() => {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                    Router.routeTo(this.listRoute, { args: this.listRouteArgs, trigger: true });
                }, (model, response) => {
                    Messages.addMessage({ response, type: Messages.TYPE_DANGER });
                });
            });
        }
    });

});
