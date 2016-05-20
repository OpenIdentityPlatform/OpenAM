/**
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

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/global/ServersService",
    "org/forgerock/openam/ui/common/components/PanelComponent",
    "org/forgerock/openam/ui/common/components/PartialBasedView",
    "org/forgerock/openam/ui/common/components/TabComponent",
    "org/forgerock/openam/ui/common/components/table/InlineEditTable",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/common/util/Promise",
    "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView"
], ($, _, Messages, AbstractView, EventManager, Constants, ServersService, PanelComponent, PartialBasedView,
    TabComponent, InlineEditTable, JSONSchema, JSONValues, Promise, FlatJSONSchemaView) => {
    function createTabs (schema) {
        return _(schema.raw.properties)
            .map((value, key) => ({ id: key, order: value.propertyOrder, title: value.title }))
            .sortBy("order")
            .value();
    }

    return AbstractView.extend({
        template: "templates/admin/views/common/HeaderFormTemplate.html",
        events: {
            "click [data-save]": "onSave",
            "click [data-inherit-value]": "toggleInheritance"
        },
        getJSONSchemaView () {
            return this.subview.getBody();
        },
        /**
         * Returns an array of promises, where the first item will always be a promise for server defaults
         * schema/values. The array will also contain a second item, which will be the server instance schema/values
         * promise if we are on the edit server instance view.
         *
         * @param   {Boolean} isDefaultServer Is it the server defaults view.
         * @returns {Promise[]}               Array of server promises.
         */
        getServerPromises (isDefaultServer) {
            const promises = [ServersService.servers.get(ServersService.servers.DEFAULT_SERVER, this.sectionId)];

            if (!isDefaultServer) {
                promises.push(ServersService.servers.get(this.serverId, this.sectionId));
            }

            return promises;
        },
        render ([serverId, sectionId]) {
            const isDefaultServer = serverId === "server-defaults";

            this.sectionId = sectionId;
            this.serverId = isDefaultServer ? ServersService.servers.DEFAULT_SERVER : serverId;

            this.data.title = $.t(`console.common.navigation.${this.sectionId}`);

            Promise.all(this.getServerPromises(isDefaultServer)).then((response) => {
                const serverDefaults = response[0];
                const server = response[1];

                if (isDefaultServer) {
                    this.schema = serverDefaults.schema;
                    this.values = serverDefaults.values;
                } else {
                    this.schema = server.schema;
                    this.values = server.values;

                    this.defaultValues = serverDefaults.values;
                }

                const clonedValues = _.cloneDeep(this.values.raw);

                this.parentRender(() => {
                    if (this.sectionId === ServersService.servers.ADVANCED_SECTION) {
                        this.subview = new PanelComponent({
                            createBody: () => new InlineEditTable({
                                values: clonedValues
                            }),
                            createFooter: () => new PartialBasedView({
                                partial: "form/_JSONSchemaFooter"
                            })
                        });
                    } else {
                        const tabs = createTabs(this.schema);
                        this.subview = new TabComponent({
                            tabs,
                            createBody: (id) => new FlatJSONSchemaView({
                                schema: new JSONSchema(this.schema.raw.properties[id]),
                                values: new JSONValues(clonedValues[id])
                            }),
                            createFooter: () => new PartialBasedView({ partial: "form/_JSONSchemaFooter" })
                        });
                    }
                    this.subview.setElement("[data-json-form]");
                    this.subview.render();
                });
            }, (response) => {
                Messages.addMessage({ type: Messages.TYPE_DANGER, response });
            });
        },
        updateData () {
            const section = this.sectionId === ServersService.servers.ADVANCED_SECTION
                ? this.sectionId
                : this.subview.getTabId();

            this.values = this.values.extend({
                [section]: this.getJSONSchemaView().getData()
            });
        },
        onSave () {
            this.updateData();
            ServersService.servers.update(this.sectionId, this.values.raw, this.serverId)
            .then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
            }, (response) => {
                Messages.addMessage({
                    response,
                    type: Messages.TYPE_DANGER
                });
            });
        },
        toggleInheritance (event) {
            const target = event.currentTarget;
            const removeRootPrefix = (key) => key.slice(5);
            const propertySchemaPath = removeRootPrefix(target.getAttribute("data-schemapath"));
            const isInherited = target.getAttribute("data-inherit-value") === "true";
            let propValue;

            if (isInherited) {
                propValue = this.values.raw[this.subview.getTabId()][propertySchemaPath].value;
            } else {
                propValue = this.defaultValues.raw[this.subview.getTabId()][propertySchemaPath];
            }

            this.getJSONSchemaView().subview.toggleInheritance(propertySchemaPath, propValue, !isInherited);
        }
    });
});
