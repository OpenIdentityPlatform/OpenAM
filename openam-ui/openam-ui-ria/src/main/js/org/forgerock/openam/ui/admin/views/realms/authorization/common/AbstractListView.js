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
        },

        deleteRecord: function (e, id, callback) {
            var self = this,
                item = self.data.items.get(id),
                onSuccess = function () {
                    self.data.items.fetch({ reset: true });
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");

                    if (callback) {
                        callback();
                    }
                },
                onError = function (model, response) {
                    self.data.items.fetch({ reset: true });
                    Messages.addMessage({
                        response: response,
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
                args: _.map([self.realmPath, id], encodeURIComponent),
                trigger: true
            });
        },

        bindDefaultHandlers: function () {
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
