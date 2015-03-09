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

define( "org/forgerock/openam/ui/uma/views/resource/DialogRevokeAllResources", [
        "org/forgerock/commons/ui/common/components/Dialog",
        "org/forgerock/openam/ui/uma/views/share/CommonShare",
        "org/forgerock/openam/ui/uma/delegates/UMADelegate",
        "org/forgerock/commons/ui/common/main/Router",
        "org/forgerock/commons/ui/common/main/Configuration",
        "org/forgerock/commons/ui/common/main/EventManager",
        "org/forgerock/commons/ui/common/util/Constants"
], function(Dialog) {

    var DialogRevokeAllResources = Dialog.extend({
        baseTemplate:    "templates/common/DefaultBaseTemplate.html",
        contentTemplate: "templates/uma/views/resource/DialogRevokeAllResources.html",

        events: {
            "click #revokeConfirm": "onRevokeConfirm",
            "click #revokeCancel":  "close"
        },

        render: function(confirmCallback, callback) {

            this.actions =  [
                { type: "button", name: $.t("common.form.ok"), id: "revokeConfirm"},
                { type: "button", name: $.t("common.form.cancel"),  id: "revokeCancel"}
            ];

            this.confirmCallback = confirmCallback;
            $("#dialogs").hide();
            this.show(_.bind(function() {
                $("#dialogs").show();
                if(callback){callback();}
            }, this));
        },

        onRevokeConfirm: function(e){
            this.close(e);
            this.confirmCallback();
        }
    });

    return new DialogRevokeAllResources();
});
