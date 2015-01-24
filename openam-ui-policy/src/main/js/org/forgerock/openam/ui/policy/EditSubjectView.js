/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
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

/*global window, define, $, _, document, console */

define("org/forgerock/openam/ui/policy/EditSubjectView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/policy/conditions/ConditionAttrArrayView",
    "org/forgerock/openam/ui/policy/conditions/ConditionAttrStringView"
], function (AbstractView, uiUtils, eventManager, constants, conf, ArrayAttr, StringAttr) {
    var EditSubjectView = AbstractView.extend({
        events: {
            'change select#selection': 'changeType'
        },
        data: {},
        subjectI18n: { 'key': 'policy.subjectTypes.', 'title': '.title', 'props': '.props.' },
        IDENTITY_RESOURCE: 'Identity',

        render: function (schema, callback, element, itemID, itemData) {
            var self = this;
            this.setElement(element);

            this.data = $.extend(true, [], schema);
            this.data.itemID = itemID;

            _.each(this.data.subjects, function (subj) {
                subj.i18nKey = $.t(self.subjectI18n.key + subj.title + self.subjectI18n.title);
            });

            this.data.subjects = _.sortBy(this.data.subjects, "i18nKey");

            this.$el.append(uiUtils.fillTemplateWithData("templates/policy/EditSubjectTemplate.html", this.data));
            this.setElement('#subject_' + itemID);

            if (itemData) {
                delete itemData.name; // Temporary fix: The name attribute is being added by the server after the policy is created.

                if (itemData.type === self.IDENTITY_RESOURCE) { // client side fix for 'Identity'
                    this.$el.data('hiddenData', self.getUIDsFromUniversalValues(itemData.subjectValues));
                }

                this.$el.data('itemData', itemData);
                this.$el.find('select#selection').val(itemData.type).trigger('change');
            }

            this.$el.find('select#selection').focus();

            if (callback) {
                callback();
            }
        },

        createListItem: function (allSubjects, item) {
            var self = this,
                itemToDisplay = null,
                itemData = item.data().itemData,
                hiddenData = item.data().hiddenData,
                type,
                html,
                list,
                mergedData;

            mergedData = _.merge({}, itemData, hiddenData);

            item.focus(); //  Required to trigger changeInput.
            this.data.subjects = allSubjects;

            if (mergedData && mergedData.type) {
                type = mergedData.type;
                itemToDisplay = {};

                _.each(mergedData, function (val, key) {
                    if (key === 'type') {
                        itemToDisplay['policy.common.type'] = $.t(self.subjectI18n.key + type + self.subjectI18n.title);
                    } else {
                        if (type === self.IDENTITY_RESOURCE) {
                            if (key !== 'subjectValues') { // Do not display the Identities subject values, but display the merged hidden data instead.
                                list = '';
                                _.forOwn(val, function (prop) {
                                    list += prop + ' ';
                                });

                                itemToDisplay[self.subjectI18n.key + type + self.subjectI18n.props + key] = list;
                            }
                        } else {
                            itemToDisplay[self.subjectI18n.key + type + self.subjectI18n.props + key] = val;
                        }
                    }
                });
            }

            html = uiUtils.fillTemplateWithData("templates/policy/ListItem.html", {data: itemToDisplay});
            item.find('.item-data').html(html);
            this.setElement('#' + item.attr('id'));
        },

        changeType: function (e) {
            e.stopPropagation();
            var self = this,
                itemData = {},
                hiddenData,
                selectedType = e.target.value,
                schema = _.findWhere(this.data.subjects, {title: selectedType}) || {},
                delay = self.$el.find('.field-float-pattern').length > 0 ? 500 : 0;

            if (this.$el.data().itemData && this.$el.data().itemData.type === selectedType) {
                itemData = this.$el.data().itemData;
                hiddenData = this.$el.data().hiddenData;
            } else {
                itemData = self.setDefaultJsonValues(schema);
                self.$el.data('itemData', itemData);
                hiddenData = itemData.type === self.IDENTITY_RESOURCE ? {'users': {}, 'groups': {}} : {};
                self.$el.data('hiddenData', hiddenData);
            }

            if (itemData) {
                self.animateOut();

                // setTimeout needed to delay transitions.
                setTimeout(function () {
                    self.$el.find('.no-float').remove();
                    self.$el.find('.clear-left').remove();

                    if (!self.$el.parents('#dropbox').length || self.$el.hasClass('editing')) {
                        self.buildHTML(itemData, hiddenData, schema);
                    }

                    self.animateIn();
                }, delay);
            }
        },

        buildHTML: function (itemData, hiddenData, schema) {
            var self = this,
                itemDataEl = this.$el.find('.item-data'),
                schemaProps = schema.config.properties,
                i18nKey;

            if (schema.title === self.IDENTITY_RESOURCE) {
                _.each(['users', 'groups'], function (identityType) {
                    new ArrayAttr().render({itemData: itemData, hiddenData: hiddenData, data: hiddenData[identityType], title: identityType, i18nKey: self.subjectI18n.key + schema.title + self.subjectI18n.props + identityType, dataSource: identityType}, itemDataEl);
                });
            } else {
                _.map(schemaProps, function (value, key) {
                    i18nKey = self.subjectI18n.key + schema.title + self.subjectI18n.props + key;

                    switch (value.type) {
                        case 'string':
                            new StringAttr().render({itemData: itemData, hiddenData: hiddenData, data: itemData[key], title: key, i18nKey: i18nKey}, itemDataEl);
                            break;
                        case 'array':
                            new ArrayAttr().render({itemData: itemData, hiddenData: hiddenData, data: itemData[key], title: key, i18nKey: i18nKey}, itemDataEl);
                            break;
                        default:
                            break;
                    }
                });
            }
            this.$el.find('.condition-attr').wrapAll('<div class="no-float"></div>');
        },

        setDefaultJsonValues: function (schema) {
            var itemData = {type: schema.title};
            _.map(schema.config.properties, function (value, key) {

                switch (value.type) {
                    case 'string':
                        itemData[key] = '';
                        break;
                    case 'array':
                        itemData[key] = [];
                        break;
                    default:
                        console.error('Unexpected data type:', key, value);
                        break;
                }
            });

            return itemData;
        },

        animateOut: function () {
            // hide all items except the title selector
            this.$el.find('.no-float').fadeOut(500);
            this.$el.find('.clear-left').fadeOut(500);
            this.$el.find('.field-float-pattern, .field-float-selectize')
                .find('label').removeClass('showLabel')
                .next('input, div input').addClass('placeholderText').prop('readonly', true);

            this.$el.removeClass('invalid-rule');
        },

        animateIn: function () {
            var self = this;
            setTimeout(function () {
                self.$el.find('.field-float-pattern, .field-float-selectize')
                    .find('label').addClass('showLabel')
                    .next('input, div input').removeClass('placeholderText').prop('readonly', false);
            }, 10);
        },

        getUIDsFromUniversalValues: function (values) {
            var returnObj = { users: {}, groups: {}},
                endIndex = -1,
                startIndex = String('id=').length;

            _.each(values, function (universalid) {
                endIndex = universalid.indexOf(',ou=');
                if (universalid.indexOf(',ou=user') > -1) {
                    returnObj.users[universalid] = universalid.substring(startIndex, endIndex);
                } else if (universalid.indexOf(',ou=group') > -1) {
                    returnObj.groups[universalid] = universalid.substring(startIndex, endIndex);
                }
            });

            return returnObj;
        }
    });

    return EditSubjectView;
});