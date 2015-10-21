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


define("org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/LegacyListItemView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, _, AbstractView, UIUtils) {
    return AbstractView.extend({
        data: {},
        mode: "append",
        render: function (itemData, callback, element, itemID) {
            this.setElement(element);
            this.data.itemID = itemID;
            this.data.itemData = itemData;

            var self = this;

            UIUtils.fillTemplateWithData(
                "templates/admin/views/realms/authorization/policies/conditions/LegacyListItem.html",
                this.data,
                function () {
                    self.setElement("#legacy_" + itemID);
                    self.delegateEvents();

                    self.$el.data("itemData", itemData);
                    if (callback) {
                        callback();
                    }
                });
        }
    });
});
