/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global window, define, $, form2js, _, js2form, document, console */
define("org/forgerock/openam/ui/policy/EditApplicationView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/policy/ActionsView",
    "org/forgerock/openam/ui/policy/ResourcesListView",
    "org/forgerock/openam/ui/policy/AddNewResourceView",
    "org/forgerock/openam/ui/policy/ReviewApplicationInfoView",
    "org/forgerock/openam/ui/policy/PolicyDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/components/Accordion",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager"
], function (AbstractView, actionsView, resourcesListView, addNewResourceView, reviewInfoView, policyDelegate, uiUtils, Accordion, constants, eventManager) {
    var EditApplicationView = AbstractView.extend({
        baseTemplate: "templates/policy/BaseTemplate.html",
        template: "templates/policy/EditApplicationTemplate.html",
        events: {
            'change #appType': 'handleAppTypeChange',
            'click input[name=nextButton]': 'openNextStep',
            'click input[name=submitForm]': 'submitForm'
        },
        data: {},
        REVIEW_INFO_STEP: 6,

        render: function (args, callback) {
            var self = this,
                data = self.data,
                appName = args[0],
                appTypesPromise = policyDelegate.getApplicationTypes(),
                envConditionsPromise = policyDelegate.getEnvironmentConditions(),
                subjConditionsPromise = policyDelegate.getSubjectConditions(),
                decisionCombinersPromise = policyDelegate.getDecisionCombiners(),
                appPromise = this.getApplication(appName);

            this.processApplicationTypes(appTypesPromise);

            $.when(appTypesPromise, envConditionsPromise, subjConditionsPromise, decisionCombinersPromise, appPromise).done(
                function (appTypes, envConditions, subjConditions, decisionCombiners) {
                    if (!data.app.applicationType) {
                        data.app.applicationType = _.keys(data.appTypes)[0];
                    }

                    self.processConditions(data, envConditions[0].result, subjConditions[0].result);

                    data.app.entitlementCombiner = self.getAvailableDecisionCombiner(decisionCombiners);

                    // Available resource patterns are supposed to be defined by the selected application type. For now we
                    // assume any resource might be created, hence we hard code the '*'.
                    data.app.resourcePatterns = ['*'];

                    self.parentRender(function () {
                        actionsView.render(data);
                        resourcesListView.render(data);
                        addNewResourceView.render(data);
                        reviewInfoView.render(data);

                        self.initAccordion();

                        if (callback) {
                            callback();
                        }
                    });
                });
        },

        /**
         * Retrieves application in case it's an existing application,
         * provides an empty object if it is a new application.
         *
         * @param appName application name
         * @returns {Object} application
         */
        getApplication: function (appName) {
            var self = this,
                deferred = $.Deferred();

            if (appName) {
                policyDelegate.getApplicationByName(appName).done(function (app) {
                    self.data.app = app;
                    self.data.appName = appName;

                    deferred.resolve();
                });
            } else {
                self.data.app = {};
                self.data.appName = null;

                deferred.resolve();
            }

            return deferred.promise();
        },

        /**
         * Processes response to form lists of all application types and map of available actions for the corresponding
         * application types.
         *
         * @param appTypesPromise service response
         */
        processApplicationTypes: function (appTypesPromise) {
            var self = this;
            appTypesPromise.done(function (resp) {
                var appTypesResult = resp.result,
                    appTypes = {},
                    typeActions = {};

                _.each(appTypesResult, function (type) {
                    appTypes[type.name] = type.name;

                    var actions = [];
                    _.each(type.actions, function (value, key, list) {
                        actions.push({action: key, selected: false, value: value});
                    });
                    typeActions[type.name] = actions;
                });

                self.data.appTypes = appTypes;
                self.data.typeActions = typeActions;
            });
        },

        getAvailableDecisionCombiner: function (decisionCombiners) {
            // Only one decision combiner is available in the system.
            return decisionCombiners[0].result[0].title;
        },

        processConditions: function (data, envConditions, subjConditions) {
            data.envConditions = this.populateConditions(data.app.conditions, envConditions);
            data.subjConditions = this.populateConditions(data.app.subjects, subjConditions);
        },

        populateConditions: function (selected, available) {
            var result = [];
            _.each(available, function (cond) {
                result.push({
                    name: cond.title,
                    logical: cond.logical,
                    selected: _.contains(selected, cond.title)});
            });
            return result;
        },

        /**
         * Retrieves available actions for the selected application type.
         */
        handleAppTypeChange: function (e) {
            this.data.app.applicationType = e.target.value;
            this.data.app.actions = [];

            actionsView.render(this.data);
        },

        /**
         * Initializes accordion.
         */
        initAccordion: function () {
            var self = this,
                options = {};

            if (this.data.app.name) {
                options.active = this.REVIEW_INFO_STEP;
            } else {
                options.disabled = true;
            }

            this.accordion = new Accordion(this.$el.find('.accordion'), options);

            this.accordion.on('beforeChange', function (e, id) {
                if (id === self.REVIEW_INFO_STEP) {
                    self.updateFields();
                    reviewInfoView.render(self.data);
                }
            });
        },

        updateFields: function () {
            var app = this.data.app,
                dataFields = this.$el.find('[data-field]'),
                dataField;

            app.subjects = [];
            app.conditions = [];

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

        /**
         * Opens next accordion step.
         * TODO: some validation probably will be done here
         */
        openNextStep: function (e) {
            this.accordion.setActive(this.accordion.getActive() + 1);
        },

        submitForm: function () {
            var app = this.data.app,
                persistedApp = _.clone(app);

            persistedApp.actions = {};

            _.each(app.actions, function (action) {
                if (action.selected) {
                    persistedApp.actions[action.action] = action.value;
                }
            });

            if (this.data.appName) {
                policyDelegate.updateApplication(this.data.appName, persistedApp).done(function () {
                    eventManager.sendEvent(constants.EVENT_HANDLE_DEFAULT_ROUTE);
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "applicationUpdated");
                });
            } else {
                policyDelegate.createApplication(persistedApp).done(function () {
                    eventManager.sendEvent(constants.EVENT_HANDLE_DEFAULT_ROUTE);
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "applicationCreated");
                });
            }
        }
    });

    return new EditApplicationView();
});
