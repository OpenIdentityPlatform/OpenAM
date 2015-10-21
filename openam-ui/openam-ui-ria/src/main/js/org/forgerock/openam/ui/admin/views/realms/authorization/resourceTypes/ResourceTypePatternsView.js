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


define("org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/ResourceTypePatternsView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/common/StripedListEditingView"
], function ($, _, AbstractView, StripedListEditingView) {

    function ResourceTypePatternsView () {
    }

    // TODO: Rename StripedListEditingView or rewrite to bootstrap table
    ResourceTypePatternsView.prototype = new StripedListEditingView();

    ResourceTypePatternsView.prototype.render = function (entity, actions, el, callback) {
        this.data = {};
        this.entity = entity;

        this.data.items = actions || [];

        this.baseRender(this.data,
            "templates/admin/views/realms/authorization/resourceTypes/ResourceTypesPatternsTemplate.html",
            el, callback);
    };

    ResourceTypePatternsView.prototype.getPendingItem = function () {
        var editing = this.$el.find(".editing");
        return editing.find(".form-control").val().toString();
    };

    ResourceTypePatternsView.prototype.isValid = function (e) {
        return this.getPendingItem(e) !== "";
    };

    ResourceTypePatternsView.prototype.isExistingItem = function (itemPending, itemFromCollection) {
        return itemPending === itemFromCollection;
    };

    ResourceTypePatternsView.prototype.getCollectionWithout = function (e) {
        var itemName = $(e.target).parents("li").data("item-name").toString();
        return _.without(this.data.items, itemName);
    };

    ResourceTypePatternsView.prototype.updateEntity = function () {
        this.entity.patterns = this.data.items;
    };

    return ResourceTypePatternsView;
});
