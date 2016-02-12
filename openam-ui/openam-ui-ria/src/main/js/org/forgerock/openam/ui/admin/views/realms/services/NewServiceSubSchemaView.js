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

define("org/forgerock/openam/ui/admin/views/realms/services/NewServiceSubSchemaView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/realm/sms/ServicesService",
    "org/forgerock/openam/ui/admin/views/realms/services/renderForm",

    // jquery dependencies
    "bootstrap-tabdrop"
], function ($, _, Messages, AbstractView, EventManager, Router, Constants, ServicesService, renderForm) {

    return AbstractView.extend({
        template: "templates/admin/views/realms/services/NewServiceSubSchemaTemplate.html",
        partials: [
            "partials/breadcrumb/_Breadcrumb.html"
        ],
        events: {
            "click [data-save]": "onSave"
        },

        render: function (args, callback) {
            this.data.realmPath = args[0];
            this.data.serviceInstance = args[1];
            this.data.subSchemaType = args[2];

            this.data.backLink = {
                href: "#" + Router.getLink(Router.configuration.routes.realmsServiceEdit,
                    _.map([this.data.realmPath, this.data.serviceInstance], encodeURIComponent)),
                text: this.data.serviceInstance,
                icon: "fa-plug"
            };

            this.parentRender(function () {
                var self = this;

                ServicesService.type.subSchema.instance.getInitialState(
                    this.data.realmPath, this.data.serviceInstance, this.data.subSchemaType
                ).then(function (initialState) {
                    renderForm(self.$el, {
                        schema: initialState.subSchema,
                        values: initialState.values
                    }, function (form) {
                        self.form = form;
                        if (callback) { callback(); }
                    });
                });
            });
        },

        onSave: function () {
            var self = this;

            ServicesService.type.subSchema.instance.create(
                this.data.realmPath,
                this.data.serviceInstance,
                this.data.subSchemaType,
                "", // TODO pass an ID here
                this.form.data()
            ).then(function () {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");

                Router.routeTo(Router.configuration.routes.realmsServiceSubSchemaInstanceEdit, {
                    args: _.map([
                        self.data.realmPath, self.data.serviceInstance, self.data.subSchemaType, self.data.name
                    ], encodeURIComponent),
                    trigger: true
                });
            }, function (response) {
                Messages.addMessage({
                    response: response,
                    type: Messages.TYPE_DANGER
                });
            });
        }
    });
});
