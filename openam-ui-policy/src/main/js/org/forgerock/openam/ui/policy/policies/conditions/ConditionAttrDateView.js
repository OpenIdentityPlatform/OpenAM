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
 * @author JKigwana
 * @author Eugenia Sergueeva
 */

/*global window, define, $, _ */

define("org/forgerock/openam/ui/policy/policies/conditions/ConditionAttrDateView", [
    "org/forgerock/openam/ui/policy/policies/conditions/ConditionAttrBaseView"
], function (ConditionAttrBaseView) {
    var ConditionAttrDateView = ConditionAttrBaseView.extend({
        template: 'templates/policy/policies/conditions/ConditionAttrDate.html',

        render: function (data, element, callback) {
            this.initBasic(data, element, 'float-left input-group');

            this.parentRender(function () {
                this.initDatePickers();

                if (callback) {
                    callback();
                }
            });
        },

        initDatePickers: function () {
            var common = {numberOfMonths: 2, dateFormat: 'yy:mm:dd'};

            this.$el.find("#startDate").datepicker(
                $.extend({onClose: function (selectedDate) {
                    $("#endDate").datepicker("option", "minDate", selectedDate);
                }}, common)
            );

            this.$el.find("#endDate").datepicker(
                $.extend({onClose: function (selectedDate) {
                    $("#startDate").datepicker("option", "maxDate", selectedDate);
                }}, common)
            );
        }
    });

    return ConditionAttrDateView;
});