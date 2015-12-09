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


define("org/forgerock/openam/ui/admin/views/realms/authorization/policies/PolicyActionsView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, _, AbstractView, UIUtils) {
    var PolicyActionsView = AbstractView.extend({
        element: "#actions",
        template: "templates/admin/views/realms/authorization/policies/PolicyActionsTemplate.html",
        noBaseTemplate: true,
        events: {
            "click .radio-inline": "changePermission",
            "change .editing select": "selectAction",
            "click .add-item": "addAction",
            "click button[data-delete]": "deleteItem",
            "keyup button[data-delete]": "deleteItem"
        },

        render: function (data, callback) {
            _.extend(this.data, data);

            var availableActions = _.cloneDeep(data.options.availableActions),
                selectedActions = [];

            _.each(data.entity.actionValues, function (value, key) {
                availableActions = _.without(availableActions, _.find(availableActions, { action: key }));
                selectedActions.push({ action: key, value: value });
            });

            this.data.availableActions = availableActions;
            this.data.selectedActions = selectedActions;

            this.parentRender(function () {
                var d1 = $.Deferred(), d2 = $.Deferred();

                this.renderAvailableActions(function () {
                    d1.resolve();
                });

                this.renderSelectedActions(function () {
                    d2.resolve();
                });

                $.when(d1, d2).done(function () {
                    if (callback) {
                        callback();
                    }
                });
            });
        },

        renderAvailableActions: function (callback) {
            var self = this;
            UIUtils.fillTemplateWithData(
                "templates/admin/views/realms/authorization/policies/PolicyAvailableActionsTemplate.html",
                this.data,
                function (tpl) {
                    self.$el.find("#availableActions").html(tpl);
                    if (callback) {
                        callback();
                    }
                });
        },

        renderSelectedActions: function (callback) {
            var self = this;
            UIUtils.fillTemplateWithData(
                "templates/admin/views/realms/authorization/common/ActionsTableTemplate.html",
                { "items": this.data.selectedActions },
                function (tpl) {
                    self.$el.find("#selectedActions").html(tpl);
                    self.$el.find("button.add-item").prop("disabled", true);
                    if (callback) {
                        callback();
                    }
                });
        },

        selectAction: function () {
            this.$el.find("button.add-item").prop("disabled", false);
        },

        addAction: function (e) {
            e.preventDefault();

            var actionName = this.$el.find("select").val(),
                action = _.find(this.data.options.availableActions, { action: actionName }),
                cloned = _.clone(action);

            if (action) {
                this.data.availableActions = _.without(this.data.availableActions,
                    _.find(this.data.availableActions, { action: actionName })
                );
                this.renderAvailableActions();
                this.data.selectedActions.push(cloned);
                this.renderSelectedActions();

                this.data.entity.actionValues[action.action] = action.value;
            }
        },

        changePermission: function (e) {
            var $target = $(e.target),
                permitted = ($target.val() || $target.find("input").val()) === "true",
                actionName = $target.closest("tr").find(".action-name").text().trim();

            _.find(this.data.selectedActions, { action: actionName }).value = permitted;

            this.data.entity.actionValues[actionName] = permitted;
        },

        deleteItem: function (e) {
            if (e.type === "keyup" && e.keyCode !== 13) {
                return;
            }
            var $target = $(e.target),
                actionName = $target.closest("tr").find(".action-name").text().trim(),
                selectedAction = _.find(this.data.selectedActions, { action: actionName });

            this.data.selectedActions = _.without(this.data.selectedActions, selectedAction);
            this.renderSelectedActions();

            delete this.data.entity.actionValues[actionName];

            this.data.availableActions.push(selectedAction);
            this.renderAvailableActions();
        }
    });

    return new PolicyActionsView();
});
