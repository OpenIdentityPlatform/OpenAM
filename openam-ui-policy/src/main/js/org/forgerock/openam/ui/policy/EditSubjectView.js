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
            'click .icon-cog' :                   'toggleEditing',
            'keyup .icon-cog' :                   'toggleEditing',
            'dblclick' :                          'toggleEditing',
            'click .icon-remove' :                'onDelete',
            "keyup .icon-remove" :                'onDelete',
            'change select#selection' :           'changeSubjectType',
            'change .field-float-pattern input':  'changeInput'
        },
        element:'#pickUpItem',
        data: {},
        mode:'append',

        EDIT_START: 'editStart',
        EDIT_STOP:  'editStop',

        render: function( subjects, callback, element, itemID, htmlData ) {

            var self = this;
            this.setElement(this.element);

            this.data = $.extend(true, {}, subjects);
            this.data.itemID = itemID;

            this.$el.html(uiUtils.fillTemplateWithData("templates/policy/EditSubjectTemplate.html", this.data));
            this.setElement('#subject_' + itemID );
            this.delegateEvents();


            if (htmlData) {
                this.$el.data('config',htmlData.config);
                this.$el.data('title', htmlData.title);
                this.$el.find('select#selection').val(htmlData.title).trigger('change');
            }

            this.$el.find('select#selection').focus();

            if (callback) {callback();}
        },


        createListItem: function(subjects, deleteme, item){

            item.focus(); //  Required to trigger changeInput.
            this.data = subjects;
            var html = '<div><h3>Subject Type</h3><span>'+(item.data().title || "")+'</span></div>\n';
            if (item.data().config) {
                _.map(item.data().config.properties, function(value, key) {
                    html += '<div><h3>'+key+'</h3><span>'+value+'</span></div>\n';
                });
            }
            item.find('.item-subject-data').html(html);
            this.setElement('#'+item.attr('id') );
            this.delegateEvents();
        },

        toggleEditing: function(e){
            if (e.type === 'keyup' && e.keyCode !== 13) { return;}
            if (this.$el.hasClass('editing') ) {
                this.$el.removeClass('editing');
                this.trigger(this.EDIT_STOP, this.$el);
            } else {
                this.trigger(this.EDIT_START, this.$el);
            }

        },

        changeInput: function(e) {
            var label = $(e.currentTarget).prev('label').text();
            this.$el.data().config.properties[label] = e.currentTarget.value;
        },

        changeSubjectType: function(e) {
            e.stopPropagation();

            //TODO: Check input is valid from list
            var self         = this,
                data         = null,
                html         = '',
                subjectType  = e.target.value,
                delay        = self.$el.find('.field-float-pattern').length > 0 ? 500 : 0,
                buildHTML    = function(properties) {
                    var count = 0,
                        returnVal = '';
                    _.map(properties, function(value, key) {
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

            if (this.$el.data().config && this.$el.data().title === subjectType) {
                data = this.$el.data();
            } else {
                data =  _.findWhere(this.data.subjects, {title: subjectType}) || {};
            }

            self.$el.data('config',data.config);
            self.$el.data('title', data.title);

            if (data.config) {

                html = buildHTML(data.config.properties);

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

        },

        onDelete: function(e) {
            e.stopPropagation();
            if (e.type === 'keyup' && e.keyCode !== 13) { return;}
            var item = $(e.currentTarget).closest('li'), self = this;
            item.animate({height: 0, paddingTop: 0, paddingBottom: 0,marginTop: 0,marginBottom: 0, opacity:0}, function() {
                self.remove();
            });
        }

    });

    return EditSubjectView;
});
