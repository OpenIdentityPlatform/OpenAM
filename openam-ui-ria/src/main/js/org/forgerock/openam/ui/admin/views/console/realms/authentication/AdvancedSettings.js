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
    "org/forgerock/openam/ui/admin/views/console/realms/authentication/ComponentBuilder",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants"
], function(AbstractView, ComponentBuilder, Configuration, EventManager, Router, Constants) {

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
                var withDefaultsOmitted = _.omit(data._schema.properties, 'defaults'),
                    withIdsInArray = _.map(withDefaultsOmitted, function(value, key) {
                        value._id = key;
                        value._initial = data[key];
                        return value;
                    }),
                    sorted = _.sortBy(withIdsInArray, 'order'),
                    components = sorted.map(function(propery) {
                        return ComponentBuilder.create(propery.type, _.omit(propery, 'type'));
                    }),
                    wraps = $("<div/>");
                    _.forEach(components, function(component) {
                        wraps.append(component);
                    });

                self.parentRender(function() {
                    self.$el.find("#putItHere").append(wraps);
                });
            })
            .fail(function() {
                // TODO: Add failure condition
            });

            self.parentRender(function() {
                if (callback) { callback();}
            });
        }

    });

    return new AdvancedSettings();
});
