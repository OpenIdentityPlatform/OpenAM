/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define, $, _, Backbone*/

define("org/forgerock/openam/ui/uma/views/resource/ListResource", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/uma/views/resource/MyResourcesTab",
    "org/forgerock/openam/ui/uma/views/resource/SharedResourcesTab",
    "org/forgerock/openam/ui/uma/delegates/UMADelegate"
], function(AbstractView, BootstrapDialog, Constants, EventManager, MessageManager, MyResourcesTab, SharedResourcesTab, UMADelegate) {

    var ListResource = AbstractView.extend({
        template: "templates/uma/views/resource/ListResource.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        events: {
            'click button#revokeAll:not(.disabled)': 'onRevokeAll'
        },
        onRevokeAll: function() {
            BootstrapDialog.show({
                type: BootstrapDialog.TYPE_DANGER,
                title: $.t("uma.resources.show.revokeAll"),
                message: $.t("uma.resources.show.revokeAllResourcesMessage"),
                closable: false,
                buttons: [{
                    id: "btnOk",
                    label: $.t("common.form.ok"),
                    cssClass: "btn-primary btn-danger",
                    action: function(dialog) {
                        dialog.enableButtons(false);
                        dialog.getButton("btnOk").text($.t("common.form.working"));
                        UMADelegate.revokeAllResources().done(function() {
                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "revokeAllResourcesSuccess");
                        }).fail(function(error) {
                            MessageManager.messages.addMessage({ message: JSON.parse(error.responseText).message, type: "error"});
                        }).always(function() {
                            dialog.close();
                        });
                    }
                }, {
                    label: $.t("common.form.cancel"),
                    action: function(dialog) {
                        dialog.close();
                    }
                }]
            });
        },

        render: function(args, callback) {
            this.parentRender(function() {
                this.$el.find('[data-toggle="tooltip"]').tooltip();
                MyResourcesTab.render();
                SharedResourcesTab.render();
            });
        }
    });

    return new ListResource();
});