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
                conditionTypesPromise = policyDelegate.getConditionTypes(),
                appPromise = this.getApplication(appName);

            this.processApplicationTypes(appTypesPromise);
            this.processConditionTypes(conditionTypesPromise);

            $.when(appTypesPromise, conditionTypesPromise, appPromise).done(function (appTypes, conditionTypes, app) {
                if (!data.app.applicationType) {
                    data.app.applicationType = _.keys(data.appTypes)[0];
                }

                self.parentRender(function () {
                    actionsView.render(data);
                    resourcesListView.render(data);
                    addNewResourceView.render(data);
                    reviewInfoView.render(data);

                    self.initAccordion();
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

        /**
         * Processes response to form list of available condition types.
         *
         * @param conditionTypesPromise service response
         */
        //TODO: not used, as the API is not ready
        processConditionTypes: function (conditionTypesPromise) {
            var self = this;
            conditionTypesPromise.done(function (resp) {
                self.data.conditionTypes = resp.result;
            });
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
                field;

            _.each(dataFields, function (field, key, list) {
                app[field.getAttribute('data-field')] = field.value;
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
