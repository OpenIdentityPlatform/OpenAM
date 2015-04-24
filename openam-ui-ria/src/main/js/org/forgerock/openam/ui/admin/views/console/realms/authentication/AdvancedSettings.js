/*
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

/*global define, $, _*/
define("org/forgerock/openam/ui/admin/views/console/realms/authentication/AdvancedSettings", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "jsonEditor"
], function(AbstractView, Configuration, Constants, JSONEditor) {
    var AdvancedSettings = AbstractView.extend({
        template: "templates/admin/views/console/realms/authentication/AdvancedSettingsTemplate.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        events: {},

        render: function(args, callback) {
            var self = this;

            this.data.realm = Configuration.globalData.auth.subRealm || "Top level Realm";
            this.data.name = args[0];
            this.data.consolePath = Constants.CONSOLE_PATH;

            // $.post('/openam/json/realm-config/authentication?_action=template')
            $.post('/openam/json/global-config/services/uma?_action=template')
            // $.post('/openam/json/global-config/authentication/modules/hotp?_action=template')
            .done(function(data) {
                data = self.sanitize(data);

                var wraps = $("<div/>").get(0),
                    schema = data._schema,
                    editor = new JSONEditor(wraps, {
                        disable_collapse: true,
                        disable_edit_json: true,
                        disable_properties: true,
                        iconlib: "fontawesome4",
                        schema: schema,
                        theme: 'bootstrap3'
                    });
                editor.setValue(_.omit(data, '_schema'));

                self.parentRender(function() {
                    self.$el.find("#putItHere").append(wraps);
                });
            })
            .fail(function() {
                // TODO: Add failure condition
            });
        },
        sanitize: function(data) {
            data._schema.title = "Authentication Core Settings";
            data._schema.type = "object";

            data._schema.properties = _.omit(data._schema.properties, 'defaults');

            _.forEach(data._schema.properties, function(property) {
                // TODO Sometimes property.propertyOrder will be property.order, will have to account for this
                property.propertyOrder = parseInt(property.order.slice(1), 10);
                delete property.order;
            });

            return data;
        }
    });

    return new AdvancedSettings();
});
