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

/*global define*/
define("org/forgerock/openam/ui/admin/views/realms/authorization/common/AbstractListView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, _, Messages, AbstractView, EventManager, Router, BackgridUtils, Constants, UIUtils) {

    return AbstractView.extend({
        toolbarTemplateID: "#gridToolbar",

        initialize: function () {
            AbstractView.prototype.initialize.call(this);

            // TODO delete once policies view is changed
            this.events = {
                "click #deleteRecords": "deleteRecords"
            };
        },

        deleteRecord: function (e, id) {
            var self = this,
                item = self.data.items.get(id),
                onSuccess = function (model, response, options) {
                    self.data.items.fetch({reset: true});
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                },
                onError = function (model, response, options) {
                    self.data.items.fetch({reset: true});
                    Messages.messages.addMessage({
                        message: response.responseJSON.message,
                        type: Messages.TYPE_DANGER
                    });
                };

            item.destroy({
                success: onSuccess,
                error: onError
            });
        },

        editRecord: function (e, id, route) {
            var self = this;

            Router.routeTo(route, {
                args: [encodeURIComponent(self.realmPath), encodeURIComponent(id)],
                trigger: true
            });
        },

        // TODO delete once policies view is changed
        deleteRecords: function (e) {
            e.preventDefault();

            if ($(e.target).hasClass("inactive")) {
                return;
            }

            var self = this,
                i = 0,
                item,
                onDestroy = function () {
                    self.data.selectedItems = [];
                    self.data.items.fetch({reset: true});

                    self.renderToolbar();
                },
                onSuccess = function (model, response, options) {
                    onDestroy();
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                },
                onError = function (model, response, options) {
                    onDestroy();
                    Messages.messages.addMessage({message: response.responseJSON.message, type: "error"});
                };

            for (; i < this.data.selectedItems.length; i++) {
                item = this.data.items.get(this.data.selectedItems[i]);

                item.destroy({
                    success: onSuccess,
                    error: onError
                });
            }
        },

        // TODO delete once policies view is changed
        onRowSelect: function (model, selected) {
            if (selected) {
                if (!_.contains(this.data.selectedItems, model.id)) {
                    this.data.selectedItems.push(model.id);
                }
            } else {
                this.data.selectedItems = _.without(this.data.selectedItems, model.id);
            }

            this.renderToolbar();
        },

        bindDefaultHandlers: function () {
            var self = this;

            // TODO delete once policies view is changed
            this.data.items.on("backgrid:selected", function (model, selected) {
                self.onRowSelect(model, selected);
            });

            this.data.items.on("backgrid:sort", BackgridUtils.doubleSortFix);
        },

        renderToolbar: function () {
            var self = this;

            UIUtils.fillTemplateWithData(this.toolbarTemplate, this.data, function (tpl) {
                self.$el.find(self.toolbarTemplateID).html(tpl);
            });
        }
    });
});