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

define("org/forgerock/openam/ui/policy/OperatorRulesView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractView, uiUtils, eventManager, constants, conf) {
    var OperatorRulesView = AbstractView.extend({
        element: "#pickup-operator",
        events: {},
        data: {},  

        render: function(data, callback) {
            this.data = data;
            this.setElement(this.element);
        },

        newListItem: function(){
            this.$el.html(uiUtils.fillTemplateWithData("templates/policy/OperatorRulesTemplate.html", this.data));
            this.$el.find("li.operator").find("select").bind("change", this.onSelect).trigger("change");
            this.$el.find('.icon-remove').bind("click", this.onDelete); 
        },

        clearListItem: function(){
            //TODO : unbind events first
            this.$el.empty();
        },

        onSelect: function(e){
            
            var item = $(e.currentTarget).parent(),
                operator = e.currentTarget.value;
            item.removeClass('any').removeClass('all').removeClass('none');
            item.addClass(operator); 

            item.data('operator', operator);
        },


        onDelete: function(e){
            var item = $(e.currentTarget).closest('li');
            //TODO : unbind events
            item.animate({height: 0, paddingTop: 0, paddingBottom: 0,marginTop: 0,marginBotttom: 0, opacity:0}, function(){
                item.remove();
            });
        }

        // TODO...
        /*toggleClose: function(e){
            e.stopPropagation();
            var button = $(e.currentTarget),
                dropbox = button.parent().parent().children('.dropbox');

            if(button.hasClass('icon-folder-open')){
                button.removeClass('icon-folder-open').addClass('icon-folder');
                dropbox.addClass('closed');
            }else{
                button.addClass('icon-folder-open').removeClass('icon-folder');
                dropbox.removeClass('closed');
            }
        }*/
      

    });


    return new OperatorRulesView();
});
