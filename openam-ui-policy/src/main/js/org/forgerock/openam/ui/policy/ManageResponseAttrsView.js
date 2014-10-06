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

define("org/forgerock/openam/ui/policy/ManageResponseAttrsView", [
    "org/forgerock/commons/ui/common/main/AbstractView" ,
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"
], function (AbstractView, eventManager, constants) {
    var ManageResponseAttrsView = AbstractView.extend({
        element: "#responseAttrs",
        template: "templates/policy/ManageResponseAttrsTemplate.html",
        noBaseTemplate: true,
        events: {
            'click .icon-plus': 'addStaticAttr',
            'keyup .icon-plus': 'addStaticAttr',
            'keyup .editing input:last-of-type': 'addStaticAttr',
            'click .icon-close ': 'deleteAttr',
            'keyup .icon-close ': 'deleteAttr'
        },

        attrType: {STATIC: "Static", USER: "User"},

        init: function (args, callback) {
            _.extend(this.data, args);

            var attrsPresent = this.data.resourceAttributes && this.data.resourceAttributes.length > 0;
            this.data.staticAttributes = attrsPresent ? this.splitStaticAttrs() : [];
            this.data.userAttributes = attrsPresent ? _.where(this.data.resourceAttributes, {type: this.attrType.USER}) : [];
            this.data.attrType = this.attrType;

            this.render(this.data, callback);
        },

        render: function (args, callback) {
            this.parentRender(function () {
                if (callback) {
                    callback();
                }
            });
        },

        addStaticAttr: function (e) {
            if (e.type === 'keyup' && e.keyCode !== 13) {
                return;
            }

            var editing = this.$el.find('.editing'),
                key = editing.find('[data-attr-add-key]'),
                val = editing.find('[data-attr-add-val]'),
                attr = {};

            if (!this.isValid(key) || !this.isValid(val)) {
                return;
            }

            attr.propertyName = key.val();
            attr.propertyValues = val.val();

            if (_.findWhere(this.data.staticAttributes, attr)) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "duplicateAttribute");
            } else {
                this.data.staticAttributes.push(attr);
                this.render(this.data);
            }
        },

        isValid: function (input) {
            return input[0].checkValidity();
        },

        deleteAttr: function (e) {
            if (e.type === 'keyup' && e.keyCode !== 13) {
                return;
            }

            var type = $(e.currentTarget).parents('.striped-list').data().attrType,
                data = $(e.currentTarget).parent().data(),
                name,
                value;

            switch (type) {
                case this.attrType.STATIC:
                    name = data.attrKey.toString();
                    value = data.attrVal.toString();
                    this.data.staticAttributes = _.without(this.data.staticAttributes,
                        _.findWhere(this.data.staticAttributes, {propertyName: name, propertyValues: value}));
                    break;
                case this.attrType.USER:
                    name = data.attrKey.toString();
                    this.data.userAttributes = _.without(this.data.userAttributes,
                        _.findWhere(this.data.userAttributes, {propertyName: name}));
                    break;
            }

            this.render(this.data);
        },

        splitStaticAttrs: function () {
            var data = [],
                prop,
                i,
                length,
                attrs = _.where(this.data.resourceAttributes, {type: this.attrType.STATIC});

            for (prop in attrs) {
                if (attrs.hasOwnProperty(prop)) {
                    for (i = 0, length = attrs[prop].propertyValues.length; i < length; i++) {
                        data.push({
                            "type": this.attrType.STATIC,
                            "propertyName": attrs[prop].propertyName,
                            "propertyValues": attrs[prop].propertyValues[i]
                        });
                    }
                }
            }

            return data;
        },

        getCombinedResponseAttrs: function () {
            var staticAttrsToSave = [],
                groupedByName = _.groupBy(this.data.staticAttributes, function (attr) {
                    return attr.propertyName;
                }),
                attr,
                i,
                length,
                self = this;

            _.each(groupedByName, function (value, key, list) {
                attr = {};
                attr.type = self.attrType.STATIC;
                attr.propertyName = key;
                attr.propertyValues = [];
                for (i = 0, length = value.length; i < length; i++) {
                    attr.propertyValues.push(value[i].propertyValues);
                }
                staticAttrsToSave.push(attr);
            });

            return _.union(staticAttrsToSave, this.data.userAttributes);
        }
    });

    return new ManageResponseAttrsView();
});
