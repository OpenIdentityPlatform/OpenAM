/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014-2015 ForgeRock AS.
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

/*global window, define, $, _, console */

define("org/forgerock/openam/ui/policy/policies/EditPolicyView", [
    "org/forgerock/openam/ui/policy/common/AbstractEditView",
    "org/forgerock/openam/ui/policy/common/ReviewInfoView",
    "org/forgerock/openam/ui/policy/common/StripedListView",
    "org/forgerock/openam/ui/policy/delegates/PolicyDelegate",
    "org/forgerock/openam/ui/policy/policies/PolicyActionsView",
    "org/forgerock/openam/ui/policy/policies/ResourcesView",
    "org/forgerock/openam/ui/policy/policies/attributes/StaticResponseAttributesView",
    "org/forgerock/openam/ui/policy/policies/attributes/SubjectResponseAttributesView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/policy/policies/conditions/ManageSubjectsView",
    "org/forgerock/openam/ui/policy/policies/conditions/ManageEnvironmentsView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router"
], function (AbstractEditView, ReviewInfoView, StripedList, PolicyDelegate, PolicyActionsView, ResourcesView,
             StaticResponseAttributesView, SubjectResponseAttributesView, UIUtils, Constants, Messages,
             ManageSubjectsView, ManageEnvironmentsView, EventManager, Router) {

    var EditPolicyView = AbstractEditView.extend({
        template: "templates/policy/policies/EditPolicyTemplate.html",
        reviewTemplate: "templates/policy/policies/ReviewPolicyStepTemplate.html",
        validationFields: ["name", "resources"],

        render: function (args, callback) {
            var self = this,
                data = self.data,
                appName =                   args[0],
                policyName =                args[1],
                policyPromise =             this.getPolicy(policyName),
                appPromise =                PolicyDelegate.getApplicationByName(appName),
                allSubjectsPromise =        PolicyDelegate.getSubjectConditions(),
                allEnvironmentsPromise =    PolicyDelegate.getEnvironmentConditions(),
                allUserAttributesPromise =  PolicyDelegate.getAllUserAttributes(),
                resourceTypesPromise =      PolicyDelegate.listResourceTypes();

            this.events['change #availableResTypes'] = this.changeResourceType;

            $.when(policyPromise, appPromise, allSubjectsPromise, allEnvironmentsPromise, allUserAttributesPromise, resourceTypesPromise)
                .done(function (policy, app, allSubjects, allEnvironments, allUserAttributes, resourceTypes) {
                var staticAttributes = [],
                    userAttributes = [],
                    availableResourceTypes,
                    resourceType;

                if (policyName) {
                    data.entity = policy;
                    data.entityName = policyName;
                } else {
                    data.entity = {};
                    data.entityName = null;
                }

                staticAttributes = _.where(policy.resourceAttributes, {type: "Static"});
                userAttributes = _.where(policy.resourceAttributes, {type: "User"});

                allUserAttributes = _.sortBy(allUserAttributes[0].result);

                data.entity.applicationName = appName;

                data.options = {};
                data.options.realm = app[0].realm;

                data.options.availableEnvironments = _.findByValues(allEnvironments[0].result, 'title', app[0].conditions);
                data.options.availableSubjects =     _.findByValues(allSubjects[0].result, 'title', app[0].subjects);

                availableResourceTypes = _.filter(resourceTypes[0].result, function (item) {
                    return _.contains(app[0].resourceTypeUuids, item.uuid);
                });
                data.options.availableResourceTypes = availableResourceTypes;

                if (policy.resourceTypeUuid) {
                    resourceType = _.findWhere(data.options.availableResourceTypes, {uuid: policy.resourceTypeUuid});

                    data.options.availableActions = self.getAvailableActionsForResourceType(resourceType);
                    data.options.availablePatterns = resourceType.patterns;
                }

                self.parentRender(function () {
                    var promises = [], resolve = function () { return (promises[promises.length] = $.Deferred()).resolve; };

                    ManageSubjectsView.render(data, resolve());
                    ManageEnvironmentsView.render(data, resolve());

                    PolicyActionsView.render(data, resolve());
                    ResourcesView.render(data, resolve());

                    self.staticAttrsView = new StaticResponseAttributesView();
                    self.staticAttrsView.render(data.entity, staticAttributes, '#staticAttrs', resolve());

                    SubjectResponseAttributesView.render([userAttributes, allUserAttributes], resolve());

                    self.prepareInfoReview();
                    self.validateThenRenderReview(resolve());
                    self.initAccordion();

                    $.when.apply($, promises).done(function () {
                        if (callback) { callback(); }
                    });
                });
            });
        },

        getAvailableActionsForResourceType: function (resourceType) {
            var availableActions = [];
            if (resourceType) {
                _.each(resourceType.actions, function (val, key) {
                    availableActions.push({action: key, value: val});
                });
            }
            return availableActions;
        },

        changeResourceType: function (e) {
            this.data.entity.resourceTypeUuid = e.target.value;

            var resourceType = _.findWhere(this.data.options.availableResourceTypes, {uuid: e.target.value});

            this.data.options.availableActions = this.getAvailableActionsForResourceType(resourceType);
            this.data.options.availablePatterns = resourceType ? resourceType.patterns : [];

            this.data.options.newPattern = null;
            this.data.entity.resources = [];
            this.data.entity.actionValues = {};

            ResourcesView.render(this.data);
            PolicyActionsView.render(this.data);
        },

        getPolicy: function (policyName) {
            var deferred = $.Deferred(),
                policy = {};

            if (policyName) {
                PolicyDelegate.getPolicyByName(policyName).done(function (policy) {
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
            this.data.actionsString = _.isEmpty(this.data.entity.actionValues) ? null :
                JSON.stringify(this.data.entity.actionValues,
                    function (key, value) {
                        if (!key) {
                            return value;
                        } else if (value === true) {
                            return $.t('policy.actions.allow');
                        }
                        return $.t('policy.actions.deny');
                    }, 2);
            this.data.combinedStaticAttrs = this.staticAttrsView.getCombinedAttrs();
            this.data.userAttrs = SubjectResponseAttributesView.getAttrs();
            this.data.responseAttrs = this.data.combinedStaticAttrs.concat(this.data.userAttrs);
            this.data.subjectString = JSON.stringify(this.data.entity.subject, null, 2);
            this.data.environmentString = JSON.stringify(this.data.entity.condition, null, 2);
        },

        submitForm: function () {
            var policy = this.data.entity,
                persistedPolicy = _.clone(policy),
                self = this;

            persistedPolicy.resourceAttributes = _.union(this.staticAttrsView.getCombinedAttrs(), SubjectResponseAttributesView.getAttrs());

            if (this.data.entityName) {
                PolicyDelegate.updatePolicy(this.data.entityName, persistedPolicy)
                    .done(function (e) {
                        Router.routeTo(Router.configuration.routes.managePolicies, {args: [persistedPolicy.applicationName], trigger: true});
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyUpdated");
                    })
                    .fail(function (e) {
                        self.errorHandler(e);
                    });

            } else {
                PolicyDelegate.createPolicy(persistedPolicy)
                    .done(function (e) {
                        Router.routeTo(Router.configuration.routes.managePolicies, { args: [persistedPolicy.applicationName], trigger: true});
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyCreated");
                    })
                    .fail(function (e) {
                        self.errorHandler(e);
                    });
            }
        },

        errorHandler: function (e) {
            var obj = { message: '', type: "error"},
                invalidResourceText = "Invalid Resource";

            if (e.responseText) {
                obj.message = JSON.parse(e.responseText).message;
            }

            if (e.status === 500) {

                if (UIUtils.responseMessageMatch( e.responseText,"Unable to persist policy")){
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unableToPersistPolicy");
                } else {
                    console.error(e.responseJSON, e.responseText, e);
                    Messages.messages.addMessage(obj);
                }

            } else if (e.status === 400 || e.status === 404){

                if (UIUtils.responseMessageMatch(e.responseText, invalidResourceText)) {

                    this.data.options.invalidResource = obj.message.substr(invalidResourceText.length + 1);
                    ReviewInfoView.render(this.data, null, this.$el.find('#reviewInfo'), this.reviewTemplate);
                    ResourcesView.render(this.data);
                    delete this.data.options.invalidResource;

                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidResource");

                } else if (obj.message === "Policy " + this.data.entity.name + " already exists") {

                    this.data.options.invalidName = true;
                    ReviewInfoView.render(this.data, null, this.$el.find('#reviewInfo'), this.reviewTemplate);
                    delete this.data.options.invalidName;
                    Messages.messages.addMessage(obj);

                } else {
                    console.log(e.responseJSON, e.responseText, e);
                    Messages.messages.addMessage(obj);
                }
            } else {
                console.log(e.responseJSON, e.responseText, e);
                Messages.messages.addMessage(obj);
            }
        }

    });

    return new EditPolicyView();
});