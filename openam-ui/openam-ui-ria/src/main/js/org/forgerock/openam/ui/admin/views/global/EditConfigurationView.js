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

define("org/forgerock/openam/ui/admin/views/global/EditConfigurationView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/services/SMSGlobalService",
    "org/forgerock/openam/ui/admin/views/global/EditConfigurationBacklink",

    // jquery dependencies
    "bootstrap-tabdrop"
], ($, _, Messages, AbstractView, EventManager, Router, Constants, Form, SMSGlobalService,
    EditConfigurationBacklink) => {


    var EditConfigurationView = AbstractView.extend({
        template: "templates/admin/views/global/EditConfigurationTemplate.html",
        events: {
            "click [data-save]": "onSave",
            "show.bs.tab ul.nav.nav-tabs a": "renderTab"
        },

        render: function (args, callback) {
            this.data.id = args[0];
            SMSGlobalService.authentication.get(this.data.id).then((data) => {
                this.data.schema = data.schema;
                this.data.values = data.values;
                this.data.name = "TODO: return of the name"; // data.name
                this.data.tabbed = this.data.schema.grouped;

                this.parentRender(() => {
                    if (this.data.tabbed) {
                        this.$el.find("ul.nav a:first").tab("show");
                        this.$el.find(".tab-menu .nav-tabs").tabdrop();
                    } else {
                        this.form = new Form(
                            this.$el.find("#tabpanel")[0],
                            this.data.schema,
                            this.data.values
                        );
                    }
                    const backlink = new EditConfigurationBacklink({ el:"#backlink" });
                    backlink.render();
                    if (callback) { callback(); }
                });
            });
        },

        onSave: function () {
            SMSGlobalService.authentication.update(this.data.id, this.form.data())
                .then((data) => {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                    this.data.values = data;
                }, (response) => {
                    Messages.addMessage({
                        response: response,
                        type: Messages.TYPE_DANGER
                    });
                });
        },

        renderTab: function (event) {
            const tabId = $(event.target).data("tabId");
            const schema = this.data.schema.grouped ? this.data.schema.properties[tabId] : this.data.schema;
            const element = this.$el.find("#tabpanel").empty().get(0);
            this.form = new Form(element, schema, this.data.values[tabId]);
        }
    });

    return new EditConfigurationView();
});
