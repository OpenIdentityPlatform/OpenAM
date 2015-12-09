/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */


define("org/forgerock/openam/ui/admin/views/realms/authorization/common/StripedListEditingView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "doTimeout"
], function ($, _, AbstractView, EventManager, Constants) {
    return AbstractView.extend({
        noBaseTemplate: true,

        events: {
            "click .fa-plus": "addItem",
            "keyup .fa-plus": "addItem",
            "keyup .editing input": "addItem",
            "click .editing button": "addItem",
            "keyup .editing button": "addItem",
            "click span[data-delete]": "deleteItem",
            "keyup span[data-delete]": "deleteItem"
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

                this.flashDomItem(this.$el.find(".text-success"), "text-success");

                if (callback) {
                    callback();
                }
            });
        },

        addItem: function (e) {
            if (e.type === "keyup" && e.keyCode !== 13) {
                return;
            }

            var pending = this.getPendingItem(e), // provide child implementation
                duplicateIndex = -1,
                counter = 0,
                self = this;

            if (!this.isValid(e)) { // provide child implementation
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidItem");
                this.flashDomItem(this.$el.find(".editing"), "text-danger");
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
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "duplicateItem");
                this.flashDomItem(this.$el.find(".list-group-item:eq(" + duplicateIndex + ")"), "text-danger");
            } else {
                this.data.items.push(pending);
                this.data.options.justAdded = pending;
                if (this.updateEntity) {
                    // provide child implementation
                    this.updateEntity();
                }
                this.renderParent(function () {
                    self.$el.find(".editing input[type=text]").focus();
                });
            }
        },

        deleteItem: function (e) {
            if (e.type === "keyup" && e.keyCode !== 13) {
                return;
            }

            this.data.items = this.getCollectionWithout(e); // provide child implementation
            if (this.updateEntity) {
                this.updateEntity();
            } // provide child implementation
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
