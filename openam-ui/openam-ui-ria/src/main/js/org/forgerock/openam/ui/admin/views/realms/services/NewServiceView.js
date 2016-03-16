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

define("org/forgerock/openam/ui/admin/views/realms/services/NewServiceView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/services/realm/sms/ServicesService",
    "org/forgerock/openam/ui/common/views/jsonSchema/JSONSchemaView",

    // jquery dependencies
    "bootstrap-tabdrop"
], ($, _, Messages, AbstractView, EventManager, Router, Constants, Form, ServicesService, JSONSchemaView) => {
    function toggleCreate (el, enable) {
        el.find("[data-create]").prop("disabled", !enable);
    }

    return AbstractView.extend({
        template: "templates/admin/views/realms/services/NewServiceTemplate.html",
        partials: [
            "partials/alerts/_Alert.html"
        ],
        events: {
            "click [data-create]": "onCreateClick",
            "change [data-service-selection]": "onSelectService"
        },

        render (args, callback) {
            this.data.realmPath = args[0];

            ServicesService.type.getCreatables(this.data.realmPath).then((creatableTypes) => {
                this.data.creatableTypes = creatableTypes;

                this.parentRender(() => {
                    if (this.data.creatableTypes.length > 1) {
                        const serviceSelection = this.$el.find("[data-service-selection]");
                        serviceSelection.selectize({
                            onInitialize () {
                                this.$control_input.attr("id", "serviceSelection");
                            }
                        });
                    } else if (this.data.creatableTypes[0] && this.data.creatableTypes[0]._id) {
                        this.selectService(this.data.creatableTypes[0]._id);
                    }
                    if (callback) { callback(); }
                });
            });
        },

        onSelectService (event) {
            this.selectService(event.target.value);
        },

        selectService (service) {
            toggleCreate(this.$el, false);
            if (service && service !== this.data.type) {
                this.data.type = service;

                if (this.jsonSchemaView) {
                    this.jsonSchemaView.remove();
                }

                ServicesService.instance.getInitialState(this.data.realmPath, this.data.type).then((response) => {
                    this.jsonSchemaView = new JSONSchemaView({
                        schema: response.schema,
                        values: response.values,
                        onRendered: toggleCreate(this.$el, true)
                    });
                    $(this.jsonSchemaView.render().el).appendTo(this.$el.find("[data-service-form]"));
                }, () => {
                    toggleCreate(this.$el, false);
                });
            }
        },

        onCreateClick () {
            ServicesService.instance.create(this.data.realmPath, this.data.type, this.jsonSchemaView.values())
            .then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                Router.routeTo(Router.configuration.routes.realmsServiceEdit, {
                    args: _.map([this.data.realmPath, this.data.type], encodeURIComponent),
                    trigger: true
                });
            }, (response) => {
                Messages.addMessage({
                    response,
                    type: Messages.TYPE_DANGER
                });
            });
        }
    });
});
