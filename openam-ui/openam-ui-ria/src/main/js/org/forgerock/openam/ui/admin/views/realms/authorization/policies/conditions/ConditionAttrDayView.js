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
 * Copyright 2015 ForgeRock AS.
 */


define("org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrDayView", [
    "jquery",
    "underscore",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrBaseView"
], function ($, _, ConditionAttrBaseView) {
    return ConditionAttrBaseView.extend({
        template: "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrDay.html",
        i18n: {
            "weekdays": { "key": "console.authorization.common.weekdays.", "full": ".full", "short": ".short" }
        },
        days: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"],

        render: function (data, element, callback) {
            this.initBasic(data, element, "pull-left attr-group");

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
});
