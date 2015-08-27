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

define("org/forgerock/openam/ui/admin/views/realms/authorization/policies/EditPolicyView", [
    "jquery",
    "underscore",
    "backbone",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/components/Accordion",
    "org/forgerock/openam/ui/admin/models/authorization/PolicyModel",
    "org/forgerock/openam/ui/admin/delegates/PoliciesDelegate",
    "org/forgerock/openam/ui/admin/views/realms/authorization/common/ReviewInfoView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/common/StripedListView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/PolicyActionsView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/ResourcesView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/attributes/StaticResponseAttributesView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/attributes/SubjectResponseAttributesView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ManageSubjectsView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ManageEnvironmentsView"
], function ($, _, Backbone, BootstrapDialog, Messages, AbstractView, EventManager, Router, UIUtils, Constants,
             Accordion, PolicyModel, PoliciesDelegate, ReviewInfoView, StripedList, PolicyActionsView, ResourcesView,
             StaticResponseAttributesView, SubjectResponseAttributesView, ManageSubjectsView, ManageEnvironmentsView) {
    var EditPolicyView = AbstractView.extend({
        template: "templates/admin/views/realms/authorization/policies/EditPolicyTemplate.html",
        reviewTemplate: "templates/admin/views/realms/authorization/policies/ReviewPolicyStepTemplate.html",
        validationFields: ["name", "resources"],

        initialize: function (options) {
            AbstractView.prototype.initialize.call(this);
            this.model = null;

            this.events = {
                "click input[name=submitForm]": "submitForm",
                "click #cancelEdit": "cancelEdit",
                "click .review-panel": "reviewRowClick",
                "keyup .review-panel": "reviewRowClick",
                "change #availableResTypes": "changeResourceType"
            };
        },

        render: function (args, callback) {
            var self = this;

            if (callback) {
                this.renderCallback = callback;
            }

            this.data = args;

            this.allSubjectsPromise = PoliciesDelegate.getSubjectConditions();
            this.allEnvironmentsPromise = PoliciesDelegate.getEnvironmentConditions();
            this.allUserAttributesPromise = PoliciesDelegate.getAllUserAttributes();
            this.resourceTypesPromise = PoliciesDelegate.listResourceTypes();

            if (this.data.policyModel) {
                this.model = new PolicyModel({name: this.data.policyModel.id});
                this.model.fetch().done(function () {
                    self.renderPolicy();
                });
            } else {
                this.model = new PolicyModel();
                this.renderPolicy();
            }
        },

        renderPolicy: function () {
            var self = this;

            this.data.entity = this.model.attributes;
            this.data.options = {};

            this.dialog = BootstrapDialog.show({
                type: BootstrapDialog.TYPE_DEFAULT,
                size: BootstrapDialog.SIZE_WIDE,
                cssClass: "edit-policy-dialog",
                title: this.model.id ?
                    $.t("common.form.edit") + ": " + this.model.id :
                    $.t("console.authorization.policies.edit.createNew"),
                message: $("<div></div>"),
                onshow: function () {
                    var dialog = this;

                    $.when(self.allSubjectsPromise, self.allEnvironmentsPromise, self.allUserAttributesPromise, self.resourceTypesPromise)
                        .done(function (allSubjects, allEnvironments, allUserAttributes, resourceTypes) {
                            var resourceType;

                            self.staticAttributes = _.where(self.model.attributes.resourceAttributes, {type: "Static"});
                            self.userAttributes = _.where(self.model.attributes.resourceAttributes, {type: "User"});

                            self.allUserAttributes = _.sortBy(allUserAttributes[0].result);

                            self.data.entity.applicationName = self.data.applicationModel.id;

                            self.data.options.availableEnvironments = _.findByValues(allEnvironments[0].result, "title",
                                self.data.applicationModel.attributes.conditions);
                            self.data.options.availableSubjects = _.findByValues(allSubjects[0].result, "title",
                                self.data.applicationModel.attributes.subjects);

                            self.data.options.availableResourceTypes = _.filter(resourceTypes[0].result, function (item) {
                                return _.contains(self.data.applicationModel.attributes.resourceTypeUuids, item.uuid);
                            });

                            if (self.model.attributes.resourceTypeUuid) {
                                resourceType = _.findWhere(self.data.options.availableResourceTypes, {
                                    uuid: self.model.attributes.resourceTypeUuid
                                });

                                self.data.options.availableActions = self.getAvailableActionsForResourceType(resourceType);
                                self.data.options.availablePatterns = resourceType.patterns;
                            }

                            UIUtils.fillTemplateWithData(self.template, self.data, function (tpl) {
                                dialog.message.html(tpl);
                                self.setElement(dialog.message);
                            });
                        });
                },
                onshown: function () {
                    var promises = [], resolve = function () {
                        return (promises[promises.length] = $.Deferred()).resolve;
                    };

                    ManageSubjectsView.render(self.data, resolve());
                    ManageEnvironmentsView.render(self.data, resolve());

                    PolicyActionsView.render(self.data, resolve());
                    ResourcesView.render(self.data, resolve());

                    self.staticAttrsView = new StaticResponseAttributesView();
                    self.staticAttrsView.render(self.data.entity, self.staticAttributes, "#staticAttrs", resolve());

                    SubjectResponseAttributesView.render([self.userAttributes, self.allUserAttributes], resolve());

                    self.prepareInfoReview();
                    self.validateThenRenderReview(resolve());
                    self.initAccordion();

                    $.when.apply($, promises).done(function () {
                        if (self.renderCallback) {
                            self.renderCallback();
                        }
                    });
                }
            });
        },

        prepareInfoReview: function () {
            this.data.actionsString = _.isEmpty(this.data.entity.actionValues) ? null :
                JSON.stringify(this.data.entity.actionValues,
                    function (key, value) {
                        if (!key) {
                            return value;
                        } else if (value === true) {
                            return $.t("common.form.allow");
                        }
                        return $.t("common.form.deny");
                    }, 2);
            this.data.combinedStaticAttrs = this.staticAttrsView.getCombinedAttrs();
            this.data.userAttrs = SubjectResponseAttributesView.getAttrs();
            this.data.responseAttrs = this.data.combinedStaticAttrs.concat(this.data.userAttrs);
            this.data.subjectString = JSON.stringify(this.data.entity.subject, null, 2);
            this.data.environmentString = JSON.stringify(this.data.entity.condition, null, 2);
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

        updateFields: function () {
            var entity = this.data.entity,
                dataFields = this.$el.find("[data-field]");

            _.each(dataFields, function (field, key, list) {
                entity[field.getAttribute("data-field")] = field.value;
            });

            this.prepareInfoReview();
        },

        cancelEdit: function (e) {
            this.dialog.close();
        },

        submitForm: function () {
            var savePromise,
                self = this;

            this.model.attributes.resourceAttributes = _.union(this.staticAttrsView.getCombinedAttrs(),
                SubjectResponseAttributesView.getAttrs());

            savePromise = this.model.save();

            if (savePromise) {
                savePromise
                    .done(function (response) {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                        self.dialog.close();
                        self.data.savePolicyCallback();
                    })
                    .fail(function (response) {
                        Messages.messages.addMessage({
                            message: JSON.parse(response.responseText).message,
                            type: "error"
                        });
                    });
            } else {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.validationError);
            }
        },

        initAccordion: function () {
            var self = this;

            this.accordion = new Accordion(this, "#accordion");

            this.accordion.on("show.bs.collapse", function (e) {
                if ($(self.accordion.panels).index(e.target) === self.accordion.panels.length - 1) {
                    self.updateFields();
                    self.validateThenRenderReview();
                }
            });
        },

        validateThenRenderReview: function (callback) {
            this.data.options.invalidEntity = this.validate();
            ReviewInfoView.render(this.data, callback, this.$el.find("#reviewInfo"), this.reviewTemplate);
        },

        validate: function () {
            var entity = this.data.entity,
                invalid = false;

            // entities that are stored in LDAP can't start with '#'. http://www.jguru.com/faq/view.jsp?EID=113588
            if (entity.name && entity.name.indexOf("#") === 0) {
                invalid = true;
                this.$el.find("input[name=entityName]").parents(".form-group").addClass("has-error");
            } else {
                this.$el.find("input[name=entityName]").parents(".form-group").removeClass("has-error");
            }

            this.data.options.incorrectName = invalid;

            _.each(this.validationFields, function (field) {
                if (entity[field] === undefined || entity[field] === null || entity[field].length === 0) {
                    invalid = true;
                    return;
                }
            });

            this.$el.find("input[name=submitForm]").prop("disabled", invalid);
        },

        reviewRowClick: function (e) {
            if (e.type === "keyup" && e.keyCode !== 13) {
                return;
            }

            var reviewRows = this.$el.find(".review-panel"),
                targetIndex = -1;
            _.find(reviewRows, function (reviewRow, index) {
                if (reviewRow === e.currentTarget) {
                    targetIndex = index;
                }
            });

            this.accordion.show(targetIndex);
        }
    });

    return new EditPolicyView();
});