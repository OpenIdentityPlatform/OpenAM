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
 * Copyright 2016 ForgeRock AS.
 */

define("org/forgerock/openam/ui/admin/views/deployment/NewSiteView", [
    "jquery",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/services/SitesService",
    "org/forgerock/openam/ui/common/views/jsonSchema/JSONSchemaView",
    "org/forgerock/openam/ui/admin/utils/FormHelper"
], ($, Messages, AbstractView, Router, SitesService, JSONSchemaView) => {

    const NewSiteView = AbstractView.extend({
        template: "templates/admin/views/deployment/NewSiteTemplate.html",
        events: {
            "click [data-create]": "onCreate"
        },

        render (args, callback) {
            SitesService.sites.getInitialState().then((data) => this.parentRender(() => {
                this.jsonSchemaView = new JSONSchemaView({
                    schema: data.schema,
                    values: data.values
                });
                $(this.jsonSchemaView.render().el).appendTo(this.$el.find("[data-site-form]"));
                if (callback) { callback(); }
            }));
        },

        onCreate () {
            const values = this.jsonSchemaView.values();
            const siteId = this.$el.find("[data-id]").val();
            values["_id"] = siteId;

            SitesService.sites.create(values)
                .then(() => { Router.routeTo(Router.configuration.routes.listSites, { args: [], trigger: true }); },
                (response) => { Messages.addMessage({ response, type: Messages.TYPE_DANGER }); }
            );
        }
    });

    return new NewSiteView();
});
