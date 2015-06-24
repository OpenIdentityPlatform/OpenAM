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
define("org/forgerock/openam/ui/admin/views/realms/AddNewRealmDialog", [
    'jquery',
    'underscore',
    'org/forgerock/commons/ui/common/main/AbstractView',
    'bootstrap-dialog',
    'org/forgerock/openam/ui/admin/models/Form',
    'org/forgerock/commons/ui/common/main/Router',
    'org/forgerock/openam/ui/admin/delegates/SMSGlobalDelegate',
    'org/forgerock/commons/ui/common/util/UIUtils'
], function ($, _, AbstractView, BootstrapDialog, Form, Router, SMSGlobalDelegate, UIUtils) {
    var AddNewRealmDialog = AbstractView.extend({
        show: function (realm) {
            var self = this;
            SMSGlobalDelegate.realms.schema().done(function (data) {
                BootstrapDialog.show({
                    title: $.t('console.realms.newRealmDialog.title'),
                    cssClass: 'realm-dialog',
                    message: function (dialog) {
                        var element = $('<div></div>');
                        dialog.form = new Form(element[0], data.schema, data.values);
                        return element;
                    },
                    buttons: [{
                        label: $.t('common.form.save'),
                        cssClass: 'btn-primary',
                        action: function (dialog) {
                            var newRealm = {},
                                aliases = dialog.$modalBody.find('#realmAliases')[0].selectize.getValue(),
                                location = dialog.$modalBody.find('#realmLocation').val();
                            newRealm.name = dialog.$modalBody.find('#realmName').val();
                            newRealm.location = location.startsWith('/') ? location : '/' + location;
                            newRealm.active = dialog.$modalBody.find('#realmStatus')[0].selectize.getValue() === 'active';
                            newRealm.aliases = aliases !== '' ? aliases : '';
                            dialog.close();
                        }
                    }, {
                        label: $.t('common.form.cancel'),
                        action: function (dialog) {
                            dialog.close();
                        }
                    }],
                    onshow: function (dialog) {
                        dialog.$modalBody.find('#realmStatus').selectize();
                        dialog.$modalBody.find('#realmAliases').selectize({
                            plugins: ['restore_on_backspace'],
                            delimiter: ',',
                            create: true,
                            hideSelected: true
                        });
                    },
                    onshown: function (dialog) {
                        self.dialogOnShown(dialog);
                    }
                });
            });
        },
        dialogOnShown: function (dialog) {
            dialog.$modalBody.find('[data-toggle="popover-realm-status"]').popover({
                content: function () {
                    return $.t('console.realms.realmStatusPopover.content');
                },
                placement: 'left',
                title: function () {
                    return $.t('console.realms.realmStatusPopover.title');
                },
                trigger: 'focus'
            });
            dialog.$modalBody.find('[data-toggle="popover-realm-aliases"]').popover({
                content: function () {
                    return $.t('console.realms.realmAliasesPopover.content');
                },
                html: true,
                placement: 'left',
                title: function () {
                    return $.t('console.realms.realmAliasesPopover.title');
                },
                trigger: 'focus'
            });
        }
    });

    return new AddNewRealmDialog();
});
