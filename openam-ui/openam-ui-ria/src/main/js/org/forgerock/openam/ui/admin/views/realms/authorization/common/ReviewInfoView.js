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

/*global define, sessionStorage */

define("org/forgerock/openam/ui/admin/views/realms/authorization/common/ReviewInfoView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants"
], function ($, _, AbstractView, constants) {
    var ReviewInfoView = AbstractView.extend({
        noBaseTemplate: true,
        events: {
            "click #toggleAdvanced": "toggleAdvancedView"
        },

        render: function (args, callback, element, template) {
            this.element = element;
            this.template = template;

            _.extend(this.data, args);

            this.storageKey = constants.OPENAM_STORAGE_KEY_PREFIX + "review-advanced-mode";
            this.data.advancedMode = !!JSON.parse(sessionStorage.getItem(this.storageKey));

            this.renderParent(callback);
        },

        renderParent: function (callback) {
            this.parentRender(function () {
                this.toggleAll();

                this.$el.find("#toggleAdvanced").text(this.data.advancedMode ?
                    $.t("console.authorization.policies.edit.summaryReview.minimized") :
                    $.t("console.authorization.policies.edit.summaryReview.maximized"));

                if (callback) {
                    callback();
                }
            });
        },

        toggleAdvancedView: function (e) {
            e.preventDefault();

            this.data.advancedMode = !this.data.advancedMode;
            sessionStorage.setItem(this.storageKey, this.data.advancedMode);

            $(e.target).text(this.data.advancedMode ?
                $.t("console.authorization.policies.edit.summaryReview.minimized") :
                $.t("console.authorization.policies.edit.summaryReview.maximized"));

            this.toggleAll();
        },

        toggleAll: function () {
            var reviewItems = this.$el.find(".review-panel:not(.panel-danger)"),
                toggle = this.data.advancedMode ? "show" : "hide";

            _.each(reviewItems, function (reviewItem) {
                $(reviewItem).find(".collapse").collapse(toggle);
            });
        }
    });

    return new ReviewInfoView();
});