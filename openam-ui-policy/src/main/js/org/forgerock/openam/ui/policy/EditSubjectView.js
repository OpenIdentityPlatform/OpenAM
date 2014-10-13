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
    "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractView, uiUtils, eventManager, constants, conf) {

    var EditSubjectView = AbstractView.extend({

        events: {
            'change select#selection' :           'changeType',
            'change .field-float-pattern input':  'changeInput'
        },

        data: {},
        mode:'append',

        render: function( schema, callback, element, itemID, itemData ) {

            var self = this;
            this.setElement(element);

            this.data = $.extend(true, [], schema);
            this.data.itemID = itemID;

            this.$el.append(uiUtils.fillTemplateWithData("templates/policy/EditSubjectTemplate.html", this.data));
            this.setElement('#subject_' + itemID );
            this.delegateEvents();


            if (itemData) {
                // Temporay fix, the name attribute is being added by the server after the policy is created.
                delete itemData.name;
                this.$el.data('itemData',itemData);
                this.$el.find('select#selection').val(itemData.type).trigger('change');
            }

            this.$el.find('select#selection').focus();

            if (callback) {callback();}
        },

        createListItem: function(allSubjects, item){

            item.focus(); //  Required to trigger changeInput.
            this.data.subjects = allSubjects;
            var html = uiUtils.fillTemplateWithData("templates/policy/ListItem.html", {data:item.data().itemData});
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
                title = '';

            this.$el.find('.selectize').each(function(){

                $(this).selectize({
                    plugins: ['restore_on_backspace'],
                    delimiter: ',',
                    persist: false,
                    create: function(input) {
                        return {
                            value: input,
                            text: input
                        };
                    },
                    onChange: function(value){
                        title = this.$input.parent().find('label')[0].dataset.title;
                        if(title !== ''){
                             self.$el.data().itemData[title] = value;
                        }
                    }
                });

            });
        },

        changeType: function(e) {
            e.stopPropagation();
            var self         = this,
                itemData     = {},
                schema       = {},
                html         = '',
                selectize    = false,
                selectedType = e.target.value,
                delay        = self.$el.find('.field-float-pattern').length > 0 ? 500 : 0,
                buildHTML    = function(schemaProps) {

                    var count = 0,
                        returnVal = '<div class="no-float">';

                    _.map(schemaProps, function(value, key) {
                        if (value.type === 'string' ) {
                            returnVal += '\n'+ uiUtils.fillTemplateWithData("templates/policy/ConditionAttrString.html", {data:itemData[key], title:key, id:count});
                        } else if (value.type === 'array' ) {
                            returnVal += '\n'+ uiUtils.fillTemplateWithData("templates/policy/ConditionAttrArray.html", {data:itemData[key], title:key, id:count, tempData:self.weekdays});  
                        } else {
                            console.error('Unexpected data type:',key,value);
                        }

                        count++;
                    });

                    returnVal += '</div>';

                    return returnVal;
                };


            schema =  _.findWhere(this.data.subjects, {title: selectedType}) || {};

            if (this.$el.data().itemData && this.$el.data().itemData.type === selectedType) {
                itemData = this.$el.data().itemData;
            } else {
                itemData.type = schema.title;
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
                self.$el.data('itemData',itemData);
            }

            if (itemData) {

                html = buildHTML(schema.config.properties);

                this.$el.find('.no-float').fadeOut(500);
                this.$el.find('.clear-left').fadeOut(500);

                this.$el.find('.field-float-pattern input')
                    .addClass('placeholderText')
                    .prop('readonly', true)
                    .prev('label')
                    .removeClass('showLabel');

                // setTimeout needed to delay transitions.
                setTimeout( function() {

                    self.$el.find('.no-float').remove();
                    self.$el.find('.clear-left').remove();
                    self.$el.find('.field-float-select').after( html );

                    selectize =  _.find(schema.config.properties, function(item){
                        return item.type === 'array'; 
                    });

                    if(selectize){
                        self.initSelectize();
                    }  

                    

                    setTimeout( function() {

                        self.$el.find('.field-float-pattern input')
                            .removeClass('placeholderText')
                            .prop('readonly', false)
                            .prev('label')
                            .addClass('showLabel');

                        self.delegateEvents();

                    }, 10);
                }, delay);
            }

            
        }

    });

    return EditSubjectView;
});
