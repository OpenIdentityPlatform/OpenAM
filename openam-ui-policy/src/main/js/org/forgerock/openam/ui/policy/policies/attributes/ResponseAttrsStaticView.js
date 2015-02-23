/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openam/ui/policy/policies/attributes/ResponseAttrsStaticView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/policy/common/StripedListEditingView"
], function (AbstractView, StripedListEditingView) {

    function ResponseAttrsStaticView() {
    }

    ResponseAttrsStaticView.prototype = new StripedListEditingView();

    ResponseAttrsStaticView.prototype.render = function (entity, staticAttributes, el, callback) {
        this.data = {};
        this.entity = entity;

        this.data.items = this.splitAttrs(staticAttributes);
        this.data.items = _.sortBy(this.data.items, 'propertyName');

        this.events['change input'] = this.checkedRequired.bind(this);
        this.events['keyup input'] = this.checkedRequired.bind(this);

        this.baseRender(this.data, "templates/policy/policies/attributes/ResponseAttrsStatic.html", el, callback);
    };

    ResponseAttrsStaticView.prototype.getPendingItem = function (e) {
        var editing = this.$el.find('.editing'),
            key = editing.find('[data-attr-add-key]'),
            val = editing.find('[data-attr-add-val]'),
            attr = {};

        attr.propertyName = key.val();
        attr.propertyValues = val.val();

        return attr;
    };

    ResponseAttrsStaticView.prototype.isValid = function (e) {
        var editing = this.$el.find('.editing'),
            key = editing.find('[data-attr-add-key]'),
            val = editing.find('[data-attr-add-val]');

        return _.every([key, val], function (input) {
            return input.val() !== '' && input[0].checkValidity();
        });
    };

    ResponseAttrsStaticView.prototype.isExistingItem = function (itemPending, itemFromCollection) {
        return itemFromCollection.propertyName === itemPending.propertyName && itemFromCollection.propertyValues === itemPending.propertyValues;
    };

    ResponseAttrsStaticView.prototype.getCollectionWithout = function (e) {
        var data = $(e.currentTarget).parent().data(),
            key = data.attrKey.toString(),
            val = data.attrVal.toString();

        return _.without(this.data.items, _.findWhere(this.data.items, {propertyName: key, propertyValues: val}));
    };

    ResponseAttrsStaticView.prototype.splitAttrs = function (attrs) {
        var data = [],
            prop,
            i,
            length;

        for (prop in attrs) {
            if (attrs.hasOwnProperty(prop)) {
                for (i = 0, length = attrs[prop].propertyValues.length; i < length; i++) {
                    data.push({
                        "type": "Static",
                        "propertyName": attrs[prop].propertyName,
                        "propertyValues": attrs[prop].propertyValues[i]
                    });
                }
            }
        }

        return data;
    };

    ResponseAttrsStaticView.prototype.getCombinedAttrs = function () {
        var data = [],
            groupedByName = _.groupBy(this.data.items, function (attribute) {
                return attribute.propertyName;
            }),
            attribute,
            i,
            length,
            self = this;

        _.each(groupedByName, function (value, key) {
            attribute = {type: "User" };
            attribute.propertyName = key;
            attribute.propertyValues = [];
            for (i = 0, length = value.length; i < length; i++) {
                attribute.propertyValues.push(value[i].propertyValues);
            }
            data.push(attribute);
        });

        return data;
    };

    ResponseAttrsStaticView.prototype.checkedRequired = function (e) {
        var inputs = $(e.currentTarget).parent().find('input'),
            required = false;

        _.find(inputs, function (input) {
            if (input.value !== '') {
                required = true;
            }
        });

        inputs.prop('required', required);
    };

    return ResponseAttrsStaticView;
});