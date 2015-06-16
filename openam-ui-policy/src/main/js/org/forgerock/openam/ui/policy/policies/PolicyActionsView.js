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
        element: '#actions',
        template: 'templates/policy/policies/PolicyActionsTemplate.html',
        noBaseTemplate: true,
        events: {
            'click .radio-inline': 'changePermission'
        },

        render: function (data, callback) {
            _.extend(this.data, data);

            var availableActions = _.cloneDeep(data.options.availableActions),
                selectedActions = [],
                itemTpl = 'templates/policy/policies/StripedListActionItemTemplate.html';

            _.each(data.entity.actionValues, function (value, key) {
                availableActions = _.without(availableActions, _.findWhere(availableActions, {action: key}));
                selectedActions.push({action: key, value: value});
            });

            this.parentRender(function () {
                var d1 = $.Deferred(), d2 = $.Deferred();

                this.actionsListView = new StripedList();
                this.actionsListView.render({
                    itemTpl: itemTpl,
                    items: availableActions,
                    title: $.t('policy.actions.availableActions'),
                    clickItem: this.selectAction.bind(this)
                }, '#availableActions', function () {
                    d1.resolve();
                });

                this.actionsListSelectedView = new StripedList();
                this.actionsListSelectedView.render({
                    itemTpl: itemTpl,
                    items: selectedActions,
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
            var action = _.findWhere(this.actionsListView.getAllItems(), {action: item}),
                cloned = _.clone(action);

            this.removeSelected(action, this.actionsListView);
            this.addSelected(cloned, this.actionsListSelectedView);

            this.data.entity.actionValues[action.action] = action.value;
        },

        deselectAction: function (item) {
            var action = _.findWhere(this.actionsListSelectedView.getAllItems(), {action: item}),
                initialAction = _.clone(_.findWhere(this.data.options.availableActions, {action: item}));

            this.removeSelected(action, this.actionsListSelectedView);
            this.addSelected(initialAction, this.actionsListView);

            delete this.data.entity.actionValues[item];
        },

        removeSelected: function (item, fromView) {
            fromView.removeItem(item);
            fromView.renderItems();
        },

        addSelected: function (item, toView) {
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

            _.findWhere(itemSelected ? this.actionsListSelectedView.getAllItems() : this.actionsListView.getAllItems(),
                function (action) {
                    return action.action === actionName;
                }).value = permitted;

            if (itemSelected) {
                this.data.entity.actionValues[actionName] = permitted;
            }
        }
    });

    return new PolicyActionsView();
});