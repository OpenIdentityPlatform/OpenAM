/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
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

define("org/forgerock/openam/ui/policy/resourcetypes/ResourceTypeActionsView", [
    "org/forgerock/commons/ui/common/main/AbstractView" ,
    "org/forgerock/openam/ui/policy/common/StripedListEditingView"
], function (AbstractView, StripedListEditingView) {

    function ResourceTypeActionsView() {
    }

    ResourceTypeActionsView.prototype = new StripedListEditingView();

    ResourceTypeActionsView.prototype.render = function (entity, actions, el, callback) {
        this.data = {};
        this.entity = entity;

        this.data.items = actions || [];

        this.events['click .radio-inline'] = this.toggleRadio.bind(this);
        this.events['keyup .radio-inline'] = this.toggleRadio.bind(this);

        this.baseRender(this.data, "templates/policy/resourcetypes/ResourceTypesActionsTemplate.html", el, callback);
    };

    ResourceTypeActionsView.prototype.toggleRadio = function (e) {
        var target = $(e.target),
            permitted,
            actionName,
            parent;

        parent = target.parents('li');

        permitted = target.val() || target.find('input').val();
        actionName = parent.data('item-name').toString();

        if (!actionName) {
            return;
        }

        _.find(this.data.items,function (action) {
            return action.name === actionName;
        }).value = (permitted === 'true');

        this.updateEntity();

        this.renderParent();
    };

    ResourceTypeActionsView.prototype.getPendingItem = function (e) {
        var editing = this.$el.find('.editing'),
            key = editing.find('.form-control'),
            input = editing.find('input:checked'),
            action = {};

        action.name = key.val();
        action.value = input.val() === 'true';

        return action;
    };

    ResourceTypeActionsView.prototype.isValid = function (e) {
        return this.getPendingItem(e).name !== '';
    };

    ResourceTypeActionsView.prototype.isExistingItem = function (itemPending, itemFromCollection) {
        return itemPending.name === itemFromCollection.name;
    };

    ResourceTypeActionsView.prototype.getCollectionWithout = function (e) {
        var itemName = $(e.target).parents('li').data('item-name');
        return _.without(this.data.items, _.findWhere(this.data.items, {name: itemName}));
    };

    ResourceTypeActionsView.prototype.updateEntity = function () {
        var actions = null;

        if (this.data.items.length) {
            actions = {};
            this.data.items.forEach(function (el) {
                actions[el.name] = el.value;
            });
        }

        this.entity.actions = actions;
    };

    return ResourceTypeActionsView;
});