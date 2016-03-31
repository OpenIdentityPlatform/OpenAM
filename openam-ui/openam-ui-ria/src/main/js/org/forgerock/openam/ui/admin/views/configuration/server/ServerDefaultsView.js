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
 * Copyright 2016 ForgeRock AS.
 */

define("org/forgerock/openam/ui/admin/views/configuration/server/ServerDefaultsView", [
    "jquery",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/services/ServersService",
    "org/forgerock/openam/ui/common/views/jsonSchema/JSONSchemaView",

    // jquery dependencies
    "bootstrap-tabdrop"
], ($, Messages, AbstractView, EventManager, Constants, Form, ServersService, JSONSchemaView) => {

    function toggleSave (el, enable) {
        el.find("[data-save]").prop("disabled", !enable);
    }

    return AbstractView.extend({
        template: "templates/admin/views/configuration/server/EditServerDefaultsTemplate.html",
        events: {
            "click [data-save]": "onSave",
            "show.bs.tab ul.nav.nav-tabs a": "renderTab"
        },

        render (args, callback) {
            const sectionId = args[0];

            this.data.title = $.t(`console.common.navigation.${sectionId}`);

            ServersService.servers.defaults.get(sectionId).then((data) => {
                this.parentRender(() => {
                    if (this.jsonSchemaView) {
                        this.jsonSchemaView.remove();
                    }
                    this.jsonSchemaView = new JSONSchemaView({
                        schema: data.schema,
                        values: data.values,
                        onRendered: () => toggleSave(this.$el, true)
                    });
                    $(this.jsonSchemaView.render().el).appendTo(this.$el.find("[data-json-form]"));
                    if (callback) { callback(); }
                });
            }, (response) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
            });
        }
    });
});
