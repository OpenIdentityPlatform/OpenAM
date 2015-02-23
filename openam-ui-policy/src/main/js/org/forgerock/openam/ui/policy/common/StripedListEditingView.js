/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

/*global window, define, $, _, document, console */

define("org/forgerock/openam/ui/policy/common/StripedListEditingView", [
    "org/forgerock/commons/ui/common/main/AbstractView" ,
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"
], function (AbstractView, eventManager, constants) {
    return AbstractView.extend({
        noBaseTemplate: true,

        events: {
            'click .icon-plus': 'addItem',
            'keyup .icon-plus': 'addItem',
            'keyup .editing input:last-of-type': 'addItem',
            'click .icon-close ': 'deleteItem',
            'keyup .icon-close ': 'deleteItem'
        },

        baseRender: function (data, tpl, el, callback) {
            this.data = data;
            this.data.options = {};

            this.template = tpl;
            this.element = el;

            this.renderParent(callback);
        },

        renderParent: function (callback) {
            this.parentRender(function () {

                delete this.data.options.justAdded;

                this.flashDomItem(this.$el.find('.highlight-good'), 'highlight-good');

                if (callback) {
                    callback();
                }
            });
        },

        addItem: function (e) {
            if (e.type === 'keyup' && e.keyCode !== 13) {
                return;
            }

            var pending = this.getPendingItem(e), // provide child implementation
                duplicateIndex = -1,
                counter = 0,
                self = this;

            if (!this.isValid(e)) { // provide child implementation
                return;
            }

            _.each(this.data.items, function (item) {

                if (self.isExistingItem(pending, item)) { // provide child implementation
                    duplicateIndex = counter;
                    return;
                }

                counter++;
            });

            if (duplicateIndex >= 0) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "duplicateItem");
                this.flashDomItem(this.$el.find('.striped-list ul li:eq(' + duplicateIndex + ')'), 'highlight-warning');
            } else {
                this.data.items.push(pending);
                this.data.options.justAdded = pending;
                if (this.updateEntity) { this.updateEntity(); } // provide child implementation
                this.renderParent();
            }
        },

        deleteItem: function (e) {
            if (e.type === 'keyup' && e.keyCode !== 13) {
                return;
            }

            this.data.items = this.getCollectionWithout(e); // provide child implementation
            if (this.updateEntity) { this.updateEntity(); } // provide child implementation
            this.renderParent();
        },

        flashDomItem: function (item, className) {
            item.addClass(className);
            $.doTimeout(_.uniqueId(className), 2000, function () {
                item.removeClass(className);
            });
        }
    });
});