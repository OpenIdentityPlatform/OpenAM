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


define("org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/ResourceTypeActionsView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, _, AbstractView, EventManager, Constants, UIUtils) {

    return AbstractView.extend({
        element: "#actions",
        template: "templates/admin/views/realms/authorization/resourceTypes/ResourceTypesActionsTemplate.html",
        noBaseTemplate: true,
        events: {
            "click .radio-inline": "toggleRadio",
            "keyup .radio-inline": "toggleRadio",
            "click .editing button": "addItem",
            "keyup .editing button": "addItem",
            "click button[data-delete]": "deleteItem",
            "keyup button[data-delete]": "deleteItem"
        },
        render: function (data, el, callback) {
            var self = this;
            _.extend(this.data, data);
            this.element = el;

            this.parentRender(function () {
                self.renderActionsTable(callback);
            });
        },

        renderActionsTable: function (callback) {
            var self = this;
            UIUtils.fillTemplateWithData(
                "templates/admin/views/realms/authorization/common/ActionsTableTemplate.html",
                { "items": this.data.actions },
                function (tpl) {
                    self.$el.find("#createdActions").html(tpl);
                    if (callback) {
                        callback();
                    }
                });
        },

        updateEntity: function () {
            var actions = null;

            if (this.data.actions.length) {
                actions = {};
                this.data.actions.forEach(function (el) {
                    actions[el.name] = el.value;
                });
            }

            this.data.entity.actions = actions;
        },

        isExistingItem: function (itemPending, itemFromCollection) {
            return itemPending.name === itemFromCollection.name;
        },

        addItem: function (e) {
            if (e.type === "keyup" && e.keyCode !== 13) {
                return;
            }

            var editing = this.$el.find(".editing"),
                actionName = editing.find(".form-control").val(),
                pending = { "name": actionName, "value": true },
                duplicateIndex = -1,
                counter = 0,
                self = this;

            if (pending.name === "") {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidItem");
                return;
            }

            _.each(this.data.actions, function (item) {
                if (self.isExistingItem(pending, item)) {
                    duplicateIndex = counter;
                    return;
                }
                counter++;
            });

            if (duplicateIndex >= 0) {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "duplicateItem");
            } else {
                this.data.actions.push(pending);
                this.updateEntity();
                this.renderActionsTable(function () {
                    self.$el.find(".editing input[type=text]").val("").focus();
                });
            }
        },

        deleteItem: function (e) {
            if (e.type === "keyup" && e.keyCode !== 13) {
                return;
            }

            var $target = $(e.target),
                actionName = $target.closest("tr").find(".action-name").text().trim();

            this.data.actions = _.without(this.data.actions, _.find(this.data.actions, { name: actionName }));
            this.updateEntity();
            this.renderActionsTable();
        },

        toggleRadio: function (e) {
            var $target = $(e.target),
                permitted,
                actionName;

            permitted = $target.val() || $target.find("input").val();
            actionName = $target.closest("tr").find(".action-name").text().trim();

            if (!actionName) {
                return;
            }

            _.find(this.data.actions, function (action) {
                return action.name === actionName;
            }).value = (permitted === "true");

            this.updateEntity();
        }
    });
});
