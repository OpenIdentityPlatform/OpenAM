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

define("org/forgerock/openam/ui/admin/views/realms/services/NewServiceView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/SMSRealmService",
    "org/forgerock/openam/ui/admin/views/realms/services/renderForm",

    // jquery dependencies
    "bootstrap-tabdrop"
], function ($, _, Messages, AbstractView, EventManager, Router, Constants, SMSRealmService, renderForm) {

    return AbstractView.extend({
        template: "templates/admin/views/realms/services/NewServiceTemplate.html",
        events: {
            "click [data-save]": "onSave",
            "change #serviceSelection": "onSelectService"
        },

        render: function (args, callback) {
            var self = this;

            this.data.realmPath = args[0];

            SMSRealmService.services.type.getCreatables(this.data.realmPath).then(function (creatableTypes) {
                self.data.creatableTypes = creatableTypes.result;

                self.parentRender(function () {
                    if (self.data.creatableTypes.length > 1) {
                        self.$el.find("#serviceSelection").selectize();
                    } else if (self.data.creatableTypes[0] && self.data.creatableTypes[0]._id) {
                        self.selectService(self.data.creatableTypes[0]._id);
                    }
                    if (callback) { callback(); }
                });
            });
        },

        onSelectService: function (e) {
            this.selectService(e.target.value);
        },

        selectService: function (service) {
            var self = this;

            if (this.data.type !== service) {
                this.data.type = service;

                SMSRealmService.services.instance.getInitialState(this.data.realmPath, this.data.type)
                    .then(function (initialState) {
                        renderForm(self.$el, {
                            schema: initialState.schema,
                            values: initialState.values
                        }, function (form) {
                            self.form = form;
                        });
                    });
            }
        },

        onSave: function () {
            var self = this;

            SMSRealmService.services.instance.create(this.data.realmPath, this.data.type, this.form.data())
                .then(function () {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");

                    Router.routeTo(Router.configuration.routes.realmsServiceEdit, {
                        args: _.map([self.data.realmPath, self.data.type], encodeURIComponent),
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
