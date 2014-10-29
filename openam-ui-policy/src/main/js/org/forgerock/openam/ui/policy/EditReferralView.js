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
 * @author JKigwana
 */

/*global window, define, $, form2js, _, js2form, document, console */

define("org/forgerock/openam/ui/policy/EditReferralView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/policy/ResourcesListView",
    "org/forgerock/openam/ui/policy/AddNewResourceView",
    "org/forgerock/openam/ui/policy/ReviewInfoView",
    "org/forgerock/openam/ui/policy/SelectRealmsView",
    "org/forgerock/openam/ui/policy/PolicyDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/components/Accordion",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router"
], function (AbstractView, resourcesListView, addNewResourceView, reviewInfoView, selectRealmsView, policyDelegate, uiUtils, Accordion, constants, eventManager, router) {
    var EditReferralView = AbstractView.extend({
        baseTemplate: "templates/policy/BaseTemplate.html",
        template: "templates/policy/EditReferralTemplate.html",
        events: {
            'click input[name=nextButton]': 'openNextStep',
            'click input[name=submitForm]': 'submitForm',
            'click .review-row': 'reviewRowClick',
            'keyup .review-row': 'reviewRowClick'
        },
        data: {},

        REVIEW_INFO_STEP: 3,

        render: function (args, callback) {

            var self = this,
                data = self.data,
                referralName = args[1],
                referalPromise  = policyDelegate.getReferralByName(referralName),
                appPromise      = policyDelegate.getApplicationByName(args[0]);


                data.appName = args[0];

            $.when(appPromise, referalPromise).done(function(app, referral){

                if (referralName) {
                    data.entity = referral;
                    data.entity.resources = data.entity.resources[data.appName];
                    data.entityName = referralName;

                } else {
                    data.entity = {};
                    data.entityName = null;
                }

                data.options = {};
                data.options.realm = app[0].realm;
                data.options.resourcePatterns = _.sortBy(app[0].resources);

                self.parentRender(function () {

                    addNewResourceView.render(data);
                    resourcesListView.render(data);
                    selectRealmsView.render(data);

                    reviewInfoView.render(this.data, null, self.$el.find('#reviewInfo'), "templates/policy/ReviewReferralStepTemplate.html");
                    self.initAccordion();

                    if (callback) {
                        callback();
                    }
                });

            });

        },

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
                    reviewInfoView.render(self.data, null, self.$el.find('#reviewInfo'), "templates/policy/ReviewReferralStepTemplate.html");
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
           //TODO
        },

        errorHandler: function (e) {
            //TODO
        }
    });

    return new EditReferralView();
});
