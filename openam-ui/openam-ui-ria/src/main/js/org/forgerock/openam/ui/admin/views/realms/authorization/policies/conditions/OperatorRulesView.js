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
 * Portions copyright 2014-2016 ForgeRock AS.
 */


define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, _, AbstractView, UIUtils) {
    return AbstractView.extend({
        template: "templates/admin/views/realms/authorization/policies/conditions/OperatorRulesTemplate.html",
        noBaseTemplate: true,
        events: {
            "change    > select": "onSelect",
            "onfocus   select": "checkOptions",
            "mousedown select": "checkOptions"
        },
        data: {},
        mode: "append",
        select: null,
        dropbox: null,

        operatorI18nKey: "console.authorization.policies.edit.operators.",

        render (args, element, itemID, firstChild, callback) {
            var self = this;

            this.data = $.extend(true, {}, args);
            this.data.itemID = itemID;
            this.data.firstChild = firstChild;

            _.each(this.data.operators, function (operator) {
                operator.i18nKey = self.operatorI18nKey + operator.title;
            });

            this.setElement(element);

            UIUtils.fillTemplateWithData(this.template, this.data, function (tpl) {
                self.$el.append(tpl);

                self.setElement(`#operator${itemID}`);
                self.select = self.$el.find("select");
                self.delegateEvents();

                self.select.focus().trigger("change");
                self.$el.data("logical", true);
                self.dropbox = self.$el.find(".dropbox");

                self.$el.find('.fa[data-toggle="popover"]').popover();

                if (callback) {
                    callback();
                }
            });
        },

        setValue (value) {
            this.select.focus().val(value).trigger("change");
        },

        rebindElement () {
            this.delegateEvents();
        },

        onSelect (e) {
            var item = $(e.currentTarget).parent(),
                value = e.currentTarget.value,
                itemData = {},
                schema = _.find(this.data.operators, function (obj) {
                    return obj.title === value;
                });

            itemData.type = schema.title;
            _.map(schema.config.properties, function (value, key) {
                itemData[key] = value;
            });

            item.data("itemData", itemData);

            _.each(this.data.operators, function (obj) {
                item.removeClass(obj.title.toLowerCase());
            });
            item.addClass(value.toLowerCase());
        },

        checkOptions (e) {
            var parent = $(e.target).parent(),
                dropbox = parent.children("ol.dropbox"),
                select = dropbox.parent().children("select"),
                option = null;

            if (dropbox.children(":not(.dragged)").length > 1) {
                _.each(this.data.operators, function (obj) {
                    option = select.find(`option[value="${obj.title}"]`);
                    var isDisabled = !!(obj.config.properties.condition || obj.config.properties.subject);
                    option.prop("disabled", isDisabled);
                });

            } else {
                select.children().prop("disabled", false);
            }
        }
    });
});
