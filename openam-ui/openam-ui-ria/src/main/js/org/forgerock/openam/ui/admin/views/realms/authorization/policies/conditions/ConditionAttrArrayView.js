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


define("org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrArrayView", [
    "jquery",
    "underscore",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrBaseView",
    "org/forgerock/openam/ui/admin/delegates/PoliciesDelegate",

    // jquery dependencies
    "selectize"
], function ($, _, ConditionAttrBaseView, PoliciesDelegate) {
    return ConditionAttrBaseView.extend({
        template: "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrArray.html",
        MIN_QUERY_LENGTH: 1,
        IDENTITY_PLACEHOLDER: "console.authorization.policies.edit.subjectTypes.Identity.placeholder",
        SCRIPT_PLACEHOLDER: "console.authorization.policies.edit.conditionTypes.Script.placeholder",
        TIME_ZONE_PLACEHOLDER: "console.authorization.policies.edit.conditionTypes.SimpleTime.props.enterTimeZone",
        SCRIPT_TYPE: "scriptId",
        TIME_ZONE_TYPE: "enforcementTimeZone",
        IDENTITY_TYPES: ["users", "groups"],
        DEFAULT_TIME_ZONE: "GMT",

        render: function (data, element, callback) {

            // default to multiple selection if this option is not specified
            if (data.multiple === undefined) {
                data.multiple = true;
            }

            this.initBasic(data, element, "field-float-selectize data-obj");

            this.parentRender(function () {
                var view = this,
                    title = "",
                    text = "",
                    itemData,
                    options,
                    item, $item,
                    type;

                this.$el.find("select.selectize").each(function () {
                    item = this;
                    $item = $(this);
                    type = $item.parent().find("label").data().title;
                    options = {};

                    if ($item.data().source) {
                        if (type === view.SCRIPT_TYPE) {
                            _.extend(options, {
                                placeholder: $.t(view.SCRIPT_PLACEHOLDER),
                                preload: true,
                                sortField: "value",
                                load: function (query, callback) {
                                    view.loadFromDataSource.call(this, item, callback);
                                },
                                onChange: function (value) {
                                    title = this.$input.parent().find("label").data().title;
                                    text = this.$input.find(":selected").text();
                                    view.data.itemData[title] = value ? value : "";
                                    view.data.hiddenData[view.data.itemData.type] = text ? text : "";
                                }
                            });
                        } else if (type === view.TIME_ZONE_TYPE) {
                            _.extend(options, {
                                placeholder: $.t(view.TIME_ZONE_PLACEHOLDER),
                                preload: true,
                                sortField: "value",
                                render: {
                                    item: function (item) {
                                        return "<span class='time-zone-selected'>" + item.text + "</span>";
                                    }
                                },
                                load: function () {
                                    var selectize = this;
                                    $.ajax({
                                        url: "timezones.json",
                                        dataType: "json",
                                        cache: true
                                    }).done(function (data) {
                                        _.each(data.timezones, function (value) {
                                            selectize.addOption({ value: value, text: value });
                                        });
                                    });
                                },
                                onChange: function (value) {
                                    view.data.itemData.enforcementTimeZone = value ? value : view.DEFAULT_TIME_ZONE;
                                }
                            });
                        } else if (_.contains(view.IDENTITY_TYPES, type)) {
                            _.extend(options, {
                                placeholder: $.t(view.IDENTITY_PLACEHOLDER),
                                sortField: "value",
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

                                    view.data.itemData.subjectValues = _.without(view.data.itemData.subjectValues,
                                        universalid);
                                    delete view.data.hiddenData[type][universalid];
                                }
                            });
                        }
                    } else {
                        _.extend(options, {
                            delimiter: ",",
                            persist: false,
                            create: function (input) {
                                return {
                                    value: input,
                                    text: input
                                };
                            },
                            onChange: function (value) {
                                title = this.$input.parent().find("label").data().title;
                                itemData = view.data.itemData;
                                itemData[title] = value ? value : [];
                            }
                        });
                        if ($item.prev("label").data("title") === "dnsName") {
                            options.createFilter = function (text) {
                                return text.indexOf("*") === -1 || text.lastIndexOf("*") === 0;
                            };
                        }
                    }

                    _.extend(options, { plugins: ["restore_on_backspace"] });
                    $item.selectize(options);
                });

                if (callback) {
                    callback();
                }
            });
        },

        queryIdentities: function (item, query, callback) {
            var selectize = this;
            PoliciesDelegate.queryIdentities($(item).data().source, query)
                .done(function (data) {
                    _.each(data.result, function (value) {
                        selectize.addOption({ value: value, text: value });
                    });
                    callback(data.result);
                }).error(function (e) {
                    console.error("error", e);
                    callback();
                });
        },

        getUniversalId: function (item, type) {
            var self = this;
            PoliciesDelegate.getUniversalId(item, type).done(function (subject) {
                self.data.itemData.subjectValues = _.union(self.data.itemData.subjectValues, subject.universalid);
                self.data.hiddenData[type][subject.universalid[0]] = item;
            });
        },

        loadFromDataSource: function (item, callback) {
            var selectize = this;
            PoliciesDelegate.getDataByType($(item).data().source)
                .done(function (data) {
                    _.each(data.result, function (value) {
                        selectize.addOption({ value: value._id, text: value.name });
                    });
                    callback(data.result);
                }).error(function (e) {
                    console.error("error", e);
                    callback();
                });
        }
    });
});
