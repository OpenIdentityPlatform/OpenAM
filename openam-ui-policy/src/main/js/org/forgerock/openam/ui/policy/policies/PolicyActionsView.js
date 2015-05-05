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

/*global define, $, _*/

define("org/forgerock/openam/ui/policy/policies/PolicyActionsView", [
    "org/forgerock/commons/ui/common/main/AbstractView" ,
    "org/forgerock/openam/ui/policy/common/StripedListView"
], function (AbstractView, StripedList) {

    var PolicyActionsView = AbstractView.extend({
        element: "#actions",
        template: "templates/policy/policies/PolicyActionsTemplate.html",
        noBaseTemplate: true,
        events: {
          'click .radio-inline': 'changePermission'
        },

        render: function (data, callback) {
            _.extend(this.data, data);

            var self = this,
                availableActions = data.options.availableActions,
                selectedActions = data.entity.actionValues,
                itemTpl = "templates/policy/policies/StripedListActionItemTemplate.html";

            this.selectedActions = [];

            if (!_.isEmpty(selectedActions)) {
                _.each(availableActions, function (action) {
                    if ( _.has(selectedActions, action.action)) {
                        data.options.availableActions = _.without(data.options.availableActions,  _.findWhere(data.options.availableActions, {action: action.action}));
                        self.selectedActions.push(action);
                        data.entity.actionValues[action.action] = action.value;
                    }
                });
            }

            this.parentRender(function () {
                var d1 = $.Deferred(), d2 = $.Deferred();

                this.actionsListView = new StripedList();
                this.actionsListView.render({
                    itemTpl: itemTpl,
                    items: this.data.options.availableActions,
                    title: $.t('policy.actions.availableActions'),
                    clickItem: this.selectAction.bind(this)
                }, '#availableActions', function () {
                    d1.resolve();
                });

                this.actionsListSelectedView = new StripedList();
                this.actionsListSelectedView.render({
                    itemTpl: itemTpl,
                    items: this.selectedActions,
                    title: $.t('policy.actions.selectedActions'),
                    created: true,
                    clickItem: this.deselectAction.bind(this)
                }, '#selectedActions', function () {
                    d2.resolve();
                });

                $.when(d1, d2).done(function () {
                    if (callback) {
                        callback();
                    }
                });
            });
        },

        selectAction: function (item) {
            var action = _.findWhere(this.data.options.availableActions, {action: item});
            this.moveSelected(action, this.actionsListView, this.actionsListSelectedView);

            this.data.options.availableActions = _.without(this.data.options.availableActions, action);
            this.selectedActions.push(action);

            this.data.entity.actionValues[action.action] = action.value;
        },

        deselectAction: function (item) {
            var action = _.findWhere(this.selectedActions, {action: item});
            this.moveSelected(action, this.actionsListSelectedView, this.actionsListView);

            this.selectedActions = _.without(this.selectedActions, action);
            this.data.options.availableActions.push(action);

            delete this.data.entity.actionValues[item];
        },

        moveSelected: function (item, fromView, toView) {
            fromView.removeItem(item);
            fromView.renderItems();

            toView.addItem(item);
            toView.renderItems();
        },

        changePermission: function (e) {
            var $target = $(e.target),
                permitted,
                actionName,
                itemSelected;

            permitted = ($target.val() || $target.find('input').val()) === 'true';
            actionName = $target.closest('li').data('listItem');
            itemSelected = $target.closest('li').data('itemSelected');

            _.find(itemSelected ? this.selectedActions : this.data.options.availableActions, function (action) {
                return action.action === actionName;
            }).value = permitted;

            this.data.entity.actionValues[actionName] = permitted;
        }
    });

    return new PolicyActionsView();
});