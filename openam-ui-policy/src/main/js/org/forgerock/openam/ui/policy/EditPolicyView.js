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
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/policy/ActionsView",
    "org/forgerock/openam/ui/policy/ResourcesListView",
    "org/forgerock/openam/ui/policy/AddNewResourceView",
    "org/forgerock/openam/ui/policy/ReviewInfoView",
    "org/forgerock/openam/ui/policy/PolicyDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/components/Accordion",
    "org/forgerock/openam/ui/policy/ManageSubjectsView",
    "org/forgerock/openam/ui/policy/ManageEnvironmentsView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router"
], function (AbstractView, actionsView, resourcesListView, addNewResourceView, reviewInfoView, policyDelegate, uiUtils, Accordion, manageSubjects, manageEnvironments, constants, eventManager, router) {
    var EditPolicyView = AbstractView.extend({
        baseTemplate: "templates/policy/BaseTemplate.html",
        template: "templates/policy/EditPolicyTemplate.html",
        events: {
            'click input[name=nextButton]': 'openNextStep',
            'click input[name=submitForm]': 'submitForm',
            'click .review-row': 'reviewRowClick',
            'keyup .review-row': 'reviewRowClick'
        },
        data:{},

        REVIEW_INFO_STEP: 5,

        render: function (args, callback) {
            var self = this,
                data = self.data,
                appName = decodeURI(args[0]),
                policyName = args[1],
                policyPromise = this.getPolicy(policyName),
                appPromise = policyDelegate.getApplicationByName(appName),
                allSubjectsPromise = policyDelegate.getSubjectConditions(), // this possibly should be in the parent. We need a means to check if this exsists, and only make this searxh if it does not
                allEnvironmentsPromise = policyDelegate.getEnvironmentConditions();

            $.when(policyPromise, appPromise, allSubjectsPromise, allEnvironmentsPromise).done(function (policy, app, allSubjects, allEnvironments) {
                var actions = [],
                    subjects = [],
                    environments = [];

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

                _.each(allSubjects[0].result, function (value) {
                   if ( _.contains(app[0].subjects, value.title) ){
                       subjects.push(value);
                   }
                });

                _.each(allEnvironments[0].result, function (value) {
                   if ( _.contains(app[0].conditions, value.title) ){
                       environments.push(value);
                   }
                });

                data.options = {};

                data.options.availableSubjects = subjects;
                data.options.availableEnvironments = environments;
                data.options.availableActions = actions;
                data.options.resourcePatterns = app[0].resources;

                data.entity.applicationName = appName;
                self.parentRender(function () {
                    manageSubjects.render(data);
                    manageEnvironments.render(data);
                    actionsView.render(data);
                    resourcesListView.render(data);
                    addNewResourceView.render(data);

                    data.subjectString = JSON.stringify(data.entity.subject, null, 2);
                    data.environmentString = JSON.stringify(data.entity.condition, null, 2);

                    reviewInfoView.render(data, null, self.$el.find('#reviewPolicyInfo'), "templates/policy/ReviewPolicyStepTemplate.html");

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

        /**
         * Initializes accordion.
         */
        initAccordion: function () {
            var self = this,
                options = {};

            if (this.data.entity.name) {
                options.active = this.REVIEW_INFO_STEP;
            } else {
                options.disabled = true;
            }

            this.accordion = new Accordion(this.$el.find('.accordion'), options);

            this.accordion.on('beforeChange', function (e, id) {
                if (id === self.REVIEW_INFO_STEP) {
                    self.updateFields();
                    reviewInfoView.render(self.data, null, self.$el.find('#reviewPolicyInfo'), "templates/policy/ReviewPolicyStepTemplate.html");
                }
            });
        },

        updateFields: function () {
            var entity = this.data.entity,
                dataFields = this.$el.find('[data-field]'),
                field;

            _.each(dataFields, function (field, key, list) {
                entity[field.getAttribute('data-field')] = field.value;
            });

            this.data.subjectString = JSON.stringify(this.data.entity.subject, null, 2);
            this.data.environmentString = JSON.stringify(this.data.entity.condition, null, 2);
        },

        /**
         * Opens next accordion step.
         * TODO: some validation probably will be done here
         */
        openNextStep: function (e) {
            this.accordion.setActive(this.accordion.getActive() + 1);
        },

        reviewRowClick:function (e) {
            if (e.type === 'keyup' && e.keyCode !== 13) { return;}
            var reviewRows = this.$el.find('.review-row'),
                targetIndex = -1;
                _.find(reviewRows, function(reviewRow, index){
                    if(reviewRow === e.currentTarget){
                        targetIndex = index;
                    }
                });

            this.accordion.setActive(targetIndex);
        },

        submitForm: function () {
            var policy = this.data.entity,
                persistedPolicy = _.clone(policy);

            persistedPolicy.actions = {};

            _.each(policy.actions, function (action) {
                if (action.selected) {
                    persistedPolicy.actions[action.action] = action.value;
                }
            });

            persistedPolicy.actionValues = persistedPolicy.actions;

            if (this.data.entityName) {
                policyDelegate.updatePolicy(this.data.entityName, persistedPolicy).done(function () {
                    router.routeTo(router.configuration.routes.managePolicies, {args: [encodeURI(persistedPolicy.applicationName)], trigger: true});
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyUpdated");
                });
            } else {
                policyDelegate.createPolicy(persistedPolicy).done(function () {
                    router.routeTo(router.configuration.routes.managePolicies, {args: [encodeURI(persistedPolicy.applicationName)], trigger: true});
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyCreated");
                });
            }
        }
    });


    return new EditPolicyView();
});
