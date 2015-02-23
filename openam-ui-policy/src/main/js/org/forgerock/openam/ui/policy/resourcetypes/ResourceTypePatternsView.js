/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openam/ui/policy/resourcetypes/ResourceTypePatternsView", [
    "org/forgerock/commons/ui/common/main/AbstractView" ,
    "org/forgerock/openam/ui/policy/common/StripedListEditingView"
], function (AbstractView, StripedListEditingView) {

    function ResourceTypePatternsView() {
    }

    ResourceTypePatternsView.prototype = new StripedListEditingView();

    ResourceTypePatternsView.prototype.render = function (entity, actions, el, callback) {
        this.data = {};
        this.entity = entity;

        this.data.items = actions || [];

        this.baseRender(this.data, "templates/policy/resourcetypes/ResourceTypesPatternsTemplate.html", el, callback);
    };

    ResourceTypePatternsView.prototype.getPendingItem = function (e) {
        var editing = this.$el.find('.editing');
        return editing.find('[data-attr-add]').val().toString();
    };

    ResourceTypePatternsView.prototype.isValid = function (e) {
        return this.getPendingItem(e) !== '';
    };

    ResourceTypePatternsView.prototype.isExistingItem = function (itemPending, itemFromCollection) {
        return itemPending === itemFromCollection;
    };

    ResourceTypePatternsView.prototype.getCollectionWithout = function (e) {
        var itemName = $(e.target).parents('li').data('item-name').toString();
        return _.without(this.data.items, itemName);
    };

    ResourceTypePatternsView.prototype.updateEntity = function () {
        this.entity.patterns = this.data.items;
    };

    return ResourceTypePatternsView;
});