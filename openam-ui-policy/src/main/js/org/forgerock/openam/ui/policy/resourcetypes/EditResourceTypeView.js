/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openam/ui/policy/resourcetypes/EditResourceTypeView", [
    "org/forgerock/openam/ui/policy/common/AbstractEditView",
    "org/forgerock/openam/ui/policy/common/ReviewInfoView",
    "org/forgerock/openam/ui/policy/delegates/PolicyDelegate",
    "org/forgerock/openam/ui/policy/resourcetypes/ResourceTypePatternsView",
    "org/forgerock/openam/ui/policy/resourcetypes/ResourceTypeActionsView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/components/Accordion",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router"
], function (AbstractEditView, reviewInfoView, policyDelegate, ResourceTypePatternsView, ResourceTypeActionsView, uiUtils, Accordion, constants, conf, eventManager, router) {
    var EditResourceTypeView = AbstractEditView.extend({
        template: "templates/policy/resourcetypes/EditResourceTypeTemplate.html",
        reviewTemplate: "templates/policy/resourcetypes/ReviewResourceTypeStepTemplate.html",
        data: {},
        validationFields: ["name", "actions", "patterns"],
        render: function (args, callback) {
            var self = this,
                data = self.data,
                uuid = args[0],
                resourceTypePromise = this.getResourceType(uuid);

            $.when(resourceTypePromise).done(function (resourceType) {
                data.options = {};

                data.entity = resourceType || {};
                data.uuid = uuid;

                if (!data.entity.realm) {
                    data.entity.realm = conf.globalData.auth.realm;
                }

                data.actions = [];
                _.each(data.entity.actions, function (v, k) {
                    data.actions.push({name: k, value: v});
                });
                data.actions.sort();

                self.parentRender(function () {

                    self.patternsView = new ResourceTypePatternsView();
                    self.patternsView.render(data.entity, data.entity.patterns, '#resTypePatterns');

                    self.actionsList = new ResourceTypeActionsView();
                    self.actionsList.render(data.entity, data.actions, '#resTypeActions');

                    self.validateThenRenderReview();
                    self.initAccordion();

                    if (callback) {
                        callback();
                    }
                });
            });
        },

        updateFields: function () {
            var entity = this.data.entity,
                dataFields = this.$el.find('[data-field]');

            _.each(dataFields, function (field) {
                entity[field.getAttribute('data-field')] = field.value;
            });
        },

        getResourceType: function (uuid) {
            var d = $.Deferred(),
                rType = null;

            if (uuid) {
                policyDelegate.getResourceType(uuid).done(function (rType) {
                    d.resolve(rType);
                });
            } else {
                d.resolve(rType);
            }

            return d.promise(rType);
        },

        submitForm: function () {
            var resType = this.data.entity;

            if (this.data.uuid) {
                policyDelegate.updateResourceType(resType)
                    .done(function (e) {
                        router.routeTo(router.configuration.routes.manageResourceTypes, {args: [], trigger: true});
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "resourceTypeUpdated");
                    })
                    .fail(function (e) {
                        // todo
                    });
            } else {
                policyDelegate.createResourceType(resType)
                    .done(function (e) {
                        router.routeTo(router.configuration.routes.manageResourceTypes, {args: [], trigger: true});
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "resourceTypeCreated");
                    })
                    .fail(function (e) {
                        // todo
                    });
            }
        }
    });

    return new EditResourceTypeView();
});