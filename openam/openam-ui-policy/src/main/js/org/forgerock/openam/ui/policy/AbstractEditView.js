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

define("org/forgerock/openam/ui/policy/AbstractEditView", [
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
    "org/forgerock/openam/ui/policy/HelpLinkView"
], function (AbstractView, actionsView, resourcesListView, addNewResourceView, responseAttrsStaticView, responseAttrsUserView, reviewInfoView, policyDelegate, uiUtils, Accordion, HelpLink) {
    var AbstractEditView = AbstractView.extend({
        baseTemplate: "templates/policy/BaseTemplate.html",
        events: {
            'click input[name=nextButton]': 'openNextStep',
            'click input[name=submitForm]': 'submitForm',
            'click .review-row': 'reviewRowClick',
            'keyup .review-row': 'reviewRowClick'
        },

        initAccordion: function () {
            var self = this,
                options = {};

            if (this.data.entity.name) {
                options.active = this.$el.find('.accordion > section').length - 1;
            } else {
                options.disabled = true;
            }

            this.accordion = new Accordion(this.$el.find('.accordion'), options);

            this.accordion.on('beforeChange', function (e, index) {
                if (index === this.$sections.length-1) {
                    self.updateFields();
                    self.validateThenRenderReview();
                }
            });

            this.addHelpLinks();
        },

        addHelpLinks: function () {
            _.each(this.$el.find('.help-link'), function (link) {
                new HelpLink().render($(link));
            });
        },

        validateThenRenderReview: function(){
            this.data.options.invalidEntity = this.validate();
            reviewInfoView.render(this.data, null, this.$el.find('#reviewInfo'), this.reviewTemplate);
        },

        validate: function () {

            var entity = this.data.entity,
                invalid = false;

            _.each(this.validationFields, function(field){
                if(entity[field] === undefined || entity[field].length === 0 ){
                    invalid = true;
                    return;
                }
            });

            this.$el.find('input[name=submitForm]').prop('disabled', invalid );
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
        }

    });

    return AbstractEditView;
});
