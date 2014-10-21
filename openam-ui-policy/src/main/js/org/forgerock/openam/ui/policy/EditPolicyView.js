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
    "org/forgerock/commons/ui/common/main/Router"
], function (AbstractView, actionsView, resourcesListView, addNewResourceView, responseAttrsStaticView, responseAttrsUserView, reviewInfoView, policyDelegate, uiUtils, Accordion, manageSubjects, manageEnvironments, constants,eventManager, router) {
    var EditPolicyView = AbstractView.extend({
        baseTemplate: "templates/policy/BaseTemplate.html",
        template: "templates/policy/EditPolicyTemplate.html",
        events: {
            'click input[name=nextButton]': 'openNextStep',
            'click input[name=submitForm]': 'submitForm',
            'click .review-row': 'reviewRowClick',
            'keyup .review-row': 'reviewRowClick'
        },
        data: {},

        REVIEW_INFO_STEP: 6,

        render: function (args, callback) {
            var self = this,
                data = self.data,
                appName = args[0],
                policyName = args[1],
                policyPromise = this.getPolicy(policyName),
                appPromise = policyDelegate.getApplicationByName(appName),
                allSubjectsPromise = policyDelegate.getSubjectConditions(), // this possibly should be in the parent. We need a means to check if this exsists, and only make this searxh if it does not
                allEnvironmentsPromise = policyDelegate.getEnvironmentConditions(),
                allUserAttributesPromise = policyDelegate.getAllUserAttributes(),
                identitySubjectUsersPromise  = policyDelegate.getAllIdentity("users"),
                identitySubjectGroupsPromise = policyDelegate.getAllIdentity("groups");

            $.when(policyPromise, appPromise, allSubjectsPromise, allEnvironmentsPromise, allUserAttributesPromise, identitySubjectUsersPromise, identitySubjectGroupsPromise)
                .done(function (policy, app, allSubjects, allEnvironments, allUserAttributes, identitySubjectUsers, identitySubjectGroups) {
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
                data.options.availableActions = _.sortBy(actions, "action");
                data.options.resourcePatterns = _.sortBy(app[0].resources);

                // this is a temporary hack... not to be committed, to get round the fact that the default apps have differnt condition types than the endpoints.
                data.options.availableEnvironments = allEnvironments[0].result; //_.findByValues(allEnvironments[0].result, 'title', app[0].conditions); 
                data.options.availableSubjects =     allSubjects[0].result; //_.findByValues(allSubjects[0].result, 'title', app[0].subjects);
           
                data.options.availableSubjects = _.union(data.options.availableSubjects, data.options.availableEnvironments);

                self.identityFix(data.options.availableSubjects,identitySubjectUsers,identitySubjectGroups);

                self.parentRender(function () {

                    manageSubjects.render(data);
                    manageEnvironments.render(data);
                    actionsView.render(data);
                    addNewResourceView.render(data);
                    resourcesListView.render(data);
                    responseAttrsStaticView.render(staticAttributes); 
                    responseAttrsUserView.render([userAttributes, allUserAttributes]);

                    self.prepareInfoReview();
                    reviewInfoView.render(this.data, null, self.$el.find('#reviewPolicyInfo'), "templates/policy/ReviewPolicyStepTemplate.html");
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

            this.prepareInfoReview();
        },

        prepareInfoReview: function(){
            this.data.combinedStaticAttrs = responseAttrsStaticView.getCombinedAttrs();
            this.data.userAttrs =           responseAttrsUserView.getAttrs();
            this.data.subjectString =       JSON.stringify(this.data.entity.subject, null, 2);
            this.data.environmentString =   JSON.stringify(this.data.entity.condition, null, 2);
        },

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
                    console.log(e);
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
            
            if( e.status === 500){

                if (uiUtils.responseMessageMatch( e.responseText,"Unable to persist policy")){
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unableToPersistPolicy");
                } else{
                    console.error(e.responseJSON, e.responseText, e);
                }
                
            } else if (e.status === 400 || e.status === 404){

                if ( uiUtils.responseMessageMatch( e.responseText,"Invalid Resource") ) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidResource");
              
                    var message = JSON.parse(e.responseText).message;
                    this.data.options.invalidResource = message.substr(17);
                    reviewInfoView.render(this.data, null, this.$el.find('#reviewPolicyInfo'), "templates/policy/ReviewPolicyStepTemplate.html");
                    resourcesListView.render(this.data);
                    delete this.data.options.invalidResource;

                } else {
                    console.log(e.responseJSON, e.responseText, e);
                }
                
            } else {
                console.log(e.responseJSON, e.responseText, e);
            }
        },

        identityFix: function (availableSubjects,identitySubjectUsers,identitySubjectGroups) {
            // this is a temporary client-side fix as the endpoint as yet does not return any information about the available lookups
            var identity = _.find(availableSubjects, function(item){
                    return item.title === "Identity";
                });
            if (identity.config.properties.subjectValues) {
                identity.config.properties.subjectValues.dataSources = [
                    {name:"users", data:identitySubjectUsers[0].result},
                    {name:"groups", data:identitySubjectGroups[0].result},
                ];
        }
                        
        }                
    });

    return new EditPolicyView();
});