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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/admin/services/SMSServiceUtils",
    "org/forgerock/openam/ui/admin/services/realm/AuthenticationService",

    // jquery dependencies
    "bootstrap-tabdrop"
], function ($, _, AbstractView, Constants, EventManager, Form, Messages, SMSServiceUtils, AuthenticationService) {
    var SettingsView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/SettingsTemplate.html",
        events: {
            "click [data-revert]"          : "revert",
            "click [data-save]"            : "save",
            "show.bs.tab ul.nav.nav-tabs a": "renderTab"
        },

        render (args, callback) {
            var self = this;

            this.data.realmLocation = args[0];

            AuthenticationService.authentication.get(this.data.realmLocation).then((data) => {
                self.data.formData = data;

                self.parentRender(function () {
                    self.$el.find("div.tab-pane").show(); // FIXME: To remove
                    self.$el.find("ul.nav a:first").tab("show");

                    self.$el.find(".tab-menu .nav-tabs").tabdrop();

                    if (callback) {
                        callback();
                    }
                });
            }, (response) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
            });
        },
        renderTab (event) {
            this.$el.find("#tabpanel").empty();

            var id = $(event.target).attr("href").slice(1),
                schema = SMSServiceUtils.sanitizeSchema(this.data.formData.schema.properties[id]),
                element = this.$el.find("#tabpanel").get(0);

            this.data.form = new Form(element, schema, this.data.formData.values[id]);
            this.$el.find("[data-header]").parent().hide();
        },
        revert () {
            this.data.form.reset();
        },
        save () {
            var formData = this.data.form.data(),
                self = this;

            AuthenticationService.authentication.update(this.data.realmLocation, formData).then((data) => {
                // update formData for correct re-render tab after saving
                _.extend(self.data.formData.values, data);
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
            }, (response) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
            });
        }
    });

    return SettingsView;
});
