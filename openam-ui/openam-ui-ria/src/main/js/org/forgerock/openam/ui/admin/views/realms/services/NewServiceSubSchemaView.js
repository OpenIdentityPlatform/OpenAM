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

define("org/forgerock/openam/ui/admin/views/realms/services/NewServiceSubSchemaView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/realm/sms/ServicesService",
    "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView",
    "org/forgerock/openam/ui/common/views/jsonSchema/GroupedJSONSchemaView"
], ($, _, Messages, AbstractView, EventManager, Router, Constants, ServicesService, FlatJSONSchemaView,
    GroupedJSONSchemaView) => AbstractView.extend({
        template: "templates/admin/views/realms/services/NewServiceSubSchemaTemplate.html",
        events: {
            "click [data-save]": "onSave",
            "keyup [data-name]": "onNameChange",
            "change [data-input]": "onNameChange"
        },
        onNameChange (event) {
            const isEmpty = _.isEmpty(event.currentTarget.value);

            this.setCreateEnabled(!isEmpty);
        },
        setCreateEnabled (enabled) {
            this.$el.find("[data-save]").prop("disabled", !enabled);
        },
        render (args) {
            this.data.realmPath = args[0];
            this.data.serviceInstance = args[1];
            this.data.subSchemaType = args[2];

            this.parentRender(() => {
                ServicesService.type.subSchema.instance.getInitialState(
                    this.data.realmPath, this.data.serviceInstance, this.data.subSchemaType
                ).then((response) => {
                    const options = {
                        schema: response.schema,
                        values: response.values,
                        showOnlyRequiredAndEmpty: true
                    };

                    if (response.schema.isCollection()) {
                        this.jsonSchemaView = new GroupedJSONSchemaView(options);
                    } else {
                        this.jsonSchemaView = new FlatJSONSchemaView(options);
                    }

                    $(this.jsonSchemaView.render().el).appendTo(this.$el.find("[data-json-form]"));
                });
            });
        },
        onSave () {
            const formData = this.jsonSchemaView.values();
            const subSchemaInstanceId = this.$el.find("[data-name]").val();

            formData["_id"] = subSchemaInstanceId;

            ServicesService.type.subSchema.instance.create(
                this.data.realmPath,
                this.data.serviceInstance,
                this.data.subSchemaType,
                formData
            ).then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");

                Router.routeTo(Router.configuration.routes.realmsServiceSubSchemaEdit, {
                    args: _.map([
                        this.data.realmPath, this.data.serviceInstance, this.data.subSchemaType, subSchemaInstanceId
                    ], encodeURIComponent),
                    trigger: true
                });
            }, (response) => {
                Messages.addMessage({
                    response,
                    type: Messages.TYPE_DANGER
                });
            });
        }
    })
);
