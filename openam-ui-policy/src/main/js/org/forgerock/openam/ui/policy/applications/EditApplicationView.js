/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/**
 * @author Eugenia Sergueeva
 */

/*global window, define, $, _, document, console */
define("org/forgerock/openam/ui/policy/applications/EditApplicationView", [
    "org/forgerock/openam/ui/policy/common/AbstractEditView",
    "org/forgerock/openam/ui/policy/common/StripedListView",
    "org/forgerock/openam/ui/policy/common/ReviewInfoView",
    "org/forgerock/openam/ui/policy/delegates/PolicyDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/components/Accordion",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/components/Messages"
], function (AbstractEditView, StripedList, reviewInfoView, policyDelegate, uiUtils, Accordion, constants, conf, eventManager, messager) {
    var EditApplicationView = AbstractEditView.extend({
        template: "templates/policy/applications/EditApplicationTemplate.html",
        reviewTemplate: "templates/policy/applications/ReviewApplicationStepTemplate.html",
        data: {},
        APPLICATION_TYPE: "iPlanetAMWebAgentService",
        validationFields: ["name", "resourceTypeUuids"],

        render: function (args, callback) {

            var self = this,
                data = self.data,
                appName = args[0],
                appTypePromise = policyDelegate.getApplicationType(self.APPLICATION_TYPE),
                envConditionsPromise = policyDelegate.getEnvironmentConditions(),
                subjConditionsPromise = policyDelegate.getSubjectConditions(),
                decisionCombinersPromise = policyDelegate.getDecisionCombiners(),
                appPromise = this.getApplication(appName),
                resourceTypesPromise = policyDelegate.listResourceTypes();

            $.when(appTypePromise, envConditionsPromise, subjConditionsPromise, decisionCombinersPromise, resourceTypesPromise, appPromise).done(
                function (appType, envConditions, subjConditions, decisionCombiners, resourceTypes) {
                    if (!data.entity.applicationType) {
                        data.entity.applicationType = self.APPLICATION_TYPE;
                    }

                    if (!data.entity.realm) {
                        data.entity.realm = conf.globalData.auth.realm;
                    }

                    self.processConditions(data, envConditions[0].result, subjConditions[0].result);

                    data.entity.entitlementCombiner = self.getAvailableDecisionCombiner(decisionCombiners);

                    data.options = {};
                    data.options.allResourceTypes = resourceTypes[0].result;
                    data.options.availableResourceTypes = _.filter(resourceTypes[0].result, function (item) {
                        return !_.contains(self.data.entity.resourceTypeUuids, item.uuid);
                    });

                    self.parentRender(function () {
                        self.buildResourceTypesList();

                        self.validateThenRenderReview();
                        self.initAccordion();

                        if (callback) {
                            callback();
                        }
                    });
                });
        },

        buildResourceTypesList: function () {
            var availableNames = _.pluck(this.data.options.availableResourceTypes, 'name'),
                selected = _.findByValues(this.data.options.allResourceTypes, 'uuid', this.data.entity.resourceTypeUuids);

            this.data.options.selectedResourceTypeNames = _.pluck(selected, 'name').sort();

            this.resourceTypesListView = new StripedList();
            this.resourceTypesListView.render({
                items: availableNames,
                title: $.t('policy.resourceTypes.availableResourceTypes'),
                filter: true,
                clickItem: this.selectResourceType.bind(this)
            }, '#availableResTypes');

            this.resourceTypesListSelectedView = new StripedList();
            this.resourceTypesListSelectedView.render({
                items: this.data.options.selectedResourceTypeNames,
                title: $.t('policy.resourceTypes.selectedResourceTypes'),
                created: true,
                clickItem: this.deselectResourceType.bind(this)
            }, '#selectedResTypes');
        },

        selectResourceType: function (item) {
            this.moveSelected(item, this.resourceTypesListView, this.resourceTypesListSelectedView);

            // todo for now two RTs in the same realm are not allowed to have the same name, but the following should be changed to use UUIDs, not names
            var selected = _.findWhere(this.data.options.allResourceTypes, {name: item});
            this.data.entity.resourceTypeUuids = this.data.entity.resourceTypeUuids.concat(selected.uuid);
            this.data.options.selectedResourceTypeNames = this.data.options.selectedResourceTypeNames.concat(selected.name).sort();

        },

        deselectResourceType: function (item) {
            this.resourceTypesListView.emptyFilter();
            this.moveSelected(item, this.resourceTypesListSelectedView, this.resourceTypesListView);

            // todo for now two RTs in the same realm are not allowed to have the same name, but the following should be changed to use UUIDs, not names
            var selected = _.findWhere(this.data.options.allResourceTypes, {name: item});
            this.data.entity.resourceTypeUuids = _.without(this.data.entity.resourceTypeUuids, selected.uuid);
            this.data.options.selectedResourceTypeNames = _.without(this.data.options.selectedResourceTypeNames, selected.name);
        },

        moveSelected: function (item, fromView, toView) {
            fromView.removeItem(item);
            fromView.renderItems();

            toView.addItem(item);
            toView.renderItems();
        },

        getApplication: function (appName) {
            var self = this,
                deferred = $.Deferred();

            if (appName) {
                policyDelegate.getApplicationByName(appName)
                .done(function (app) {
                    self.data.entity = app;
                    self.data.entityName = appName;
                    deferred.resolve();
                });
            } else {
                self.data.entity = {};
                self.data.entity.resourceTypeUuids = [];
                self.data.entityName = null;
                deferred.resolve();
            }

            return deferred.promise();
        },

        getAvailableDecisionCombiner: function (decisionCombiners) {
            // Only one decision combiner is available in the system.
            return decisionCombiners[0].result[0].title;
        },

        processConditions: function (data, envConditions, subjConditions) {
            if (!data.entityName){
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

        updateFields: function () {
            var app = this.data.entity,
                dataFields = this.$el.find('[data-field]'),
                dataField;

            _.each(dataFields, function (field, key, list) {
                dataField = field.getAttribute('data-field');

                if (field.type === 'checkbox') {
                    if (field.checked) {
                        app[dataField].push(field.value);
                    }
                } else {
                    app[dataField] = field.value;
                }
            });
        },

        submitForm: function () {
            var app = this.data.entity,
                persistedApp = _.clone(app),
                self = this;

            if (this.data.entityName) {
                policyDelegate.updateApplication( this.data.entityName, persistedApp )
                .done(function (e) {
                    eventManager.sendEvent(constants.EVENT_HANDLE_DEFAULT_ROUTE);
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "applicationUpdated");
                })
                .fail(function (e) {
                    self.errorHandler(e);
                });
            } else {
                policyDelegate.createApplication(persistedApp)
                .done(function (e) {
                    console.log(e);
                    eventManager.sendEvent(constants.EVENT_HANDLE_DEFAULT_ROUTE);
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "applicationCreated");
                })
                .fail(function (e) {
                    self.errorHandler(e);
                });
            }
        },
        errorHandler : function (e) {

            var obj = { message: JSON.parse(e.responseText).message, type: "error"},
                invalidResourceText = "Invalid Resource";

            if (e.status === 500) {
                console.error(e.responseJSON, e.responseText, e);
                messager.messages.addMessage(obj);
            } else if (e.status === 400 || e.status === 404) {

                if ( uiUtils.responseMessageMatch( e.responseText, invalidResourceText) ) {
                    this.data.options.invalidResource = obj.message.substr(invalidResourceText.length + 1);
                    reviewInfoView.render(this.data, null, this.$el.find('#reviewInfo'), 'templates/policy/applications/ReviewApplicationStepTemplate.html');
                    delete this.data.options.invalidResource;
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidResource");

                } else {
                    console.log(e.responseJSON, e.responseText, e);
                    messager.messages.addMessage(obj);
                }
            } else if (e.status === 409) {
                // duplicate name
                this.data.options.invalidName = true;
                reviewInfoView.render(this.data, null, this.$el.find('#reviewInfo'), "templates/policy/applications/ReviewApplicationStepTemplate.html");
                delete this.data.options.invalidName;
                messager.messages.addMessage(obj);

            } else {
                console.log(e.responseJSON, e.responseText, e);
                messager.messages.addMessage(obj);
            }
        }
    });

    return new EditApplicationView();
});