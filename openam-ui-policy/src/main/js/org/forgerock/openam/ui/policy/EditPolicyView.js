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
            'click input[name=submitForm]': 'submitForm'
        },
        data: {              // Mock data to be replaced later
            subjects: [
                {
                    type: "Virtual Subject",
                    list: [
                        { name: "iplanet-am-session-get-valid-sessions" },
                        { name: "sunIdentityServerPPFacadegreetmesound" },
                        { name: "iplanet-am-user-password-reset-question-answer" },
                        { name: "iplanet-am-user-admin-start-dn" },
                        { name: "iplanet-am-user-success-url" },
                        { name: "sunIdentityServerPPDemographicsDisplayLanguage" },
                        { name: "iplanet-am-user-federation-info" }
                    ]
                },
                {
                    type: "Attribute Subject",
                    list: [

                        { name: "sunIdentityServerPPCommonNameMN" },
                        { name: "iplanet-am-session-get-valid-sessions" },
                        { name: "sunIdentityServerPPFacadegreetmesound" },
                        { name: "iplanet-am-user-password-reset-question-answer" },
                        { name: "iplanet-am-user-admin-start-dn" }
                    ]
                },
                {
                    type: "Identity Repository User",
                    list: [
                        { name: "sunIdentityServerPPInformalName" },
                        { name: "sunIdentityServerPPFacadeGreetSound" },
                        { name: "sunIdentityServerPPLegalIdentityGender" }
                    ]
                },
                {
                    type: "Identity Repository Group",
                    list: [
                        { name: "iplanet-am-user-password-reset-question-answer" },
                        { name: "iplanet-am-user-admin-start-dn" },
                        { name: "iplanet-am-user-success-url" },
                        { name: "sunIdentityServerPPDemographicsDisplayLanguage" },
                        { name: "iplanet-am-user-federation-info" }
                    ]
                }
            ],


            "result": [
                {
                    "title": "OR",
                    "logical": true,
                    "config": {
                        "type": "object",
                        "properties": {
                            "conditions": {
                                "type": "array",
                                "items": {
                                    "type": "any"
                                }
                            }
                        }
                    }
                },
                {
                    "title": "Time",
                    "logical": false,
                    "config": {
                        "type": "object",
                        "properties": {
                            "startTime": {
                                "type": "string"
                            },
                            "endTime": {
                                "type": "string"
                            },
                            "startDay": {
                                "type": "string"
                            },
                            "endDay": {
                                "type": "string"
                            },
                            "startDate": {
                                "type": "string"
                            },
                            "endDate": {
                                "type": "string"
                            },
                            "enforcementTimeZone": {
                                "type": "string"
                            }
                        }
                    }
                },
                {
                    "title": "IP",
                    "logical": false,
                    "config": {
                        "type": "object",
                        "properties": {
                            "startIp": {
                                "type": "string"
                            },
                            "endIp": {
                                "type": "string"
                            }
                        }
                    }
                },
                {
                    "title": "StringAttribute",
                    "logical": false,
                    "config": {
                        "type": "object",
                        "properties": {
                            "attributeName": {
                                "type": "string"
                            },
                            "value": {
                                "type": "string"
                            },
                            "caseSensitive": {
                                "type": "boolean",
                                "required": true
                            }
                        }
                    }
                },
                {
                    "title": "Policy",
                    "logical": false,
                    "config": {
                        "type": "object",
                        "properties": {
                            "className": {
                                "type": "string"
                            },
                            "properties": {
                                "type": "object"
                            }
                        }
                    }
                },
                {
                    "title": "DNSName",
                    "logical": false,
                    "config": {
                        "type": "object",
                        "properties": {
                            "domainNameMask": {
                                "type": "string"
                            }
                        }
                    }
                },
                {
                    "title": "AttributeLookup",
                    "logical": false,
                    "config": {
                        "type": "object",
                        "properties": {
                            "key": {
                                "type": "string"
                            },
                            "value": {
                                "type": "string"
                            }
                        }
                    }
                },
                {
                    "title": "AND",
                    "logical": true,
                    "config": {
                        "type": "object",
                        "properties": {
                            "conditions": {
                                "type": "array",
                                "items": {
                                    "type": "any"
                                }
                            }
                        }
                    }
                },
                {
                    "title": "NumericAttribute",
                    "logical": false,
                    "config": {
                        "type": "object",
                        "properties": {
                            "attributeName": {
                                "type": "string"
                            },
                            "operator": {
                                "type": "string",
                                "enum": [ "LESS_THAN", "LESS_THAN_OR_EQUAL", "EQUAL", "GREATER_THAN_OR_EQUAL", "GREATER_THAN" ]
                            },
                            "value": {
                                "type": "number"
                            }
                        }
                    }
                },
                {
                    "title": "NOT",
                    "logical": true,
                    "config": {
                        "type": "object",
                        "properties": {
                            "condition": {
                                "type": "object",
                                "properties": {
                                }
                            }
                        }
                    }
                }
            ]

        },
        REVIEW_INFO_STEP: 5,

        render: function (args, callback) {
            var self = this,
                data = self.data,
                appName = args[0],
                policyName = args[1],
                policyPromise = this.getPolicy(policyName),
                appPromise = policyDelegate.getApplicationByName(appName);

            $.when(policyPromise, appPromise).done(function (policy, app) {
                var actions = [],
                    resources = [],
                    exceptions = [];
                if (policyName) {
                    _.each(policy.resources.included, function (value, list) {
                        resources.push(value);
                    });

                    _.each(policy.resources.excluded, function (value, list) {
                        exceptions.push(value);
                    });

                    policy.actions = policy.actionValues;
                    policy.resources = resources;
                    policy.exceptions = exceptions;

                    data.entity = policy;
                    data.entityName = policyName;
                } else {
                    data.entity = {};
                    data.entityName = null;
                }
                _.each(app[0].actions, function (value, key, list) {
                    actions.push({action: key, selected: false, value: value});
                });

                data.entity.availableActions = actions;
                data.entity.resourcePatterns = app[0].resources;

                data.entity.applicationName = appName;
                self.parentRender(function () {
                    manageSubjects.render(data);
                    manageEnvironments.render(data);
                    actionsView.render(data);
                    resourcesListView.render(data);
                    addNewResourceView.render(data);
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
        },

        /**
         * Opens next accordion step.
         * TODO: some validation probably will be done here
         */
        openNextStep: function (e) {
            this.accordion.setActive(this.accordion.getActive() + 1);
        },

        submitForm: function () {
            var policy = this.data.entity,
                persistedPolicy = _.clone(policy);

            persistedPolicy.actions = {};
            persistedPolicy.resources = {};

            _.each(policy.actions, function (action) {
                if (action.selected) {
                    persistedPolicy.actions[action.action] = action.value;
                }
            });

            persistedPolicy.actionValues = persistedPolicy.actions;
            persistedPolicy.resources.included = policy.resources;
            persistedPolicy.resources.excluded = policy.exceptions;

            if (this.data.entityName) {
                policyDelegate.updatePolicy(this.data.entityName, persistedPolicy).done(function () {
                    router.routeTo(router.configuration.routes.managePolicies, {args: [persistedPolicy.applicationName], trigger: true});
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyUpdated");
                });
            } else {
                policyDelegate.createPolicy(persistedPolicy).done(function () {
                    router.routeTo(router.configuration.routes.managePolicies, {args: [persistedPolicy.applicationName], trigger: true});
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policyCreated");
                });
            }
        }
    });


    return new EditPolicyView();
});