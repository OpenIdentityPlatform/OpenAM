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

define("org/forgerock/openam/ui/policy/conditions/ConditionAttrBooleanView", [
    "org/forgerock/openam/ui/policy/conditions/ConditionAttrBaseView"
], function (ConditionAttrBaseView) {
    var ConditionAttrBooleanView = ConditionAttrBaseView.extend({
        template: 'templates/policy/conditions/ConditionAttrBoolean.html',

        render: function (data, element, callback) {
            this.initBasic(data, element, 'field-float-pattern data-obj button-field');

            this.events['click .buttonControl a.button'] = _.bind(this.buttonControlClick, this);
            this.events['keyup .buttonControl a.button'] = _.bind(this.buttonControlClick, this);

            this.parentRender(function () {
                if (callback) {
                    callback();
                }
            });
        },

        buttonControlClick: function (e) {
            if (e.type === 'keyup' && e.keyCode !== 13) {
                return;
            }

            var $target = $(e.currentTarget),
                buttonControl = $target.closest('ul.buttonControl'),
                label = buttonControl.prev('label').data().title;

            this.data.itemData[label] = $target.data('val');
            buttonControl.find('li a').removeClass('selected');
            $target.addClass('selected');
        }
    });

    return ConditionAttrBooleanView;
});