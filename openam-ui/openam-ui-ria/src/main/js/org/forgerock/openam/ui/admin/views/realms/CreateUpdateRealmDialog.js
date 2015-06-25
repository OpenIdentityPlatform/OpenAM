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
 * Copyright 2015 ForgeRock AS.
 */

/*global define*/
define("org/forgerock/openam/ui/admin/views/realms/CreateUpdateRealmDialog", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "bootstrap-dialog",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/admin/delegates/SMSGlobalDelegate"
], function ($, _, AbstractView, BootstrapDialog, Form, FormHelper, SMSGlobalDelegate) {
    var CreateUpdateRealmDialog = AbstractView.extend({
        show: function (realmLocation) {
            var self = this,
                promise,
                newRealm = _.isEmpty(realmLocation);

            if (newRealm) {
                promise = SMSGlobalDelegate.realms.schema();
            } else {
                promise = SMSGlobalDelegate.realms.get(realmLocation);
            }

            promise.done(function(data) {
                var i18nTitleKey = newRealm ? "createTitle" : "updateTitle",
                    i18nButtonKey = newRealm ? "create" : "save";
                BootstrapDialog.show({
                    title: $.t("console.realms.createUpdateRealmDialog." + i18nTitleKey, { realmLocation: realmLocation }),
                    cssClass: "realm-dialog",
                    message: function (dialog) {
                        var element = $("<div></div>");
                        dialog.form = new Form(element[0], data.schema, data.values);
                        return element;
                    },
                    buttons: [{
                        label: $.t("common.form." + i18nButtonKey),
                        cssClass: "btn-primary",
                        action: function (dialog) {
                            var promise;

                            if (newRealm) {
                                promise = SMSGlobalDelegate.realms.create(dialog.form.data());
                            } else {
                                promise = SMSGlobalDelegate.realms.update(dialog.form.data().location, dialog.form.data());
                            }

                            promise.done(function() {
                                dialog.close();
                            });

                            FormHelper.bindSavePromiseToElement(promise, this);
                        }
                    }, {
                        label: $.t("common.form.cancel"),
                        action: function (dialog) {
                            dialog.close();
                        }
                    }],
                    onshown: function (dialog) {
                        self.dialogOnShown(dialog);
                    }
                });
            });
        },
        dialogOnShown: function (dialog) {
            dialog.$modalBody.find("[data-toggle='popover-realm-status']").popover({
                content: $.t("console.realms.realmStatusPopover.content"),
                placement: "left",
                title: $.t("console.realms.realmStatusPopover.title"),
                trigger: "focus"
            });
            dialog.$modalBody.find("[data-toggle='popover-realm-aliases']").popover({
                content: $.t("console.realms.realmAliasesPopover.content"),
                html: true,
                placement: "left",
                title: $.t("console.realms.realmAliasesPopover.title"),
                trigger: "focus"
            });
        }
    });

    return new CreateUpdateRealmDialog();
});
