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

define("org/forgerock/openam/ui/admin/views/realms/services/EditServiceView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/SMSRealmService",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/admin/views/realms/services/renderForm",

    // jquery dependencies
    "bootstrap-tabdrop"
], function ($, _, Messages, AbstractView, EventManager, Router, Constants, SMSRealmService, FormHelper, renderForm) {

    function deleteService () {
        SMSRealmService.services.instance.remove().then(_.bind(function () {
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");

            Router.routeTo(Router.configuration.routes.realmsServices, {
                args: [encodeURIComponent(this.data.realmPath)],
                trigger: true
            });
        }, this), function (model, response) {
            Messages.addMessage({
                response: response,
                type: Messages.TYPE_DANGER
            });
        });
    }

    return AbstractView.extend({
        template: "templates/admin/views/realms/services/EditServiceTemplate.html",
        partials: [
            "templates/admin/views/realms/partials/_HeaderDeleteButton.html"
        ],
        events: {
            "click [data-save]": "onSave",
            "click [data-delete]": "onDelete"
        },

        render: function (args, callback) {
            this.data.realmPath = args[0];
            this.data.type = args[1];

            SMSRealmService.services.instance.get(this.data.realmPath, this.data.type)
                .then(_.bind(function (instance) {
                    this.data.schema = instance.schema;
                    this.data.values = instance.values;
                    this.data.name = instance.name;

                    this.parentRender(function () {
                        renderForm(this.$el.find("[data-service-form-holder]"), this.data, _.bind(function (form) {
                            this.form = form;
                        }, this));
                        if (callback) { callback(); }
                    });
                }, this));
        },

        onSave: function () {
            SMSRealmService.services.instance.update().then(function () {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
            });
        },

        onDelete: function (e) {
            e.preventDefault();

            FormHelper.showConfirmationBeforeDeleting({
                message: $.t("console.services.list.confirmDeleteSelected")
            }, _.bind(deleteService, this, e));
        }
    });
});
