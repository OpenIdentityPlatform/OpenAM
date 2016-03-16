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

define("org/forgerock/openam/ui/admin/views/global/EditSiteView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/SitesService",
    "org/forgerock/openam/ui/common/views/jsonSchema/JSONSchemaView",
    "org/forgerock/openam/ui/admin/utils/FormHelper",

    // jquery dependencies
    "bootstrap-tabdrop"
], ($, _, Messages, AbstractView, EventManager, Router, Constants, SitesService, JSONSchemaView, FormHelper) => {

    function toggleSave (el, enable) {
        el.find("[data-save]").prop("disabled", !enable);
    }

    function deleteInstance (id, etag) {
        SitesService.sites.remove(id, etag).then(() => {
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
            Router.routeTo("listSites", {
                trigger: true
            });
        }, (response) => Messages.addMessage({
            response,
            type: Messages.TYPE_DANGER
        }));
    }

    const EditSitesView = AbstractView.extend({
        template: "templates/admin/views/global/EditSiteTemplate.html",
        partials: [
            "templates/admin/views/realms/partials/_HeaderDeleteButton.html"
        ],
        events: {
            "click [data-save]": "onSave",
            "click [data-delete]": "onDelete"
        },

        render (args, callback) {
            this.data.id = args[0];

            SitesService.sites.get(this.data.id).then((data) => {
                this.data.name = data.values.raw._id;
                this.data.etag = data.values.raw.etag;

                this.parentRender(() => {
                    if (this.jsonSchemaView) {
                        this.jsonSchemaView.remove();
                    }
                    this.jsonSchemaView = new JSONSchemaView({
                        schema: data.schema,
                        values: data.values,
                        onRendered: () => toggleSave(this.$el, true)
                    });
                    $(this.jsonSchemaView.render().el).appendTo(this.$el.find("[data-service-form]"));
                    if (callback) { callback(); }
                });
            });
        },

        onSave () {
            SitesService.sites.update(this.data.id, this.jsonSchemaView.values(), this.data.etag)
                .then(() => EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved"),
                (response) => Messages.addMessage({
                    response,
                    type: Messages.TYPE_DANGER
                }));
        },

        onDelete (e) {
            e.preventDefault();
            FormHelper.showConfirmationBeforeDeleting({
                message: $.t("console.services.list.confirmDeleteSelected")
            }, _.partial(deleteInstance, this.data.id, this.data.etag));
        }
    });

    return new EditSitesView();
});
