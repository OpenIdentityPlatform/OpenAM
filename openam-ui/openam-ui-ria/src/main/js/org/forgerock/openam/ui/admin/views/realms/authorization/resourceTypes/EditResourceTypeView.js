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
 * Copyright 2015-2016 ForgeRock AS.
 */

define("org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/EditResourceTypeView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/admin/models/authorization/ResourceTypeModel",
    "org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/ResourceTypePatternsView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/ResourceTypeActionsView",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "bootstrap-tabdrop"
], function ($, _, Messages, AbstractView, EventManager, Router, Constants, UIUtils, ResourceTypeModel,
             ResourceTypePatternsView, ResourceTypeActionsView, FormHelper) {

    return AbstractView.extend({
        partials: [
            "templates/admin/views/realms/partials/_HeaderDeleteButton.html",
            "partials/util/_HelpLink.html"
        ],
        events: {
            "click #saveChanges": "submitForm",
            "click #delete": "onDeleteClick"
        },
        tabs: [
            { name: "patterns", attr: ["patterns"] },
            { name: "actions", attr: ["actions"] },
            { name: "settings", attr: ["name", "description"] }
        ],

        onModelSync: function () {
            this.renderAfterSyncModel();
        },

        render: function (args, callback) {
            var uuid;

            this.data.realmPath = args[0];
            if (callback) {
                this.renderCallback = callback;
            }

            // Realm location is the first argument, second one is the resource type uuid
            if (args.length === 2) {
                uuid = args[1];
            }

            if (uuid) {
                this.template =
                    "templates/admin/views/realms/authorization/resourceTypes/EditResourceTypeTemplate.html";
                this.model = new ResourceTypeModel({ uuid: uuid });
                this.listenTo(this.model, "sync", this.onModelSync);
                this.model.fetch();
            } else {
                this.template = "templates/admin/views/realms/authorization/resourceTypes/NewResourceTypeTemplate.html";
                this.newEntity = true;
                this.model = new ResourceTypeModel();
                this.listenTo(this.model, "sync", this.onModelSync);
                this.renderAfterSyncModel();
            }
        },

        renderAfterSyncModel: function () {
            var self = this,
                data = this.data;
            this.data.entity = _.cloneDeep(this.model.attributes);

            data.actions = [];
            _.each(this.data.entity.actions, function (v, k) {
                data.actions.push({ name: k, value: v });
            });
            data.actions.sort();

            this.initialActions = _.cloneDeep(data.actions);
            this.initialPatterns = _.cloneDeep(data.entity.patterns);

            this.parentRender(function () {
                var promises = [], resolve = function () { return (promises[promises.length] = $.Deferred()).resolve; },
                    data = self.data;

                self.$el.find(".tab-menu .nav-tabs").tabdrop();
                self.renderSettings();

                self.patternsView = new ResourceTypePatternsView();
                self.patternsView.render(data.entity, data.entity.patterns, "#resTypePatterns", resolve());

                self.actionsList = new ResourceTypeActionsView();
                self.actionsList.render(data, "#resTypeActions", resolve());

                $.when.apply($, promises).done(function () {
                    FormHelper.setActiveTab(self);
                    if (self.renderCallback) { self.renderCallback(); }
                });
            });
        },

        renderSettings: function () {
            var self = this;
            UIUtils.fillTemplateWithData(
                "templates/admin/views/realms/authorization/resourceTypes/ResourceTypeSettingsTemplate.html",
                this.data,
                function (tpl) {
                    self.$el.find("#resTypeSetting").html(tpl);
                });
        },

        updateFields: function () {
            var app = this.data.entity,
                dataFields = this.$el.find("[data-field]"),
                dataField;

            _.each(dataFields, function (field) {
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
                nonModifiedAttributes = _.clone(this.model.attributes),
                activeTab = this.$el.find(".tab-pane.active"),
                activeTabProperties;

            this.updateFields();
            this.activeTabId = this.$el.find(".tab-menu li.active a").attr("href");

            if (this.newEntity) {
                _.extend(this.model.attributes, this.data.entity);
            } else {
                activeTabProperties = _.pick(this.data.entity, this.tabs[activeTab.index()].attr);
                _.extend(this.model.attributes, activeTabProperties);
            }

            savePromise = this.model.save();

            if (savePromise) {
                savePromise
                    .done(function () {
                        if (self.newEntity) {
                            Router.routeTo(Router.configuration.routes.realmsResourceTypeEdit, {
                                args: _.map([self.data.realmPath, self.model.id], encodeURIComponent),
                                trigger: true
                            });
                        }

                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                    });
            } else {
                _.extend(this.model.attributes, nonModifiedAttributes);
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.validationError);
            }
        },

        onDeleteClick: function (e) {
            e.preventDefault();

            FormHelper.showConfirmationBeforeDeleting({ type: $.t("console.authorization.common.resourceType") },
                _.bind(this.deleteResourceType, this));
        },

        deleteResourceType: function () {
            var self = this,
                onSuccess = function () {
                    Router.routeTo(Router.configuration.routes.realmsResourceTypes, {
                        args: [encodeURIComponent(self.data.realmPath)],
                        trigger: true
                    });
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                },
                onError = function (model, response) {
                    Messages.addMessage({ response: response, type: Messages.TYPE_DANGER });
                };

            this.model.destroy({
                success: onSuccess,
                error: onError,
                wait: true
            });
        }
    });
});
