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

/*global window, define, $, _ */

define("org/forgerock/openam/ui/policy/common/AbstractEditView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/policy/common/ReviewInfoView",
    "org/forgerock/openam/ui/common/components/Accordion",
    "org/forgerock/openam/ui/policy/common/HelpLinkView"
], function (AbstractView, ReviewInfoView, Accordion, HelpLink) {
    var AbstractEditView = AbstractView.extend({

        events: {
            'click input[name=submitForm]': 'submitForm',
            'click .review-panel': 'reviewRowClick',
            'keyup .review-panel': 'reviewRowClick'
        },

        initAccordion: function () {
            var self = this;

            this.accordion = new Accordion(this, '#accordion');

            this.accordion.on('show.bs.collapse', function (e) {
                if ($(self.accordion.panels).index(e.target) === self.accordion.panels.length - 1) {
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

        validateThenRenderReview: function () {
            this.data.options.invalidEntity = this.validate();
            ReviewInfoView.render(this.data, null, this.$el.find('#reviewInfo'), this.reviewTemplate);
        },

        validate: function () {

            var entity = this.data.entity,
                invalid = false;

            // entities that are stored in LDAP can't start with '#'. http://www.jguru.com/faq/view.jsp?EID=113588
            if (entity.name && entity.name.startsWith('#')) {
                invalid = true;
                this.$el.find('input[name=entityName]').parents('.form-group').addClass('has-error');
            } else {
                this.$el.find('input[name=entityName]').parents('.form-group').removeClass('has-error');
            }

            this.data.options.incorrectName = invalid;

            _.each(this.validationFields, function (field) {
                if (entity[field] === undefined || entity[field] === null || entity[field].length === 0) {
                    invalid = true;
                    return;
                }
            });

            this.$el.find('input[name=submitForm]').prop('disabled', invalid);
        },

        reviewRowClick: function (e) {
            if (e.type === 'keyup' && e.keyCode !== 13) {
                return;
            }

            var reviewRows = this.$el.find('.review-panel'),
                targetIndex = -1;
            _.find(reviewRows, function (reviewRow, index) {
                if (reviewRow === e.currentTarget) {
                    targetIndex = index;
                }
            });

            this.accordion.show(targetIndex);
        }

    });

    return AbstractEditView;
});