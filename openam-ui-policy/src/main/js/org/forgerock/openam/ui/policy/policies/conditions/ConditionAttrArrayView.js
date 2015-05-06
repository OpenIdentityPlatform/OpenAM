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

define("org/forgerock/openam/ui/policy/policies/conditions/ConditionAttrArrayView", [
    "org/forgerock/openam/ui/policy/policies/conditions/ConditionAttrBaseView",
    "org/forgerock/openam/ui/policy/delegates/PolicyDelegate"
], function (ConditionAttrBaseView, policyDelegate) {
    var ConditionAttrArrayView = ConditionAttrBaseView.extend({
        template: 'templates/policy/policies/conditions/ConditionAttrArray.html',
        MIN_QUERY_LENGTH: 1,
        SCRIPT_TYPE: 'scriptId',
        IDENTITY_PLACEHOLDER: 'policy.subjectTypes.Identity.placeholder',
        SCRIPT_PLACEHOLDER: 'policy.conditionTypes.Script.placeholder',

        render: function (data, element, callback) {
            data.multiple = data.title !== this.SCRIPT_TYPE;
            this.initBasic(data, element, 'field-float-selectize data-obj');

            this.parentRender(function () {
                var view = this,
                    title = '',
                    itemData,
                    options,
                    item, $item,
                    type;

                this.$el.find('select.selectize').each(function () {
                    item = this;
                    $item = $(this);
                    type = $item.parent().find('label')[0].dataset.title;
                    options = {};

                    // special case for items with added data sets.
                    if (item.dataset && item.dataset.source) {
                        if (type === view.SCRIPT_TYPE) {
                            _.extend(options, {
                                placeholder: $.t(view.SCRIPT_PLACEHOLDER),
                                create: false,
                                preload: true,
                                maxItems: 1,
                                sortField: 'value',
                                load: function (query, callback) {
                                    view.loadFromDataSource.call(this, item, callback);
                                },
                                onChange: function (value) {
                                    title = this.$input.parent().find('label')[0].dataset.title;
                                    itemData = view.data.itemData;
                                    itemData[title] = value ? value : '';
                                }
                            });
                        } else {
                            // Currently this is only "Identity"
                            _.extend(options, {
                                placeholder: $.t(view.IDENTITY_PLACEHOLDER),
                                create: false,
                                sortField: 'value',
                                load: function (query, callback) {
                                    if (query.length < view.MIN_QUERY_LENGTH) {
                                        return callback();
                                    }
                                    view.queryIdentities.call(this, item, query, callback);
                                },
                                onItemAdd: function (item) {
                                    view.getUniversalId(item, type);
                                },

                                onItemRemove: function (item) {
                                    var universalid = _.findKey(view.data.hiddenData[type], function (obj) {
                                        return obj === item;
                                    });

                                    view.data.itemData.subjectValues = _.without(view.data.itemData.subjectValues, universalid);
                                    delete view.data.hiddenData[type][universalid];
                                }
                            });
                        }
                    } else {
                        _.extend(options, {
                            delimiter: ',',
                            persist: false,
                            create: function (input) {
                                return {
                                    value: input,
                                    text: input
                                };
                            },
                            onChange: function (value) {
                                title = this.$input.parent().find('label')[0].dataset.title;
                                itemData = view.data.itemData;
                                itemData[title] = value ? value : [];
                            }});
                        if ($item.prev('label').data('title') === 'dnsName') {
                            options.createFilter = function (text) {
                                return text.indexOf('*') === -1 || text.lastIndexOf('*') === 0;
                            };
                        }
                    }

                    _.extend(options, {plugins: ['restore_on_backspace']});
                    $item.selectize(options);
                });

                if (callback) {
                    callback();
                }
            });
        },

        queryIdentities: function (item, query, callback) {
            var selectize = this;
            policyDelegate.queryIdentities(item.dataset.source, query)
                .done(function (data) {
                    _.each(data.result, function (value) {
                        selectize.addOption({value: value, text: value});
                    });
                    callback(data.result);
                }).error(function (e) {
                    console.error('error', e);
                    callback();
                });
        },

        getUniversalId: function (item, type) {
            var self = this;
            policyDelegate.getUniversalId(item, type).done(function (subject) {
                self.data.itemData.subjectValues = _.union(self.data.itemData.subjectValues, subject.universalid);
                self.data.hiddenData[type][subject.universalid[0]] = item;
            });
        },

        loadFromDataSource: function(item, callback) {
            var selectize = this;
            policyDelegate.getDataByType(item.dataset.source)
                .done(function (data) {
                    _.each(data.result, function (value) {
                        selectize.addOption({value: value.name, text: value.name});
                    });
                    callback(data.result);
                }).error(function (e) {
                    console.error('error', e);
                    callback();
                });
        }
    });

    return ConditionAttrArrayView;
});