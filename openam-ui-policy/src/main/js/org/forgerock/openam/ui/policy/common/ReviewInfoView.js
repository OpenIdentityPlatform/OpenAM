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

/*global define, $, _, sessionStorage */

define("org/forgerock/openam/ui/policy/common/ReviewInfoView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants"
], function (AbstractView, constants) {
    var ReviewInfoView = AbstractView.extend({
        noBaseTemplate: true,
        events: {
            'click #toggleAdvanced': 'toggleAdvancedView'
        },

        render: function (args, callback, element, template) {
            this.element = element;
            this.template = template;

            _.extend(this.data, args);

            this.storageKey = constants.OPENAM_STORAGE_KEY_PREFIX + 'review-advanced-mode';
            this.data.advancedMode = !!JSON.parse(sessionStorage.getItem(this.storageKey));

            this.renderParent(callback);
        },

        renderParent: function (callback) {
            this.parentRender(function () {
                this.toggleAll();

                this.$el.find('#toggleAdvanced').text(this.data.advancedMode ? $.t('policy.summaryReview.minimized') : $.t('policy.summaryReview.maximized'));

                if (callback) {
                    callback();
                }
            });

        },

        toggleAdvancedView: function (e) {
            e.preventDefault();

            this.data.advancedMode = !this.data.advancedMode;
            sessionStorage.setItem(this.storageKey, this.data.advancedMode);

            $(e.target).text(this.data.advancedMode ? $.t('policy.summaryReview.minimized') : $.t('policy.summaryReview.maximized'));

            this.toggleAll();
        },

        toggleAll: function () {
            var reviewItems = this.$el.find('.review-panel:not(.panel-danger)'),
                toggle = this.data.advancedMode ? 'show' : 'hide';

            _.each(reviewItems, function (reviewItem) {
                $(reviewItem).find('.collapse').collapse(toggle);
            });
        }
    });

    return new ReviewInfoView();
});