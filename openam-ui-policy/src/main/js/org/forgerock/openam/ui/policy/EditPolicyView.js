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
 * @author Aleanora Kaladzinskaya
 * @author Eugenia Sergueeva
 */

/*global window, define, $, form2js, _, js2form, document, console */

define("org/forgerock/openam/ui/policy/EditPolicyView", [
    "org/forgerock/openam/ui/policy/AbstractEditView",
    "org/forgerock/openam/ui/policy/ActionsView",
    "org/forgerock/openam/ui/policy/ResourcesListView",
    "org/forgerock/openam/ui/policy/AddNewResourceView",
    "org/forgerock/openam/ui/policy/ManageResponseAttrsView",
    "org/forgerock/openam/ui/policy/ResponseAttrsUserView",
    "org/forgerock/openam/ui/policy/ReviewInfoView",
    "org/forgerock/openam/ui/policy/PolicyDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/components/Accordion",
    "org/forgerock/openam/ui/policy/ManageSubjectsView",
    "org/forgerock/openam/ui/policy/ManageEnvironmentsView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/components/Messages"
], function (AbstractEditView, actionsView, resourcesListView, addNewResourceView, responseAttrsStaticView, responseAttrsUserView, reviewInfoView, policyDelegate,
             uiUtils, Accordion, manageSubjects, manageEnvironments, constants,eventManager, router, messager) {
    var EditPolicyView = AbstractEditView.extend({
        template: "templates/policy/EditPolicyTemplate.html",
        reviewTemplate: "templates/policy/ReviewPolicyStepTemplate.html",
        data: {},
        validationFields: ["name", "resources"],

        render: function (args, callback) {
            var self = this,
                data = self.data,
                appName = args[0],
                policyName = args[1],
                policyPromise = this.getPolicy(policyName),
                appPromise = policyDelegate.getApplicationByName(appName),
                allSubjectsPromise = policyDelegate.getSubjectConditions(),
                allEnvironmentsPromise = policyDelegate.getEnvironmentConditions(),
                allUserAttributesPromise = policyDelegate.getAllUserAttributes();

            $.when(policyPromise, appPromise, allSubjectsPromise, allEnvironmentsPromise, allUserAttributesPromise)
                .done(function (policy, app, allSubjects, allEnvironments, allUserAttributes) {
                var actions = [],
                    subjects = [],
                    conditions = [],
                    staticAttributes = [],
                    userAttributes = [];

                if (policyName) {
                    policy.actions = policy.actionValues;
                    data.entity = policy;
                    data.entityName = policyName;

                } else {
                    data.entity = {};
                    data.entityName = null;
                }
                _.each(app[0].actions, function (value, key) {
                    actions.push({action: key, selected: false, value: value});
                });

                // here we split by type
                staticAttributes =  _.where(policy.resourceAttributes, {type: responseAttrsStaticView.attrType});
                staticAttributes = responseAttrsStaticView.splitAttrs( staticAttributes);

                userAttributes = _.where(policy.resourceAttributes, {type: responseAttrsUserView.attrType});
                allUserAttributes = _.sortBy(allUserAttributes[0].result);

                data.entity.applicationName = appName;

                data.options = {};
                data.options.realm = app[0].realm;
                data.options.availableActions = _.sortBy(actions, "action");
                data.options.resourcePatterns = _.sortBy(app[0].resources);

                data.options.availableEnvironments = _.findByValues(allEnvironments[0].result, 'title', app[0].conditions);
                data.options.availableSubjects =     _.findByValues(allSubjects[0].result, 'title', app[0].subjects);

                self.parentRender(function () {

                    manageSubjects.render(data);
                    manageEnvironments.render(data);
                    actionsView.render(data);
                    addNewResourceView.render(data);
                    resourcesListView.render(data);
                    responseAttrsStaticView.render(staticAttributes);
                    responseAttrsUserView.render([userAttributes, allUserAttributes]);

                    self.prepareInfoReview();
                    self.validateThenRenderReview();
                    self.initAccordion();

                    if (callback) {
                        callback();
                    }

                });

            });
        },

        getPolicy: function (policyName) {
            var self = this,
                deferred = $.Deferred(),
                policy = {};

            if (policyName) {
                policyDelegate.getPolicyByName(policyName).done(function (policy) {
                    deferred.resolve(policy);
                });
            } else {
                deferred.resolve(policy);
            }
            return deferred.promise(policy);
        },

        updateFields: function () {
            var entity = this.data.entity,
                dataFields = this.$el.find('[data-field]');

            _.each(dataFields, function (field, key, list) {
                entity[field.getAttribute('data-field')] = field.value;
            });

            this.prepareInfoReview();
        },

        prepareInfoReview: function(){
            this.data.combinedStaticAttrs = responseAttrsStaticView.getCombinedAttrs();
            this.data.userAttrs =           responseAttrsUserView.getAttrs();
            this.data.responseAttrs =       this.data.combinedStaticAttrs.concat(this.data.userAttrs);
            this.data.subjectString =       JSON.stringify(this.data.entity.subject, null, 2);
            this.data.environmentString =   JSON.stringify(this.data.entity.condition, null, 2);
        },

        submitForm: function () {
            var policy = this.data.entity,
                persistedPolicy = _.clone(policy),
                self = this;

            persistedPolicy.actions = {};
            _.each(policy.actions, function (action) {
                if (action.selected) {
                    persistedPolicy.actions[action.action] = action.value;
                }
            });

            persistedPolicy.actionValues = persistedPolicy.actions;
            persistedPolicy.resourceAttributes = _.union( responseAttrsStaticView.getCombinedAttrs(), responseAttrsUserView.getAttrs());

            if (this.data.entityName) {
                policyDelegate.updatePolicy(this.data.entityName, persistedPolicy)
                .done( function (e) {
                    router.routeTo(router.configuration.routes.managePolicies, {args: [persistedPolicy.applicationName], trigger: true});
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyUpdated");
                })
                .fail( function(e) {
                    self.errorHandler(e);
                });

            } else {
                policyDelegate.createPolicy(persistedPolicy)
                .done( function (e) {
                    router.routeTo( router.configuration.routes.managePolicies, { args: [persistedPolicy.applicationName], trigger: true});
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyCreated");
                })
                .fail( function (e) {
                    self.errorHandler(e);
                });
            }
        },

        errorHandler: function (e) {

            var obj = { message: JSON.parse(e.responseText).message, type: "error"},
                invalidResourceText = "Invalid Resource";

            if( e.status === 500){

                if (uiUtils.responseMessageMatch( e.responseText,"Unable to persist policy")){
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unableToPersistPolicy");
                } else {
                    console.error(e.responseJSON, e.responseText, e);
                    messager.messages.addMessage( obj );
                }

            } else if (e.status === 400 || e.status === 404){

                if ( uiUtils.responseMessageMatch( e.responseText,invalidResourceText) ) {

                    this.data.options.invalidResource = obj.message.substr(invalidResourceText.length + 1);
                    reviewInfoView.render(this.data, null, this.$el.find('#reviewInfo'), this.reviewTemplate);
                    resourcesListView.render(this.data);
                    delete this.data.options.invalidResource;

                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidResource");

                } else if ( obj.message === "Policy " + this.data.entity.name + " already exists"  ) {

                    this.data.options.invalidName = true;
                    reviewInfoView.render(this.data, null, this.$el.find('#reviewInfo'), this.reviewTemplate);
                    delete this.data.options.invalidName;
                    messager.messages.addMessage( obj );

                } else {
                    console.log(e.responseJSON, e.responseText, e);
                    messager.messages.addMessage( obj );
                }

            } else {
                console.log(e.responseJSON, e.responseText, e);
                messager.messages.addMessage( obj );
            }
        }
        
    });

    return new EditPolicyView();
});
