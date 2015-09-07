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
 * Portions copyright 2014-2015 ForgeRock AS.
 */

/*global define*/

define("org/forgerock/openam/ui/admin/views/realms/authorization/policySets/EditPolicySetView", [
    "jquery",
    "underscore",
    "org/forgerock/openam/ui/admin/models/authorization/PolicySetModel",
    "org/forgerock/openam/ui/admin/views/realms/authorization/common/StripedListView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/PoliciesView",
    "org/forgerock/openam/ui/admin/delegates/PoliciesDelegate",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, _, PolicySetModel, StripedListView, PoliciesView, PoliciesDelegate, Messages, AbstractView,
             EventManager, Router, Constants, UIUtils) {
    return AbstractView.extend({
        partials: [
            "templates/admin/views/realms/partials/_HeaderDeleteButton.html"
        ],
        APPLICATION_TYPE: "iPlanetAMWebAgentService",
        validationFields: ["name", "resourceTypeUuids"],
        events: {
            "click #saveChanges": "submitForm",
            "click #revertChanges": "revertChanges",
            "click #delete": "deletePolicySet"
        },

        initialize: function (options) {
            AbstractView.prototype.initialize.call(this);
            this.model = null;
        },

        onModelSync: function (model, response) {
            this.renderAfterSyncModel();
        },

        render: function (args, callback) {
            var name = null;

            this.realmPath = args[0];

            if (callback) {
                this.renderCallback = callback;
            }

            // Realm location is the first argument, second one is the application name
            if (args.length === 2) {
                name = args[1];
            }

            this.appTypePromise = PoliciesDelegate.getApplicationType(this.APPLICATION_TYPE);
            this.envConditionsPromise = PoliciesDelegate.getEnvironmentConditions();
            this.subjConditionsPromise = PoliciesDelegate.getSubjectConditions();
            this.decisionCombinersPromise = PoliciesDelegate.getDecisionCombiners();
            this.resourceTypesPromise = PoliciesDelegate.listResourceTypes();

            if (name) {
                this.template = "templates/admin/views/realms/authorization/policySets/EditPolicySetTemplate.html";
                this.model = new PolicySetModel({name: name});
                this.listenTo(this.model, "sync", this.onModelSync);
                this.model.fetch();
            } else {
                this.newEntity = true;
                this.template = "templates/admin/views/realms/authorization/policySets/NewPolicySetTemplate.html";
                this.model = new PolicySetModel();
                this.listenTo(this.model, "sync", this.onModelSync);
                this.renderAfterSyncModel();
            }
        },

        renderAfterSyncModel: function () {
            this.data.entity = this.model.attributes;

            if (!this.data.entity.realm) {
                this.data.entity.realm = this.realmPath;
            }

            this.renderApplication();
        },

        renderApplication: function () {
            var self = this,
                parentRenderCallback = function () {
                    self.parentRender(function () {
                        PoliciesView.render({
                            realmPath: self.realmPath,
                            applicationModel: self.model
                        }, function (policiesNumber) {
                            if (policiesNumber > 0) {
                                self.data.disableSettingsEdit = true;
                                self.$el.find("#saveChanges, #revertChanges, #delete").attr("disabled", true);
                            }

                            UIUtils.fillTemplateWithData(
                                "templates/admin/views/realms/authorization/policySets/PolicySetSettingsTemplate.html",
                                self.data, function (tpl) {
                                    self.$el.find("#policySetSettings").append(tpl);
                                    self.populateResourceTypes();

                                    if (self.renderCallback) {
                                        self.renderCallback();
                                    }
                                });
                        });
                    });
                },
                populateAvailableResourceTypes = function (resourceTypes) {
                    var options = {};

                    options.allResourceTypes = resourceTypes;
                    options.availableResourceTypes = _.filter(resourceTypes, function (item) {
                        return !_.contains(self.data.entity.resourceTypeUuids, item.uuid);
                    });

                    options.selectedResourceTypes = _.findByValues(options.allResourceTypes, "uuid",
                        self.data.entity.resourceTypeUuids);

                    options.selectedResourceTypesInitial = _.clone(options.selectedResourceTypes);

                    return options;
                };

            if (!this.model.id) {
                // Fill in the necessary information about application
                $.when(this.appTypePromise, this.envConditionsPromise, this.subjConditionsPromise,
                        this.decisionCombinersPromise, this.resourceTypesPromise)
                    .done(function (appType, envConditions, subjConditions, decisionCombiners, resourceTypes) {
                        self.data.entity.applicationType = self.APPLICATION_TYPE;
                        self.processConditions(self.data, envConditions[0].result, subjConditions[0].result);
                        self.data.entity.entitlementCombiner = decisionCombiners[0].result[0].title;
                        _.extend(self.data, {options: populateAvailableResourceTypes(resourceTypes[0].result)});
                        parentRenderCallback();
                    });
            } else {
                this.resourceTypesPromise.done(function (resourceTypes) {
                    _.extend(self.data, {options: populateAvailableResourceTypes(resourceTypes.result)});
                    parentRenderCallback();
                });
            }
        },

        populateResourceTypes: function () {
            var self = this;

            this.resTypesSelection = this.$el.find("#resTypesSelection").selectize({
                sortField: "name",
                valueField: "uuid",
                labelField: "name",
                searchField: "name",
                options: this.data.options.allResourceTypes,
                onChange: function (value) {
                    self.data.entity.resourceTypeUuids = value;
                }
            });

            if (this.data.disableSettingsEdit) {
                this.resTypesSelection[0].selectize.disable();
            }
        },

        processConditions: function (data, envConditions, subjConditions) {
            if (!data.entityName) {
                data.entity.conditions = this.populateConditions(envConditions, envConditions);
                data.entity.subjects = this.populateConditions(subjConditions, subjConditions);
            }
        },

        populateConditions: function (selected, available) {
            var result = [];
            _.each(available, function (cond) {
                result.push(cond.title);
            });
            return result;
        },

        revertChanges: function (e) {
            this.resTypesSelection[0].selectize.clear(true);
            this.resTypesSelection[0].selectize.addItems(
                _.pluck(this.data.options.selectedResourceTypesInitial, "uuid"));
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
                        if (self.newEntity) {
                            Router.routeTo(Router.configuration.routes.realmsPolicySetEdit, {
                                args: [encodeURIComponent(self.realmPath), self.model.id],
                                trigger: true
                            });
                        }

                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                    })
                    .fail(function (response) {
                        _.extend(self.model.attributes, nonModifiedAttributes);
                        Messages.addMessage({
                            message: response.responseJSON.message,
                            type: Messages.TYPE_DANGER
                        });
                    });
            } else {
                _.extend(this.model.attributes, nonModifiedAttributes);
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.validationError);
            }
        },

        deletePolicySet: function (e) {
            e.preventDefault();

            var self = this,
                onSuccess = function (model, response, options) {
                    Router.routeTo(Router.configuration.routes.realmsPolicySets, {
                        args: [encodeURIComponent(self.realmPath)],
                        trigger: true
                    });
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                },
                onError = function (model, response, options) {
                    Messages.addMessage({
                        message: response.responseJSON.message,
                        type: Messages.TYPE_DANGER
                    });
                };

            this.model.destroy({
                success: onSuccess,
                error: onError
            });
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
        }
    });
});