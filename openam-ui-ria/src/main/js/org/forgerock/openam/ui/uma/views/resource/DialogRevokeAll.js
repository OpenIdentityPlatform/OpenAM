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

define( "org/forgerock/openam/ui/uma/views/resource/DialogRevokeAll", [
        "org/forgerock/commons/ui/common/components/Dialog",
        "org/forgerock/openam/ui/uma/views/share/CommonShare",
        "org/forgerock/openam/ui/uma/delegates/UmaDelegate",
        "org/forgerock/commons/ui/common/main/Router",
        "org/forgerock/commons/ui/common/main/Configuration",
        "org/forgerock/commons/ui/common/main/EventManager",
        "org/forgerock/commons/ui/common/util/Constants"
], function(Dialog, CommonShare, UmaDelegate, Router, Configuration, EventManager, Constants) {

    var DialogRevokeAll = Dialog.extend({
        baseTemplate:    "templates/common/DefaultBaseTemplate.html",
        contentTemplate: "templates/uma/views/resource/DialogRevokeAll.html",

        events: {
            "click #revokeConfirm": "onRevokeConfirm",
            "click #revokeCancel":  "close"
        },

        actions: [
            { type: "button", name: "Yes", id: "revokeConfirm"},
            { type: "button", name: "No",  id: "revokeCancel"}
        ],

        render: function(args, callback) {
            $("#dialogs").hide();
            this.show(_.bind(function() {
                $("#dialogs").show();
                if(callback){callback();}
            }, this));
        },

        // Override Dialog method
        onRevokeConfirm: function(e) {
            var self = this, msg = {};
            UmaDelegate.revokeAllResources().done(function(){
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "revokeAllResourcesSuccess");
                self.close();
            }).fail(function(error){
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "revokeAllResourcesFail");
                self.close();
            });
        }
    });

    return new DialogRevokeAll();
});
