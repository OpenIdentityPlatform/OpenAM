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
 * @author Eugenia Sergueeva
 */

/*global window, define, $, _, document, console, sessionStorage */

define("org/forgerock/openam/ui/policy/ReviewInfoView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants"
], function (AbstractView, constants) {
    var ReviewInfoView = AbstractView.extend({
        noBaseTemplate: true,
        events: {
            'click #toggleAdvanced': 'toggleAdvancedView',
            'click .icon-arrow-down2': 'toggleAdvancedView'
        },

        render: function (args, callback, element, template) {
            this.element = element;
            this.template = template;

            _.extend(this.data, args);

            this.data.actionsSelected = _.find(this.data.entity.actions, function (action) {
                return action.selected === true;
            });

            this.storageKey = constants.OPENAM_STORAGE_KEY_PREFIX + 'review-advanced-mode';
            this.data.advancedMode = !!JSON.parse(sessionStorage.getItem(this.storageKey));

            this.renderParent(callback);
        },

        renderParent: function (callback) {
            this.parentRender(function () {
                var reviewItems;
                if (this.data.advancedMode) {
                    reviewItems = this.$el.find('.advanced-mode');
                    reviewItems.removeClass('hidden');
                } else {
                    reviewItems = this.$el.find('.advanced-mode').filter(function () {
                        return !$(this).parent().hasClass('invalid');
                    });
                    reviewItems.addClass('hidden');
                }

                if (callback) {
                    callback();
                }
            });

        },

        toggleAdvancedView: function (e) {
            e.stopPropagation();
            e.preventDefault();
            this.data.advancedMode = !this.data.advancedMode;
            sessionStorage.setItem(this.storageKey, this.data.advancedMode);
            this.renderParent();
        }
    });

    return new ReviewInfoView();
});