/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
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

 /*global $ _ define*/
define("org/forgerock/openam/ui/console/views/Settings", [
  "org/forgerock/commons/ui/common/main/AbstractView",
  "org/forgerock/openam/ui/console/views/ComponentBuilder"
], function(AbstractView, ComponentBuilder) {
    var Settings = AbstractView.extend({
        template: "templates/console/views/Settings.html",
        render: function(args, callback) {
            var self = this;

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
                    self.$el.find(".container div:last-child").append(wraps);
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

    return new Settings();
});
