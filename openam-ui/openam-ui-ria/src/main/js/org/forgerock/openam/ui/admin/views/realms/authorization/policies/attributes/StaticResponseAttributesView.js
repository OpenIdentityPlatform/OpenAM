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
 * Portions copyright 2014-2015 ForgeRock AS.
 */


define("org/forgerock/openam/ui/admin/views/realms/authorization/policies/attributes/StaticResponseAttributesView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/common/StripedListEditingView"
], function ($, _, AbstractView, StripedListEditingView) {
    function StaticResponseAttributesView () {
    }

    StaticResponseAttributesView.prototype = new StripedListEditingView();

    StaticResponseAttributesView.prototype.render = function (entity, staticAttributes, el, callback) {
        this.data = {};
        this.entity = entity;
        this.attrType = "Static";

        this.data.items = this.splitAttrs(staticAttributes);
        this.data.items = _.sortBy(this.data.items, "propertyName");

        this.events["change input"] = this.checkedRequired.bind(this);
        this.events["keyup input"] = this.checkedRequired.bind(this);

        this.baseRender(this.data,
            "templates/admin/views/realms/authorization/policies/attributes/StaticAttributesTemplate.html",
            el, callback);
    };

    StaticResponseAttributesView.prototype.getPendingItem = function () {
        var editing = this.$el.find(".editing"),
            key = editing.find("[data-attr-key]"),
            val = editing.find("[data-attr-val]"),
            attr = {};

        attr.propertyName = key.val();
        attr.propertyValues = val.val();

        return attr;
    };

    StaticResponseAttributesView.prototype.isValid = function () {
        var editing = this.$el.find(".editing"),
            key = editing.find("[data-attr-key]"),
            val = editing.find("[data-attr-val]");

        return _.every([key, val], function (input) {
            return input.val() !== "" && input[0].checkValidity();
        });
    };

    StaticResponseAttributesView.prototype.isExistingItem = function (itemPending, itemFromCollection) {
        return itemFromCollection.propertyName === itemPending.propertyName &&
            itemFromCollection.propertyValues === itemPending.propertyValues;
    };

    StaticResponseAttributesView.prototype.getCollectionWithout = function (e) {
        var data = $(e.currentTarget).parent().data(),
            key = data.attrKey.toString(),
            val = data.attrVal.toString();

        return _.without(this.data.items, _.find(this.data.items, { propertyName: key, propertyValues: val }));
    };

    StaticResponseAttributesView.prototype.splitAttrs = function (attrs) {
        var data = [],
            prop,
            i,
            length;

        for (prop in attrs) {
            if (attrs.hasOwnProperty(prop)) {
                for (i = 0, length = attrs[prop].propertyValues.length; i < length; i++) {
                    data.push({
                        "type": this.attrType,
                        "propertyName": attrs[prop].propertyName,
                        "propertyValues": attrs[prop].propertyValues[i]
                    });
                }
            }
        }

        return data;
    };

    StaticResponseAttributesView.prototype.getCombinedAttrs = function () {
        var data = [],
            groupedByName = _.groupBy(this.data.items, function (attribute) {
                return attribute.propertyName;
            }),
            attribute,
            i,
            length,
            self = this;

        _.each(groupedByName, function (value, key) {
            attribute = { type: self.attrType };
            attribute.propertyName = key;
            attribute.propertyValues = [];
            for (i = 0, length = value.length; i < length; i++) {
                attribute.propertyValues.push(value[i].propertyValues);
            }
            data.push(attribute);
        });

        return data;
    };

    StaticResponseAttributesView.prototype.checkedRequired = function (e) {
        var inputs = $(e.currentTarget).parent().find("input"),
            required = false;

        _.find(inputs, function (input) {
            if (input.value !== "") {
                required = true;
            }
        });

        inputs.prop("required", required);
    };

    return StaticResponseAttributesView;
});
