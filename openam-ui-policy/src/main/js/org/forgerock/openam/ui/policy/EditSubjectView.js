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
            'change select#selection' :           'changeSubjectType',
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
                this.$el.data('itemData',itemData);
                this.$el.find('select#selection').val(itemData.type).trigger('change');
            }

            this.$el.find('select#selection').focus();

            if (callback) {callback();}
        },

        createListItem: function(allSubjects, item){

            item.focus(); //  Required to trigger changeInput.
            this.data.subjects = allSubjects;
            var html = '';
            if (item.data().itemData) {
                _.map(item.data().itemData, function(value, key) {
                    html += '<div><h3>'+key+'</h3><span>'+value+'</span></div>\n';
                });
            }
            if (html === '') {
                html = '<div class="invalid"><h3>blank</h3><span>edit rule...</span></div>';
            }
            item.find('.item-subject-data').html(html);
            this.setElement('#'+item.attr('id') );
            this.delegateEvents();
        },

        changeInput: function(e) {
            var label = $(e.currentTarget).prev('label').text();
            this.$el.data().itemData[label] = e.currentTarget.value;
        },

        changeSubjectType: function(e) {
            e.stopPropagation();

            var self         = this,
                itemData     = {},
                schema       = {},
                html         = '',
                selectedType = e.target.value,
                delay        = self.$el.find('.field-float-pattern').length > 0 ? 500 : 0,
                buildHTML    = function(properties) {
                    var count = 0,
                        returnVal = '';
                    _.map(properties, function(value, key) {
                        if(key === 'type') {
                            return; // the Type is rendered using the template
                        }
                        if (_.isString(value)) {

                            returnVal += '\n'+
                            '<div class="field-float-pattern data-obj">'+
                                '<label for="selection_' + (count) + '">' + key + '</label>'+
                                '<input type="text" id="selection_' + (count) + '" name="selection_' + (count) + '" placeholder="" value="' + value + '" readonly=true class="placeholderText" />'+
                            '</div>';
                        } else if (_.isArray(value)) {
                            // TODO ... We are assuming for now that items array will contain strings.
                            returnVal += '\n'+
                            '<div class="field-float-pattern data-obj">'+
                                '<label for="selection_' + (count) + '">' + key + '</label>'+
                                '<input type="text" id="selection_' + (count) + '" name="selection_' + (count) + '" placeholder="" value="TODO: Array UI" readonly=true class="placeholderText" />'+
                            '</div>';
                        } else if (_.isObject(value)) {
                            // TODO ...
                            console.log('TODO...');
                        }
                        count++;
                    });

                    return returnVal;
                };


            schema =  _.findWhere(this.data.subjects, {title: selectedType}) || {};

            if (this.$el.data().itemData && this.$el.data().itemData.type === selectedType) {
                itemData = this.$el.data().itemData;
            } else {
                itemData.type = schema.title;
                _.map(schema.config.properties, function(value, key) {
                    itemData[key] = value;
                });
                self.$el.data('itemData',itemData);
            }

            if (itemData) {

                html = buildHTML(itemData);

                this.$el.find('.field-float-pattern input')
                    .addClass('placeholderText')
                    .prop('readonly', true)
                    .prev('label')
                    .removeClass('showLabel');

                // setTimeout needed to delay transitions.
                setTimeout( function() {
                    self.$el.find('.field-float-pattern').remove();
                    self.$el.find('.field-float-select').after( html );

                    setTimeout( function() {

                        self.$el.find('.field-float-pattern input')
                            .removeClass('placeholderText')
                            .prop('readonly', false)
                            .prev('label')
                            .addClass('showLabel');

                        }, 10);
                }, delay);
            }

            this.delegateEvents();
        }

    });

    return EditSubjectView;
});
