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

/*global define, $, _ */

define("org/forgerock/openam/ui/uma/views/resource/DialogRevokeAllPolicies", [
    "org/forgerock/commons/ui/common/components/Dialog",
    "org/forgerock/openam/ui/uma/views/resource/EditResource",
    'org/forgerock/commons/ui/common/main/EventManager',
    'org/forgerock/commons/ui/common/util/Constants'
], function(Dialog, EditResourceView, EventManager, Constants) {
    var DialogRevokeAllPolicies = Dialog.extend({
        contentTemplate: "templates/uma/views/resource/DialogRevokeAllPolicies.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        events: {
            "click #revokeConfirm": "onRevokeConfirm",
            "click #revokeCancel":  "close"
        },
        render: function(id, callback) {
            this.action = [
                { type: "button", name: $.t("common.form.cancel"),  id: "revokeCancel"},
                { type: "button", name: $.t("common.form.reset"), id: "revokeConfirm"}
            ];
            $("#dialogs").hide();
            this.show(_.bind(function() {
                $("#dialogs").show();
            }, this));
        },
        onRevokeConfirm: function(e){
            if (EditResourceView.model.get('policy')) {
                EditResourceView.model.get('policy').destroy().done(function (response) {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "revokeAllPoliciesSuccess");
                    EditResourceView.render();
                }).fail(function (error) {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "revokeAllPoliciesFail");
                });
                this.close(e);
            } else {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "revokeAllPoliciesFail");
                this.close(e);
            }
        }
    });

    return new DialogRevokeAllPolicies();
});

