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

/*global window, define, $, form2js, _, js2form, document, console, Handlebars */

define("org/forgerock/openam/ui/policy/SelectRealmsView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/policy/ResourcesListView" ,
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"
], function (AbstractView, resourcesListView,  eventManager, constants) {
    var SelectRealmsView = AbstractView.extend({
        element: "#selectRealms",
        template: "templates/policy/SelectRealmsTemplate.html",
        noBaseTemplate: true,
        events: {
            'change input#siblingRealm': 'editSibling',
            'keyup  input#siblingRealm': 'editSibling',
            'change input#subRealm': 'editSubRealm',
            'keyup  input#subRealm': 'editSubRealm',
            'click .icon-plus': 'addRealm',
            'keyup .icon-plus': 'addRealm',
            'click .icon-close ': 'deleteRealm',
            'keyup .icon-close ': 'deleteRealm'
        },

        render: function (args, callback) {

            _.extend(this.data, args);

            var self = this,
                lastFSlash = this.data.options.realm.lastIndexOf('/');

            this.data.options.parentRealm = this.data.options.realm.substring(0,lastFSlash);
            this.data.options.currentRealm = this.data.options.realm.substring(lastFSlash+1);
            this.data.entity.realms = _.sortBy(this.data.entity.realms);

            this.parentRender(function () {

                delete self.data.options.justAdded;
                self.flashDomItem( self.$el.find('.highlight-good'), 'highlight-good');
               
                if (callback) {
                    callback();
                }
            });

        },

        addRealm: function (e) {

            var siblingRealm =  this.$el.find('#siblingRealm').val(),
                subRealm =      this.$el.find('#subRealm').val() || '',
                newRealm =         '',
                duplicateIndex = -1;

            if ( subRealm !== '' ){
                if(subRealm.charAt(0) !== '/'){
                    subRealm = '/' + subRealm;
                } 
            }

            newRealm =  this.data.options.parentRealm + '/' + siblingRealm + subRealm;  
            
            if(siblingRealm === ''){
                this.$el.find('#siblingRealm').focus();
                return;
            }

            duplicateIndex = _.indexOf(this.data.entity.realms, newRealm);

            if ( duplicateIndex >= 0 ) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "duplicateRealm");
                this.flashDomItem( this.$el.find('#selectedRealms ul li:eq('+duplicateIndex+')'), 'highlight-warning' );
            } else {
                this.data.entity.realms.push(newRealm); 
                this.data.options.justAdded = newRealm;
                this.render(this.data);  
            }
   
        },

        editSibling: function (e) {
            if (e.type === 'keyup' && e.keyCode === 191) { 
                e.currentTarget.value = e.currentTarget.value.replace(/\//g, '');
            }
            if (e.currentTarget.value !== this.data.options.currentRealm) {
                this.$el.find('#subRealm').addClass('readonly').prop('readonly', true).val('');
                this.$el.find('#lastFSlash').addClass('fade');
            } else {
                this.$el.find('#subRealm').removeClass('readonly').prop('readonly', false);
                this.$el.find('#lastFSlash').removeClass('fade');
            }
           
        },

        editSubRealm: function (e) {
            if (e.type === 'keyup' && e.keyCode === 191) { 
                e.currentTarget.value = e.currentTarget.value.replace(/\//g, '');
            }

            if (e.currentTarget.value !== '') {
                this.$el.find('#siblingRealm').addClass('readonly').prop('readonly', true).val(this.data.options.currentRealm);
            } else {
                this.$el.find('#siblingRealm').removeClass('readonly').prop('readonly', false);
            }
        },

        deleteRealm: function (e) {
            if (e.type === 'keyup' && e.keyCode !== 13) { return;}
            var realm = $(e.currentTarget).parent().data().realm;
            this.data.entity.realms = _.without(this.data.entity.realms, realm);
            this.render(this.data);
        },

        flashDomItem: function ( item, className ) {
            var self = this;
            item.addClass(className);
            $.doTimeout(_.uniqueId(className), 2000, function() {
                item.removeClass(className);
            });
        }
    });

    Handlebars.registerHelper('policyEditorRealmHelper', function(string) {
        var result  = string.replace(/\//g, '<span class="realm">/</span>');
        return new Handlebars.SafeString(result);
    });

    return new SelectRealmsView();
});