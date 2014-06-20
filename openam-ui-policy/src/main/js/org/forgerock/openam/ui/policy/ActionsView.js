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

/*global window, define, $, form2js, _, js2form, document, console */

define("org/forgerock/openam/ui/policy/ActionsView", [
    "org/forgerock/commons/ui/common/main/AbstractView"
], function (AbstractView) {
    var ActionsView = AbstractView.extend({
        element: "#actions",
        template: "templates/policy/ActionsTemplate.html",
        noBaseTemplate: true,
        events: {
            'click .toggle-action': 'toggleAction',
            'click .toggle-all-actions': 'toggleAllActions',
            'click input[type=radio]': 'changePermission'
        },

        render: function (args, callback) {
            _.extend(this.data, args);

            this.init();

            this.parentRender(function () {
                this.$toggleAll = this.$el.find('.toggle-all-actions');
            });
        },

        init: function () {
            var data = this.data,
                app = data.app,
                availableActions,
                selectedActions;

            if (!app.actions) {
                app.actions = [];
            }

            availableActions = data.typeActions[app.applicationType];
            selectedActions = app.actions;

            if (!_.isEmpty(selectedActions)) {
                _.each(availableActions, function (action) {
                    if (typeof selectedActions[action.action] !== 'undefined') {
                        action.selected = true;
                        action.value = selectedActions[action.action];
                    }

                });
            }

            app.actions = availableActions;
        },

        /**
         * Toggles action.
         */
        toggleAction: function (e) {
            var actionName = e.target.getAttribute('data-action-name');

            _.find(this.data.app.actions,function (action) {
                return action.action === actionName;
            }).selected = e.target.checked;
        },

        /**
         * Changes action permission.
         */
        changePermission: function (e) {
            var value = e.target.value,
                actionName = e.target.getAttribute('data-action-name');

            _.find(this.data.app.actions,function (action) {
                return action.action === actionName;
            }).value = value === 'Allow';
        },

        /**
         * Toggles all actions.
         */
        toggleAllActions: function (e) {
            var checked = e.target.checked,
                actions = this.data.app.actions;

            _.each(actions, function (action) {
                action.selected = checked;
            });

            this.render();

            this.$toggleAll.attr('checked', checked);
        }
    });

    return new ActionsView();
});
