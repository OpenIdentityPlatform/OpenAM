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
    "org/forgerock/openam/ui/admin/utils/FormBuilder",
    "org/forgerock/openam/ui/admin/delegates/SMSDelegate"
], function(AbstractView, Configuration, Constants, FormBuilder, SMSDelegate) {
    var AdvancedSettings = AbstractView.extend({
        template: "templates/admin/views/console/realms/authentication/AdvancedSettingsTemplate.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        events: {},

        render: function(args, callback) {
            var self = this;

            this.data.realm = Configuration.globalData.auth.subRealm || "Top level Realm";
            this.data.name = args[0];
            this.data.consolePath = Constants.CONSOLE_PATH;

            SMSDelegate.getRealmAuthentication()
            .done(function(data) {
                self.data.sms = data;

                self.parentRender(function() {
                    self.$el.find("div.tab-pane").show(); // FIXME: To remove

                    self.$el.find("ul.nav.nav-tabs a").on('show.bs.tab', function(event) {
                        self.$el.find("div.panel-body").empty(); // FIXME: Improve

                        var id = $(event.target).attr('href').slice(1),
                            schema = self.data.sms.schema.properties[id],
                            element = $("div.panel-body").get(0);

                        FormBuilder.build(element, schema, self.data.sms.values);
                    });

                    self.$el.find('ul.nav a:first').tab('show');

                    if (callback) {
                        callback();
                    }
                });
            })
            .fail(function() {
                // TODO: Add failure condition
            });
        }
    });

    return new AdvancedSettings();
});
