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
        element: "#pickup-subject",
        events: {},
        data: {},  
        editItem: null,
        constants:{
            EDIT_START:'editStart',
            EDIT_STOP: 'editStop'
        },

        render: function(data) {
            this.setElement(this.element);
            this.data = data;
        },

        toggleEditing: function(e){

            e.stopPropagation();
 
            var self = this, 
                item = e.currentTarget === 'li' ? e.currentTarget : $(e.currentTarget).closest('li');

            if (self.editItem.hasClass('editing')) {

                self.newListItem(item);
                item.removeClass('editing');
                item.parent().find('li').eq(self.editItem.index()+1).remove();
                self.trigger(self.constants.EDIT_STOP);

            } else {

                self.newEditable();
                item.find('.item-subject-data div').each(function(i){

                    var each = self.editItem.find('.data-obj').eq(i);
                    each.children('input').val($(this).find('span').text()); 
                    each.children('select').val($(this).find('span').text());

                    if(i===0){
                        self.editItem.find('select#subjectType').trigger('change');
                    }
                });
                
                item.before(self.editItem);
                self.editItem.addClass('editing');
                self.trigger(self.constants.EDIT_START);
            }
        },


        clearEditable: function(){
            //TODO : unbind events
            this.$el.empty();
        },


        newEditable: function(){
            this.$el.html(uiUtils.fillTemplateWithData("templates/policy/EditSubjectTemplate.html", this.data));  
            this.$el.find('select#subjectType').on('change', _.bind(this.changeSubjectType, this));
            this.editItem = this.$el.find('.subject');
            this.editItem.find('.icon-cog').on('click', _.bind(this.toggleEditing, this));

        },

        newListItem: function(item){
     
            var self = this, html = '', label = '', input = '', itemData = {};

            item.find('.item-subject-data').children('.data-obj').each(function() {
                label = $(this).children('label');
                input = $(this).children('input').val() ||  $(this).children('select').val() || '';   
                html += '<div class="'+label.attr('for')+'"><h3>'+label.text()+'</h3><span>'+input+'</span></div>'; 
                itemData[label.attr('for')] = [label.text(), input]; 
            });
    
            item.data('subject',itemData);

            //TODO: unbind old events

            item.find('.item-subject-data').html(html);
            item.find('.icon-remove').bind("click", self.onDelete);    
            item.on('dblclick', _.bind(this.toggleEditing, this));

            
        },

        changeSubjectType: function(e) {

            //TODO: Check input is valid from list.

            var type     = e.target.value,
                typInput = this.editItem.find('#subjectType'),
                subInput = this.editItem.find('input[name=subjectList]'),
                valInput = this.editItem.find('input[name=attributeValue]'),
                dataList = $("#subjectList"),
                subList  = _.findWhere(this.data.subjects, {type: type}),
                options, i;
                subList  = subList ? subList.list : [];

            subInput.prev('label').text(type);

            if (type==='') {
                typInput.addClass('placeholderText');
                subInput.prop('placeholder','').val('');
            } else {
                typInput.removeClass('placeholderText');
                subInput.prop('placeholder','Find ' + type).val('');
            }

            dataList.empty();

            if(subList.length) {
                for(i=0; i<subList.length; i++) {
                    options = $("<option></option>").attr("value", subList[i].name);
                    dataList.append(options);
                }
            }
 
            switch(type){

                case 'Attribute Subject':
                    subInput.prop('readonly', false).prev('label').addClass('showLabel');
                    valInput.prop('readonly', false).prev('label').addClass('showLabel');
                break;

                case 'Virtual Subject':
                case 'Identity Repository User':
                case 'Identity Repository Group':
                    subInput.prop('readonly', false).prev('label').addClass('showLabel');
                    valInput.val('').prop('readonly', true).prev('label').removeClass('showLabel');
                break;

                default:
                    subInput.val('').prop('readonly', true).prev('label').removeClass('showLabel');
                    valInput.val('').prop('readonly', true).prev('label').removeClass('showLabel');
                break;

            }  
  
        },

        onDelete: function(e){
            var item = $(e.currentTarget).closest('li');
            //TODO : unbind events
            item.animate({height: 0, paddingTop: 0, paddingBottom: 0,marginTop: 0,marginBotttom: 0, opacity:0}, function(){
                item.remove();
            });
        }   

    });

    return new EditSubjectView();
});
