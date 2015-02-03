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
 */

/*global window, define, $, _, document, console */

define("org/forgerock/openam/ui/policy/referrals/SelectRealmsView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"
], function (AbstractView,  eventManager, constants) {
    var SelectRealmsView = AbstractView.extend({
        element: "#selectRealms",
        template: "templates/policy/referrals/SelectRealmsTemplate.html",
        noBaseTemplate: true,
        events: {
            'click .icon-plus': 'addRealm',
            'keyup .icon-plus': 'addRealm',
            'click .icon-close ': 'deleteRealm',
            'keyup .icon-close ': 'deleteRealm'
        },

        render: function (args, callback) {

            var self = this;
            _.extend(this.data, args);

            this.data.entity.realms = _.sortBy(this.data.entity.realms);
            this.data.options.selectableRealms = this.getSelectableRealms();

            this.parentRender(function () {

                delete self.data.options.justAdded;
                self.flashDomItem( self.$el.find('.highlight-good'), 'highlight-good');

                self.$el.find('#selectableRealms').selectize({
                    create: false
                });

                if (callback) {
                    callback();
                }
            });

        },

        getSelectableRealms: function () {

            var self = this,
                selectableRealms = $.extend([], self.data.options.filteredRealms);

            if (self.data.options.filteredRealms.length > 0) {
                _.each(self.data.entity.realms, function(realm){
                    selectableRealms = _.reject(selectableRealms,
                        function(selected) {
                            return selected === realm;
                        });
                });
            }

            return selectableRealms.length > 0 ? selectableRealms : null;
        },

        addRealm: function (e) {

            var newRealm =  this.$el.find('#selectableRealms').val(),
                duplicateIndex = -1;

            if(newRealm === ''){
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


        deleteRealm: function (e) {
            if (e.type === 'keyup' && e.keyCode !== 13) { return;}
            var realm = $(e.currentTarget).parent().data().realm;
            this.data.entity.realms = _.without(this.data.entity.realms, realm);
            this.render(this.data);
        },

        flashDomItem: function ( item, className ) {
            item.addClass(className);
            $.doTimeout(_.uniqueId(className), 2000, function() {
                item.removeClass(className);
            });
        }
    });

    return new SelectRealmsView();
});