/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openam/ui/policy/ActionsView", [
    "org/forgerock/commons/ui/common/main/AbstractView"
], function (AbstractView) {
    var ActionsView = AbstractView.extend({
        element: "#actions",
        template: "templates/policy/ActionsTemplate.html",
        noBaseTemplate: true,
        events: {
            'click .striped-list li': 'toggleAction',
            'click span[class*="icon-radio-"]': 'changePermission',
            'click .striped-list .toggle-all': 'toggleAllActions'
        },

        render: function (args, callback) {
            _.extend(this.data, args);

            this.init();
            this.renderParent(callback);
        },

        renderParent: function (callback) {
            this.parentRender(function () {
                if (callback) {
                    callback();
                }
            });
        },

        init: function () {
            var self = this,
                data = this.data,
                entity = data.entity,
                availableActions,
                selectedActions,
                actionSelected;

            if (!entity.actions) {
                entity.actions = [];
            }

            availableActions = data.options.availableActions;
            selectedActions = entity.actions;

            this.data.selectedAll = false;

            if (!_.isEmpty(selectedActions)) {
                this.data.selectedAll = true;

                _.each(availableActions, function (action) {
                    actionSelected = typeof selectedActions[action.action] !== 'undefined';
                    self.data.selectedAll = self.data.selectedAll && actionSelected;
                    if (actionSelected) {
                        action.selected = true;
                        action.value = selectedActions[action.action];
                    }
                });
            }

            entity.actions = availableActions;
        },

        toggleAction: function (e) {
            var self = this,
                $target = $(e.target),
                $li,
                actionName;

            if ($target.is('span[class*="icon-radio-"]')) {
                return;
            }

            $li = $target.closest('li');
            actionName = $li.data('action-name');

            this.data.selectedAll = true;
            _.each(this.data.entity.actions, function (action) {
                if (action.action === actionName) {
                    action.selected = !action.selected;
                }
                self.data.selectedAll = self.data.selectedAll && action.selected;
            });

            this.renderParent();
        },

        changePermission: function (e) {
            var $target = $(e.target),
                permitted,
                actionName;

            if ($target.hasClass('icon-radio-checked')) {
                return;
            }

            permitted = $target.data('action-permission');
            actionName = $target.closest('li').data('action-name');

            _.find(this.data.entity.actions,function (action) {
                return action.action === actionName;
            }).value = permitted;

            this.renderParent();
        },

        toggleAllActions: function (e) {
            var self = this,
                actions = this.data.entity.actions;

            _.each(actions, function (action) {
                action.selected = !self.data.selectedAll;
            });

            this.data.selectedAll = !this.data.selectedAll;
            this.renderParent();
        }
    });

    return new ActionsView();
});
