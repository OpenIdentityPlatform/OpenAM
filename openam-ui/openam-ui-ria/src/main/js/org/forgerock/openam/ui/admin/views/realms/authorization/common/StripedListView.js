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
 * Copyright 2015-2016 ForgeRock AS.
 */


define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, _, AbstractView, UIUtils) {
    return AbstractView.extend({
        noBaseTemplate: true,
        template: "templates/admin/views/realms/authorization/common/StripedListWrapperTemplate.html",
        events: {
            "click [data-list-item]":   "clickItem",
            "keyup [data-list-item]":   "clickItem",
            "click [data-list-filter]": "filterItems",
            "keyup [data-list-filter]": "filterItems"
        },

        render (data, el, callback) {
            this.data = data;
            this.element = el;
            this.items = data.items ? _.cloneDeep(data.items).sort() : [];

            if (!this.data.itemTpl) {
                this.data.itemTpl = "templates/admin/views/realms/authorization/common/StripedListItemTemplate.html";
            }

            this.parentRender(function () {
                this.renderItems();

                if (callback) {
                    callback();
                }
            });
        },

        setItems (items) {
            this.items = _.cloneDeep(items).sort();
        },

        renderItems () {
            var self = this;
            this.data.items = this.filter ? this.getFilteredItems() : this.getAllItems();
            UIUtils.fillTemplateWithData(this.data.itemTpl, this.data, function (tpl) {
                self.$el.find(".list-group").html(tpl);
            });
        },

        clickItem (e) {
            if (e.type === "keyup") {
                switch (e.keyCode) {
                    case 38: // arrow down
                        $(e.target).prev("li").focus();
                        return;
                    case 40: // arrow up
                        $(e.target).next("li").focus();
                        return;
                    case 13:
                        break;
                    default:
                        return;
                }
            }

            if ((this.data.created && !$(e.target).is(".fa-close")) ||
                $(e.target).is(".radio-inline") ||
                $(e.target).parents(".radio-inline").length) {
                return;
            }

            var target = $(e.currentTarget),
                li = target.is("li") ? target : target.parents("li"),
                item = li.data("listItem");

            if (!item) {
                return;
            }

            if (this.data.clickItem) {
                this.data.clickItem(item);
            }
        },

        filterItems (e) {
            if (e.type === "keyup" && e.keyCode === 40) {
                $(e.target).parent().next().find("li:first-child").focus();
                return;
            }

            this.setFilter(e.currentTarget.value);
            this.renderItems();
        },

        emptyFilter () {
            this.setFilter("");

            this.$el.find("[data-list-filter]").val("");
        },

        setFilter (filter) {
            this.filter = filter.toString().toLowerCase();
        },

        getAllItems () {
            return this.items;
        },

        getFilteredItems () {
            var filter = this.filter;

            return _.filter(this.items, function (item) {
                return item.toString().toLowerCase().indexOf(filter) !== -1;
            });
        },

        removeItem (item) {
            this.items = _.without(this.items, item);
        },

        addItem (item) {
            this.items.push(item);
            this.items.sort();
        }
    });
});
