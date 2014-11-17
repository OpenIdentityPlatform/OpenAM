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
    "org/forgerock/openam/ui/policy/AbstractEditView",
    "org/forgerock/openam/ui/policy/ResourcesListView",
    "org/forgerock/openam/ui/policy/AddNewResourceView",
    "org/forgerock/openam/ui/policy/ReviewInfoView",
    "org/forgerock/openam/ui/policy/PolicyDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/components/Accordion",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/components/Messages"
], function (AbstractEditView, resourcesListView, addNewResourceView, reviewInfoView, policyDelegate, uiUtils, Accordion, constants, conf, eventManager, messager) {
    var EditApplicationView = AbstractEditView.extend({
        template: "templates/policy/EditApplicationTemplate.html",
        reviewTemplate: "templates/policy/ReviewApplicationStepTemplate.html",
        data: {},
        APPLICATION_TYPE: "iPlanetAMWebAgentService",
        validationFields: ["name", "resources"],

        render: function (args, callback) {

            var self = this,
                data = self.data,
                appName = args[0],
                appTypePromise = policyDelegate.getApplicationType(self.APPLICATION_TYPE),
                envConditionsPromise = policyDelegate.getEnvironmentConditions(),
                subjConditionsPromise = policyDelegate.getSubjectConditions(),
                decisionCombinersPromise = policyDelegate.getDecisionCombiners(),
                appPromise = this.getApplication(appName);

            $.when(appTypePromise, envConditionsPromise, subjConditionsPromise, decisionCombinersPromise, appPromise).done(
                function (appType, envConditions, subjConditions, decisionCombiners) {
                    if (!data.entity.applicationType) {
                        data.entity.applicationType = self.APPLICATION_TYPE;
                    }

                    if (!data.entity.realm) {
                        data.entity.realm = conf.globalData.auth.realm;
                    }

                    self.processConditions(data, envConditions[0].result, subjConditions[0].result);

                    data.options = {};
                    data.entity.entitlementCombiner = self.getAvailableDecisionCombiner(decisionCombiners);

                    // Available resource patterns are supposed to be defined by the selected application type. For now we
                    // assume any resource might be created, hence we hard code the '*'.
                    data.options.resourcePatterns = ['*'];

                    self.parentRender(function () {
                        resourcesListView.render(data);
                        addNewResourceView.render(data);

                        self.validateThenRenderReview();
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
                policyDelegate.getApplicationByName(appName)
                .done(function (app) {
                    self.data.entity = app;
                    self.data.entityName = appName;
                    deferred.resolve();
                });
            } else {
                self.data.entity = {};
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
               messager.messages.addMessage( obj );
            } else if (e.status === 400 || e.status === 404) {

                if ( uiUtils.responseMessageMatch( e.responseText, invalidResourceText) ) {
                    this.data.options.invalidResource = obj.message.substr(invalidResourceText.length + 1);
                    reviewInfoView.render(this.data, null, this.$el.find('#reviewInfo'), 'templates/policy/ReviewApplicationStepTemplate.html');
                    delete this.data.options.invalidResource;
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidResource");

                } else {
                    console.log(e.responseJSON, e.responseText, e);
                    messager.messages.addMessage( obj );
                }
            } else if (e.status === 409) {
                // duplicate name
                this.data.options.invalidName = true;
                reviewInfoView.render(this.data, null, this.$el.find('#reviewInfo'), "templates/policy/ReviewApplicationStepTemplate.html");
                delete this.data.options.invalidName;
                 messager.messages.addMessage( obj );

            } else {
                console.log(e.responseJSON, e.responseText, e);
                messager.messages.addMessage( obj );
            }
        }
    });

    return new EditApplicationView();
});
