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
 * @author Eugenia Sergueeva
 */

/*global window, define, $, form2js, _, js2form, document, console, Handlebars */

define("org/forgerock/openam/ui/policy/ResourcesListView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"
], function (AbstractView,eventManager,constants) {

    var ResourcesListView = AbstractView.extend({
        element: "#resourcesList",
        template: "templates/policy/ResourcesListTemplate.html",
        noBaseTemplate: true,
        events: {
            'click .icon-plus': 'addResource',
            'keyup .icon-plus': 'addResource',
            'keyup .editing input:last-of-type': 'addResource',
            'click .icon-close ': 'deleteResource',
            'keyup .icon-close ': 'deleteResource'
        },

        render: function (args, callback) {
            _.extend(this.data, args);

            if (this.data.entity.resources) {
                this.data.entity.resources = _.sortBy(this.data.entity.resources);
            } else {
                this.data.entity.resources = [];
            }

            var self = this;
            
            this.parentRender(function () {

                delete self.data.options.justAdded;
                self.flashDomItem( self.$el.find('.highlight-good'), 'highlight-good' );
    
                self.$el.find('.editing').find('input').autosizeInput({space:19});
                self.$el.find('.editing').find('input:eq(0)').focus().select();

                if(callback){
                     callback();
                }
        
            });
        },

        validate: function (inputs) {
            // This is very simple native validation for supporting browsers for now. 
            // More complexity to come later.
            var self = this;
                self.valid = true;

            _.find(inputs, function(input){
                // unsupporting browsers will return undefined not false
                if ( input.checkValidity() === false) {
                    self.valid = false;
                    return;
                }   
            });

            return self.valid;
        },

        addResource: function (e) {
            if (e.type === 'keyup' && e.keyCode !== 13) { return;}

            var resourceStr = this.$el.find('.editing').data().resource.replace('-*-', '̂'),
                inputs = this.$el.find('.editing').find('input'),
                strLength = resourceStr.length,
                resource = '',
                count = 0, 
                i = 0,
                duplicateIndex = -1;

            if( this.validate(inputs) === false){
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidResource");
                this.flashDomItem( this.$el.find('.editing'), 'invalid' );
                return;
            }
            
            for (i = 0; i < strLength; i++) {
                
                if (resourceStr[i] === '*'){
                    resource += inputs[count].value; 
                    count++;
                } else if (resourceStr[i] === '̂'){
                    resource += inputs[count].value === '̂' ? '-*-' : inputs[count].value ;
                    count++;
                } else {
                    resource += resourceStr[i];
                }
            } 

            duplicateIndex = _.indexOf(this.data.entity.resources, resource);

            if ( duplicateIndex >= 0 ) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "duplicateResource");
                this.flashDomItem( this.$el.find('#createdResources ul li:eq('+duplicateIndex+')'), 'highlight-warning' );
            } else {
                this.data.entity.resources.push(resource);
                this.data.options.justAdded = resource;
                this.render(this.data);
            }
            
        },
 
        deleteResource: function (e) {
            if (e.type === 'keyup' && e.keyCode !== 13) { return;}
            var resource = $(e.currentTarget).parent().data().resource;
            this.data.entity.resources = _.without(this.data.entity.resources, resource);
            this.render(this.data);
        },

        flashDomItem: function ( item, className ) {
            item.addClass(className);
            $.doTimeout(_.uniqueId(className), 2000, function() {
                item.removeClass(className);
            });
        }

    });

    Handlebars.registerHelper('policyEditorResourceHelper', function() {
        var result  = this.options.newPattern.replace('-*-', '̂');
        result = result.replace(/\*/g, '<input required type="text" value="*" placeholder="*" />');
        result = result.replace('̂',   '<input required type="text" value="-*-" placeholder="-*-" pattern="[^\/]+" />');

        return new Handlebars.SafeString(result);
    });
   
    return new ResourcesListView();
});
