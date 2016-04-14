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

define("org/forgerock/openam/ui/admin/views/configuration/server/ServerDefaultsView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openam/ui/admin/services/ServersService",
    "org/forgerock/openam/ui/common/components/PartialBasedView",
    "org/forgerock/openam/ui/common/components/TabComponent",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView"
], ($, _, Messages, AbstractView, EventManager, Constants, ServersService, PartialBasedView, TabComponent,
    JSONSchema, JSONValues, FlatJSONSchemaView) => {
    function createTabs (schema) {
        return _(schema.raw.properties)
            .map((value, key) => ({ id: key, order: value.propertyOrder, title: value.title }))
            .sortBy("order")
            .value();
    }

    return AbstractView.extend({
        template: "templates/admin/views/common/HeaderFormTemplate.html",
        events: {
            "click [data-save]": "onSave"
        },
        getJSONSchemaView () {
            return this.subview.getTabBody();
        },
        render (args) {
            this.data.sectionId = args[0];

            this.data.title = $.t(`console.common.navigation.${this.data.sectionId}`);

            ServersService.servers.defaults.get(this.data.sectionId).then((response) => {
                this.data.schema = response.schema;
                this.data.values = response.values;

                this.parentRender(() => {
                    const tabs = createTabs(response.schema);

                    this.subview = new TabComponent({
                        tabs,
                        createTabBody: (id) => new FlatJSONSchemaView({
                            schema: new JSONSchema(this.data.schema.raw.properties[id]),
                            values: new JSONValues(this.data.values.raw[id])
                        }),
                        createTabFooter: () => new PartialBasedView({ partial: "form/_JSONSchemaFooter" })
                    });

                    this.subview.setElement("[data-json-form]");
                    this.subview.render();
                });
            }, (response) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
            });
        },
        updateData () {
            this.data.values = this.data.values.extend({
                [this.subview.getTabId()]: this.getJSONSchemaView().values()
            });
        },
        onSave () {
            this.updateData();
            ServersService.servers.defaults.update(this.data.sectionId, this.data.values.raw)
            .then((data) => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                this.data.values = data;
            }, (response) => {
                Messages.addMessage({
                    response,
                    type: Messages.TYPE_DANGER
                });
            });
        }
    });
});
