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

define("org/forgerock/openam/ui/policy/conditions/ConditionAttrObjectView", [
    "org/forgerock/openam/ui/policy/conditions/ConditionAttrBaseView"
], function (ConditionAttrBaseView) {
    var ConditionAttrObjectView = ConditionAttrBaseView.extend({
        template: 'templates/policy/conditions/ConditionAttrObject.html',

        render: function (data, element, callback) {
            this.initBasic(data, element, 'field-float-selectize data-obj');

            this.parentRender(function () {
                this.initSelectize();

                if (callback) {
                    callback();
                }
            });
        },

        initSelectize: function () {
            var view = this,
                title = '',
                itemData,
                options,
                keyValPair,
                propName,
                propVal,
                $item;

            this.$el.find('select.selectize').each(function () {
                $item = $(this);
                options = {
                    persist: false,
                    delimiter: ';',
                    onItemRemove: function (value) {
                        title = this.$input.parent().find('label')[0].dataset.title;
                        itemData = view.data.itemData;
                        keyValPair = value.split(':');
                        delete itemData[title][keyValPair[0]];
                    },
                    onItemAdd: function (value) {
                        title = this.$input.parent().find('label')[0].dataset.title;
                        itemData = view.data.itemData;
                        keyValPair = value.split(':');
                        propName = keyValPair[0];
                        propVal = keyValPair[1];

                        if (!itemData[title][propName]) {
                            itemData[title][propName] = [];
                        }

                        itemData[title][propName] = _.union(_.compact(propVal.split(',')), itemData[title][propName]);
                    },
                    create: function (input) {
                        return {
                            value: input,
                            text: input
                        };
                    },
                    onChange: function (value) {
                        title = this.$input.parent().find('label')[0].dataset.title;
                        itemData = view.data.itemData;
                    },
                    createFilter: function (text) {
                        return (/^\w+:(?:\w+,?)+$/).test(text);
                    }
                };

                _.extend(options, {plugins: ['restore_on_backspace']});
                $item.selectize(options);
            });
        }
    });

    return ConditionAttrObjectView;
});
