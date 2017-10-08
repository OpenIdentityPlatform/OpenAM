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
 * Copyright 2015-2016 ForgeRock AS.
 */


define([
    "jquery",
    "lodash",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrBaseView",
    "bootstrap-datetimepicker"
], function ($, _, ConditionAttrBaseView) {
    return ConditionAttrBaseView.extend({
        template: "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrDate.html",

        render (data, element, callback) {
            this.initBasic(data, element, "pull-left attr-group");

            this.parentRender(function () {
                this.initDatePickers();

                if (callback) {
                    callback();
                }
            });
        },

        initDatePickers () {
            var options = {
                    format: "YYYY:MM:DD",
                    useCurrent: false,
                    icons: {
                        previous: "fa fa-chevron-left",
                        next: "fa fa-chevron-right"
                    }
                },
                startDate = this.$el.find("#startDate"),
                endDate = this.$el.find("#endDate");

            startDate.datetimepicker(options);
            endDate.datetimepicker(options);

            startDate.on("dp.change", function (e) {
                endDate.data("DateTimePicker").minDate(e.date);
            });

            endDate.on("dp.change", function (e) {
                startDate.data("DateTimePicker").maxDate(e.date);
            });
        }
    });
});
