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

define("org/forgerock/openam/ui/policy/policies/conditions/ConditionAttrDayView", [
    "org/forgerock/openam/ui/policy/policies/conditions/ConditionAttrBaseView"
], function (ConditionAttrBaseView) {
    var ConditionAttrDayView = ConditionAttrBaseView.extend({
        template: 'templates/policy/policies/conditions/ConditionAttrDay.html',
        i18n: {
            'weekdays': { 'key': 'policy.common.weekdays.', 'full': '.full', 'short': '.short' }
        },
        days: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'],

        render: function (data, element, callback) {
            this.initBasic(data, element, 'float-left input-group');

            this.data.weekdays = this.getWeekDays();
            this.parentRender(function () {
                if (callback) {
                    callback();
                }
            });
        },

        getWeekDays: function () {
            var weekdays = [], i = 0, self = this;
            _.invoke(self.days, function () {
                weekdays[i] = {};
                weekdays[i].title = $.t(self.i18n.weekdays.key + this + self.i18n.weekdays.full);
                weekdays[i].value = $.t(self.i18n.weekdays.key + this + self.i18n.weekdays.short);
                i++;
            });
            return weekdays;
        }
    });

    return ConditionAttrDayView;
});