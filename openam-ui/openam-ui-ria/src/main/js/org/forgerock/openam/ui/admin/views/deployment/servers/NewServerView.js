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

define("org/forgerock/openam/ui/admin/views/deployment/servers/NewServerView", [
    "jquery",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/services/ServersService",
    "org/forgerock/openam/ui/admin/views/common/Backlink"
], ($, Messages, AbstractView, Router, ServersService, Backlink) => {
    const getTrimmedValue = (field) => field.val().trim();

    const NewServerView = AbstractView.extend({
        template: "templates/admin/views/deployment/servers/NewServerTemplate.html",
        events: {
            "click [data-create]": "createServer",
            "keyup [data-server-url]": "toggleCreateButton"
        },

        render () {
            this.parentRender(() => { new Backlink().render(); });
            return this;
        },

        createServer () {
            const serverUrl = getTrimmedValue(this.$el.find("[data-server-url]"));

            ServersService.servers.create({ "_id": serverUrl })
                .then(() => {
                    Router.routeTo(Router.configuration.routes.editServerGeneral, { args: [serverUrl], trigger: true });
                },
                (response) => { Messages.addMessage({ response, type: Messages.TYPE_DANGER }); }
            );
        },

        toggleCreateButton (event) {
            const serverUrl = getTrimmedValue($(event.currentTarget));
            const valid = serverUrl !== "";

            this.$el.find("[data-create]").prop("disabled", !valid);
        }
    });

    return new NewServerView();
});
