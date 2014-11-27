/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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
 */

/*global window, define, $, form2js, _, js2form, document, console */

define("org/forgerock/openam/ui/policy/EditSubjectView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/policy/PolicyDelegate"
], function(AbstractView, uiUtils, eventManager, constants, conf, policyDelegate) {

    var EditSubjectView = AbstractView.extend({

        events: {
            'change select#selection' :           'changeType',
            'change .field-float-pattern input':  'changeInput'
        },

        data: {},
        mode:'append',
        subjectI18n: {
            'key': 'policy.subjectTypes.',
            'title': '.title',
            'props': '.props.'
        },
        MIN_QUERY_LENGTH: 1,
        IDENTITY_RESOURCE: 'Identity',

        render: function( schema, callback, element, itemID, itemData ) {

            var self = this;
            this.setElement(element);

            this.data = $.extend(true, [], schema);
            this.data.itemID = itemID;

            _.each(this.data.subjects, function (subj) {
                subj.i18nKey = $.t(self.subjectI18n.key + subj.title + self.subjectI18n.title);
            });

            this.data.subjects = _.sortBy(this.data.subjects, "i18nKey");

            this.$el.append(uiUtils.fillTemplateWithData("templates/policy/EditSubjectTemplate.html", this.data));
            this.setElement('#subject_' + itemID );
            this.delegateEvents();


            if (itemData ) {

                delete itemData.name; // Temporay fix: The name attribute is being added by the server after the policy is created.

                if(itemData.type === self.IDENTITY_RESOURCE){ // client side fix for 'Identity'
                    this.$el.data('hiddenData', self.getUIDsFromUniversalValues(itemData.subjectValues));
                }

                this.$el.data('itemData',itemData);
                this.$el.find('select#selection').val(itemData.type).trigger('change');
            }

            this.$el.find('select#selection').focus();

            if (callback) {callback();}
        },

        createListItem: function(allSubjects, item){
            var self = this,
                itemToDisplay = null,
                itemData = item.data().itemData,
                hiddenData = item.data().hiddenData,
                type,
                html,
                list,
                prop,
                mergedData;

            mergedData = _.merge({}, itemData, hiddenData);

            item.focus(); //  Required to trigger changeInput.
            this.data.subjects = allSubjects;

            if ( mergedData && mergedData.type ) {
                type = mergedData.type;
                itemToDisplay = {};

                _.each(mergedData, function (val, key) {

                    if (key === 'type') {
                        itemToDisplay['policy.common.type'] = $.t(self.subjectI18n.key + type + self.subjectI18n.title);
                    } else {

                        if (type === self.IDENTITY_RESOURCE) {

                            if (key !== 'subjectValues' ){ // Do not display the Identities subject values, but display the merged hidden data instead.
                                list = '';
                                _.forOwn(val, function(prop){
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
            this.setElement('#'+item.attr('id'));
            this.delegateEvents();
        },

        changeInput: function(e) {
            var label = $(e.currentTarget).prev('label').data().title;
            this.$el.data().itemData[label] = e.currentTarget.value;
        },

        initSelectize: function() {

            var self = this,
                title,
                options,
                additionalOptions;

            _.each ( this.$el.find('.selectize'), function(selector) {

                title = '';
                options = {
                    plugins: ['restore_on_backspace']
                };

                var type = $(selector).parent().find('label')[0].dataset.title;

                // special case for items with added data sets. Currently this is only "Identity"
                if (selector.dataset && selector.dataset.source) {

                    additionalOptions = {
                        placeholder: $.t(self.subjectI18n.key + 'Identity.placeholder'),
                        create: false,
                        sortField: 'value',
                        load: function(query, callback) {
                            if (query.length < self.MIN_QUERY_LENGTH) {
                                return callback();
                            }

                            var selectize = this;
                            policyDelegate.queryIdentities(selector.dataset.source, query)
                            .done(function(data){
                                _.each(data.result, function(value){
                                    selectize.addOption({value:value, text:value});
                                });
                                callback(data.result);
                            }).error(function(e){
                                console.error('error', e);
                                callback();
                            });
                        },

                        onItemAdd: function(item){

                            policyDelegate.getUniversalId(item, type).done(function(subject){
                                self.$el.data().itemData.subjectValues = _.union(self.$el.data().itemData.subjectValues, subject.universalid);
                                self.$el.data().hiddenData[type][subject.universalid[0]] = item;
                            });

                        },

                        onItemRemove: function(item){

                            var universalid  = _.findKey(self.$el.data().hiddenData[type], function(obj){
                                return obj === item;
                            });

                            self.$el.data().itemData.subjectValues = _.without(self.$el.data().itemData.subjectValues, universalid);
                            delete self.$el.data().hiddenData[type][universalid];
                        }
                    };

                } else {

                    additionalOptions = {
                        persist:false,
                        delimiter: ',',
                        create:function(input) {
                            return {
                                value: input,
                                text: input
                            };
                        },
                        onChange: function(value){
                            title = $(selector).parent().find('label')[0].dataset.title;
                            if(title !== ''){
                                 self.$el.data().itemData[title] = value;
                            }
                        }
                    };

                }

                $(selector).selectize(_.merge(options, additionalOptions));

            });

        },

        changeType: function(e) {
            e.stopPropagation();
            var self         = this,
                itemData     = {},
                hiddenData   = {},
                schema       = {},
                html         = '',
                selectize    = false,
                selectedType = e.target.value,
                delay        = self.$el.find('.field-float-pattern').length > 0 ? 500 : 0,
                i18nKey,
                buildHTML    = function(schemaProps) {

                    var count = 0,
                        returnVal = '<div class="no-float">';

                    if (schema.title === self.IDENTITY_RESOURCE){

                        _.each(['users', 'groups'], function(identityType){
                            returnVal += '\n'+ uiUtils.fillTemplateWithData("templates/policy/ConditionAttrArray.html", {
                                data:hiddenData[identityType],
                                title:identityType,
                                i18nKey: self.subjectI18n.key + schema.title + self.subjectI18n.props + identityType,
                                id:count,
                                dataSource: identityType
                            });
                            count++;

                        });

                    } else {

                        _.map(schemaProps, function(value, key) {

                            i18nKey = self.subjectI18n.key + schema.title + self.subjectI18n.props + key;

                            if (value.type === 'string' ) {
                                returnVal += '\n'+ uiUtils.fillTemplateWithData("templates/policy/ConditionAttrString.html", {data:itemData[key], title:key, i18nKey: i18nKey, id:count });
                            } else if (value.type === 'array' ) {
                                returnVal += '\n'+ uiUtils.fillTemplateWithData("templates/policy/ConditionAttrArray.html", {data:itemData[key], title:key, i18nKey: i18nKey, id:count });
                            } else {
                                console.error('Unexpected data type:',key,value);
                            }

                            count++;
                        });

                    }

                    returnVal += '</div>';

                    return returnVal;
                };


            schema =  _.findWhere(this.data.subjects, {title: selectedType}) || {};

            if (this.$el.data().itemData && this.$el.data().itemData.type === selectedType) {
                itemData = this.$el.data().itemData;
                hiddenData = this.$el.data().hiddenData;
            } else {

                itemData = self.setDefaultJsonValues(schema);
                self.$el.data('itemData',itemData);

                if (itemData.type === self.IDENTITY_RESOURCE) {
                    hiddenData = {'users':{}, 'groups':{}};
                    self.$el.data('hiddenData', hiddenData );
                }

            }

            if (itemData) {

                html = buildHTML(schema.config.properties);
                self.animateOut();

                // setTimeout needed to delay transitions.
                setTimeout( function() {

                    self.$el.find('.no-float').remove();
                    self.$el.find('.clear-left').remove();
                    self.$el.find('#typeSelector').after( html );

                    self.initOptions(itemData, schema);
                    self.animateIn();

                }, delay);
            }
        },

        initOptions: function(itemData, schema){

            var selectize =  _.find(schema.config.properties, function(item){
                return item.type === 'array';
            });

            if (selectize) {
                this.initSelectize();
            }
        },

        setDefaultJsonValues: function(schema){

            var itemData = {type: schema.title};
            _.map(schema.config.properties, function(value,key) {

                switch (value.type) {
                    case 'string':
                        itemData[key] = '';
                    break;
                    case 'array':
                        itemData[key] = [];
                    break;
                    default:
                        console.error('Unexpected data type:',key,value);
                    break;
                }

            });

            return itemData;
        },

        animateOut: function(){
            // all items except the title selector are contained inside either a no-float or a clear-left
            this.$el.find('.no-float').fadeOut(500);
            this.$el.find('.clear-left').fadeOut(500);
            this.$el.find('.field-float-pattern, .field-float-selectize')
                .find('label').removeClass('showLabel')
                .next('input, div input').addClass('placeholderText').prop('readonly', true);

            this.$el.removeClass('invalid-rule');
        },

        animateIn: function(){

            var self = this;
            setTimeout( function() {
                self.$el.find('.field-float-pattern, .field-float-selectize')
                    .find('label').addClass('showLabel')
                    .next('input, div input').removeClass('placeholderText').prop('readonly', false);

                self.delegateEvents();
            }, 10);
        },

        getUIDsFromUniversalValues: function(values) {

            var returnObj = { users:{}, groups:{}},
                endIndex = -1,
                startIndex = String('id=').length;

            _.each(values,function(universalid){
                endIndex = universalid.indexOf(',ou=');
                if(universalid.indexOf(',ou=user') > -1){
                    returnObj.users[universalid]=  universalid.substring(startIndex,endIndex);
                } else if(universalid.indexOf(',ou=group') > -1){
                    returnObj.groups[universalid]=  universalid.substring(startIndex,endIndex);
                }
            });

            return returnObj;
        }

    });

    return EditSubjectView;
});
