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

define("org/forgerock/openam/ui/policy/policies/conditions/ConditionAttrTimeZoneView", [
    "org/forgerock/openam/ui/policy/policies/conditions/ConditionAttrBaseView"
], function (ConditionAttrBaseView) {
    var ConditionAttrTimeZoneView = ConditionAttrBaseView.extend({
        template: 'templates/policy/policies/conditions/ConditionAttrTimeZone.html',
        DEFAULT_TIME_ZONE: 'GMT',

        render: function (data, element, callback) {
            this.initBasic(data, element, 'float-left');

            _.extend(this.data, {defaultTimeZone: this.DEFAULT_TIME_ZONE});

            this.initTimeZones();

            this.parentRender(function () {
                if (callback) {
                    callback();
                }
            });

            return this;
        },

        initTimeZones: function () {
            var self = this;

            if (this.data.timezones) {
                this.setTimeZones();
                return;
            }

            $.ajax({
                url: 'timezones.json',
                dataType: "json",
                cache: true
            }).then(function (data) {
                self.data.timezones = data.timezones;
                self.setTimeZones();
            });
        },

        setTimeZones: function () {
            var self = this,
                timezone = self.$el.find('#enforcementTimeZone');

            timezone.autocomplete({
                source: self.data.timezones
            });

            if (!timezone.val()) {
                timezone.val(self.data.defaultTimeZone).trigger('autocompleteselect');
                self.data.itemData.enforcementTimeZone = self.data.defaultTimeZone;
            }
        },

        attrSpecificChangeInput: function (e) {
            this.handleTimeZone(e.currentTarget.value);
        },

        handleTimeZone: function (currentVal) {
            if (!_.find(this.data.timezones, function (zone) {
                return zone === currentVal;
            })) {
                this.data.itemData.enforcementTimeZone = this.data.defaultTimeZone;
            }
        }
    });

    return ConditionAttrTimeZoneView;
});