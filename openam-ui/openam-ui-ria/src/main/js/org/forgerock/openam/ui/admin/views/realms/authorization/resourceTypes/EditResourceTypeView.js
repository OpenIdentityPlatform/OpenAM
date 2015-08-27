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
 * Copyright 2015 ForgeRock AS.
 */

/*global define*/
define("org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/EditResourceTypeView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/models/authorization/ResourceTypeModel",
    "org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/ResourceTypePatternsView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/ResourceTypeActionsView"
], function ($, _, Messages, AbstractView, EventManager, Router, Constants, ResourceTypeModel, ResourceTypePatternsView,
             ResourceTypeActionsView) {

    return AbstractView.extend({
        template: "templates/admin/views/realms/authorization/resourceTypes/EditResourceTypeTemplate.html",
        partials: [
            "templates/admin/views/realms/partials/_HeaderDeleteButton.html"
        ],
        events: {
            "click #saveChanges": "submitForm",
            "click #revertChanges": "revertChanges",
            "click #delete": "deleteResourceType"
        },

        initialize: function (options) {
            AbstractView.prototype.initialize.call(this);
            this.model = null;
        },

        onModelSync: function (model, response) {
            this.renderAfterSyncModel();
        },

        render: function (args, callback) {
            var uuid;

            this.realmPath = args[0];
            if (callback) {
                this.renderCallback = callback;
            }

            // Realm location is the first argument, second one is the resource type uuid
            if (args.length === 2) {
                uuid = args[1];
            }

            if (uuid) {
                this.model = new ResourceTypeModel({uuid: uuid});
                this.listenTo(this.model, "sync", this.onModelSync);
                this.model.fetch();
            } else {
                this.model = new ResourceTypeModel();
                this.listenTo(this.model, "sync", this.onModelSync);
                this.renderAfterSyncModel();
            }
        },

        renderAfterSyncModel: function () {
            var self = this,
                data = this.data;
            this.data.entity = this.model.attributes;

            data.actions = [];
            _.each(this.data.entity.actions, function (v, k) {
                data.actions.push({name: k, value: v});
            });
            data.actions.sort();

            this.initialActions = _.cloneDeep(data.actions);
            this.initialPatterns = _.cloneDeep(data.entity.patterns);

            this.parentRender(function () {
                var promises = [], resolve = function () { return (promises[promises.length] = $.Deferred()).resolve;},
                    data = self.data;

                self.patternsView = new ResourceTypePatternsView();
                self.patternsView.render(data.entity, data.entity.patterns, "#resTypePatterns", resolve());

                self.actionsList = new ResourceTypeActionsView();
                self.actionsList.render(data.entity, data.actions, "#resTypeActions", resolve());

                $.when.apply($, promises).done(function () {
                    if (self.renderCallback) { self.renderCallback(); }
                });
            });
        },

        revertChanges: function (e) {
            this.patternsView.render(this.data.entity, this.initialPatterns, "#resTypePatterns");
            this.actionsList.render(this.data.entity, this.initialActions, "#resTypeActions");
        },

        updateFields: function () {
            var app = this.data.entity,
                dataFields = this.$el.find("[data-field]"),
                dataField;

            _.each(dataFields, function (field, key, list) {
                dataField = field.getAttribute("data-field");

                if (field.type === "checkbox") {
                    if (field.checked) {
                        app[dataField].push(field.value);
                    }
                } else {
                    app[dataField] = field.value;
                }
            });
        },

        submitForm: function (e) {
            e.preventDefault();

            var self = this,
                savePromise,
                nonModifiedAttributes = _.clone(this.model.attributes);

            this.updateFields();

            _.extend(this.model.attributes, this.data.entity);
            savePromise = this.model.save();

            if (savePromise) {
                savePromise
                    .done(function (response) {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                    })
                    .fail(function (response) {
                        _.extend(self.model.attributes, nonModifiedAttributes);
                        Messages.messages.addMessage({
                            message: response.responseJSON.message,
                            type: Messages.TYPE_DANGER
                        });
                    });
            } else {
                _.extend(this.model.attributes, nonModifiedAttributes);
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.validationError);
            }
        },

        deleteResourceType: function (e) {
            e.preventDefault();

            var self = this,
                onSuccess = function (model, response, options) {
                    Router.routeTo(Router.configuration.routes.realmsResourceTypes, {
                        args: [encodeURIComponent(self.realmPath)],
                        trigger: true
                    });
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                },
                onError = function (model, response, options) {
                    Messages.messages.addMessage({message: response.responseJSON.message, type: "error"});
                };

            this.model.destroy({
                success: onSuccess,
                error: onError
            });
        }
    });
});
