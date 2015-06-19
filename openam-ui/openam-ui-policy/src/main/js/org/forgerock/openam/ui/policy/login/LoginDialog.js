/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014-2015 ForgeRock AS.
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

/*global window, define, $*/

define("org/forgerock/openam/ui/policy/login/LoginDialog", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "bootstrap-dialog"
], function(AbstractView, Configuration, Constants, UIUtils, BootstrapDialog) {
    var LoginDialog = AbstractView.extend({
            template: "templates/policy/login/LoginDialog.html",
            data : {
                reauthUrl: Constants.host + "/"+ Constants.context + "?goto=" + encodeURIComponent(window.location.href)
            },
            render: function () {
                var self = this,
                    options = {
                        type: BootstrapDialog.TYPE_DEFAULT,
                        title: $.t("policy.common.sessionExpired"),
                        buttons: [{
                            label: $.t("common.form.close"),
                            cssClass: "btn-primary",
                            action: function(dialog) {
                                dialog.close();
                            }
                        }]
                    };

                UIUtils.fillTemplateWithData(self.template, self.data, function(template){
                    options.message = function(dialog) {
                        return $(template);
                    };

                    BootstrapDialog.show(options);
                });
            }
        });

    return new LoginDialog();

});