/**
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
 * Copyright 2015 ForgeRock AS.
 */

define("org/forgerock/openam/ui/admin/views/realms/authentication/EditModuleDialog", [
    "jquery",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/commons/ui/common/main/Router"
], function ($, BootstrapDialog, Router) {
    function closeDialog (dialog) {
        dialog.close();
    }
    function redirectAndClose (dialog) {
        Router.setUrl(dialog.options.data.link);
        dialog.close();
    }

    return function (name, chains, href) {
        BootstrapDialog.show({
            title: $.t("console.authentication.modules.inUse.title"),
            message: $.t("console.authentication.modules.inUse.message", {
                moduleName: name,
                usedChains: chains
            }),
            data: {
                link : href
            },
            buttons: [{
                label: $.t("common.form.cancel"),
                cssClass: "btn-default",
                action: closeDialog
            }, {
                label: $.t("common.form.yes"),
                cssClass: "btn-primary",
                action: redirectAndClose
            }]
        });
    };
});
