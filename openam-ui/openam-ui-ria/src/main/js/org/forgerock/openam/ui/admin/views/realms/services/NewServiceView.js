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

    // jquery dependencies
    "bootstrap-tabdrop"
], ($, _, Messages, AbstractView, EventManager, Router, Constants, Form, ServicesService) => {

    function toggleCreate (el, enable) {
        el.find("[data-create]").prop("disabled", !enable);
    }

    function shouldHideProperty (obj, key, value) {

        return obj.required === true &&
                (obj.type === "boolean" ||
                 obj.type === "object" ||  // Remove once AME-9762 is completed
                (obj.type === "string" && !_.isEmpty(value)) ||
                (obj.type === "array" && !_.isEmpty(value)) ||
                (obj.type === "number" && value !== ""));
    }


    function toggleHideSchemaGroup (schema, hide) {
        _.set(schema, "options.hidden", hide);
    }

    function hideProperty (schema, key) {
        _.set(schema, "properties[" + key + "].options.hidden", true);
    }

    function hideProps (schema, values) {

        toggleHideSchemaGroup(schema, true);
        _.each(schema.properties, (obj, key) => {
            if (shouldHideProperty(obj, key, values[key])) {
                hideProperty(schema, key);
            } else {
                toggleHideSchemaGroup(schema, false);
            }
        });

        return schema;
    }

    return AbstractView.extend({
        template: "templates/admin/views/realms/services/NewServiceTemplate.html",
        events: {
            "click [data-create]": "onCreateClick",
            "change #serviceSelection": "onSelectService"
        },

        render: function (args, callback) {
            this.data.realmPath = args[0];

            ServicesService.type.getCreatables(this.data.realmPath).then((creatableTypes) => {
                this.data.creatableTypes = creatableTypes;

                this.parentRender(() => {
                    if (this.data.creatableTypes.length > 1) {
                        this.$el.find("#serviceSelection").selectize();
                    } else if (this.data.creatableTypes[0] && this.data.creatableTypes[0]._id) {
                        this.selectService(this.data.creatableTypes[0]._id);
                    }
                    if (callback) { callback(); }
                });
            });
        },

        onSelectService: function (e) {
            this.selectService(e.target.value);
        },

        selectService: function (service) {
            toggleCreate(this.$el, service);

            if (service && service !== this.data.type) {
                this.data.type = service;

                if (this.form) {
                    this.form.destroy();
                }

                ServicesService.instance.getInitialState(this.data.realmPath, this.data.type)
                    .then((data) => {
                        let schemaWithHiddenProperties = _.clone(data.schema, true);
                        data.schema.grouped = false; // FIXME: Remove once AME-9670 is fixed
                        if (data.schema.grouped) {
                            _.each(data.schema.properties, (groupedSchema, key) => {
                                schemaWithHiddenProperties.properties[key] = hideProps(groupedSchema, data.values[key]);
                            });
                        } else {
                            schemaWithHiddenProperties = hideProps(data.schema, data.values);
                        }

                        this.form = new Form(
                            this.$el.find("[data-service-form]")[0],
                            schemaWithHiddenProperties,
                            data.values
                        );
                    }, () => {
                        toggleCreate(this.$el, false);
                    });
            }
        },

        onCreateClick: function () {
            ServicesService.instance.create(this.data.realmPath, this.data.type, this.form.data()).then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                Router.routeTo(Router.configuration.routes.realmsServiceEdit, {
                    args: _.map([this.data.realmPath, this.data.type], encodeURIComponent),
                    trigger: true
                });
            }, (response) => {
                Messages.addMessage({
                    response: response,
                    type: Messages.TYPE_DANGER
                });
            });
        }
    });
});
