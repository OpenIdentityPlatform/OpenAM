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

    // jquery dependencies
    "selectize"
], function ($, _, ConditionAttrBaseView) {
    return ConditionAttrBaseView.extend({
        template: "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrObject.html",

        render (data, element, callback) {
            this.initBasic(data, element, "field-float-selectize data-obj");

            this.parentRender(function () {
                this.initSelectize();

                if (callback) {
                    callback();
                }
            });
        },

        initSelectize () {
            var view = this,
                title = "",
                itemData,
                options,
                keyValPair,
                propName,
                propVal,
                $item;

            this.$el.find("select.selectize").each(function () {
                $item = $(this);
                options = {
                    persist: false,
                    delimiter: ";",
                    onItemRemove (value) {
                        title = this.$input.parent().find("label").data().title;
                        itemData = view.data.itemData;
                        keyValPair = value.split(":");
                        delete itemData[title][keyValPair[0]];
                    },
                    onItemAdd (value) {
                        title = this.$input.parent().find("label").data().title;
                        itemData = view.data.itemData;
                        keyValPair = value.split(":");
                        propName = keyValPair[0];
                        propVal = keyValPair[1];

                        if (!itemData[title][propName]) {
                            itemData[title][propName] = [];
                        }

                        itemData[title][propName] = _.union(_.compact(propVal.split(",")), itemData[title][propName]);
                    },
                    create (input) {
                        return {
                            value: input,
                            text: input
                        };
                    },
                    onChange () {
                        title = this.$input.parent().find("label").data().title;
                        itemData = view.data.itemData;
                    },
                    createFilter (text) {
                        return (/^\w+:(?:\w+,?)+$/).test(text);
                    }
                };

                _.extend(options, { plugins: ["restore_on_backspace"] });
                $item.selectize(options);
            });
        }
    });
});
