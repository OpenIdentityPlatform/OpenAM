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

/*global define, _, $*/
define("org/forgerock/openam/ui/policy/common/AbstractListView", [
    "org/forgerock/openam/ui/policy/util/BackgridUtils",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function (BackgridUtils, Messages, AbstractView, EventManager, Constants, UIUtils) {

    return AbstractView.extend({
        toolbarTemplateID: '#gridToolbar',

        initialize: function () {
            AbstractView.prototype.initialize.call(this);

            this.events = {
                'click #deleteRecords': 'deleteRecords'
            };
        },

        deleteRecords: function (e) {
            e.preventDefault();

            if ($(e.target).hasClass('inactive')) {
                return;
            }

            var self = this,
                i = 0,
                item,
                onDestroy = function () {
                    self.data.selectedItems = [];
                    self.data.items.fetch({reset: true});

                    UIUtils.fillTemplateWithData(self.toolbarTemplate, self.data, function (tpl) {
                        self.$el.find(self.toolbarTemplateID).html(tpl);
                    });
                },
                onSuccess = function (model, response, options) {
                    onDestroy();
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, 'deleteSuccess');
                },
                onError = function (model, response, options) {
                    onDestroy();
                    Messages.messages.addMessage({message: response.responseJSON.message, type: 'error'});
                };

            for (; i < this.data.selectedItems.length; i++) {
                item = this.data.items.get(this.data.selectedItems[i]);

                item.destroy({
                    success: onSuccess,
                    error: onError
                });
            }
        },

        onRowSelect: function (model, selected) {
            var self = this;

            if (selected) {
                if (!_.contains(this.data.selectedItems, model.id)) {
                    this.data.selectedItems.push(model.id);
                }
            } else {
                this.data.selectedItems = _.without(this.data.selectedItems, model.id);
            }

            UIUtils.fillTemplateWithData(this.toolbarTemplate, this.data, function (tpl) {
                self.$el.find(self.toolbarTemplateID).html(tpl);
            });
        },

        bindDefaultHandlers: function () {
            var self = this;

            this.data.items.on("backgrid:selected", function (model, selected) {
                self.onRowSelect(model, selected);
            });

            this.data.items.on("backgrid:sort", BackgridUtils.doubleSortFix);
        }
    });
});