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
define('org/forgerock/openam/ui/admin/views/realms/RealmsListView', [
    'jquery',
    'underscore',
    'org/forgerock/commons/ui/common/main/AbstractView',
    'org/forgerock/openam/ui/admin/views/realms/AddNewRealmDialog',
    'backgrid',
    'org/forgerock/openam/ui/common/util/BackgridUtils',
    'bootstrap-dialog',
    'org/forgerock/openam/ui/admin/models/Form',
    'org/forgerock/openam/ui/admin/utils/FormHelper',
    'org/forgerock/commons/ui/common/main/Router',
    'org/forgerock/openam/ui/admin/delegates/SMSGlobalDelegate'
], function ($, _, AbstractView, AddNewRealmDialog, Backgrid, BackgridUtils, BootstrapDialog, Form, FormHelper, Router, SMSGlobalDelegate) {
    var RealmsView = AbstractView.extend({
        template: 'templates/admin/views/realms/RealmsListTemplate.html',
        editDetailsDialogTemplate: 'templates/admin/views/realms/RealmPropertiesDialogTemplate.html',
        events: {
            'click .delete-realm'        : 'deleteRealm',
            'click #addRealm'            : 'addRealm',
            'click .edit-realm'          : 'editRealm',
            'click .toggle-realm-active' : 'toggleRealmActive'
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
        },
        getRealmFromEvent: function (event) {
            var location = $(event.currentTarget).closest('div[data-realm-location]').data('realm-location'),
                realm = _.findWhere(this.data.realms, { location: location });

            return realm;
        },
        addRealm: function (event) {
            event.preventDefault();
            AddNewRealmDialog.show();
        },
        editRealm: function (event) {
            event.preventDefault();

            var realm = this.getRealmFromEvent(event);

            SMSGlobalDelegate.realms.get(realm.location).done(function (data) {
                BootstrapDialog.show({
                    title: realm.name + ' - ' + $.t('console.realms.realmPropertiesDialog.dialogTitle'),
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
                            var promise = SMSGlobalDelegate.realms.save(dialog.form.data());
                            promise.done(function () {
                                dialog.close();
                            });

                            FormHelper.bindSavePromiseToElement(promise, this);
                        }
                    }, {
                        label: $.t('common.form.cancel'),
                        action: function (dialog) {
                            dialog.close();
                        }
                    }]
                });
            });
        },
        toggleRealmActive: function (event) {
            event.preventDefault();

            var realm = this.getRealmFromEvent(event);
            realm.active = !realm.active;

            this.saveRealm(realm);
        },
        deleteRealm: function (event) {
            event.preventDefault();

            var self = this,
                realm = this.getRealmFromEvent(event);

            if (realm.active) {
                BootstrapDialog.show({
                    title: $.t('console.realms.warningDialog.title', { realmName: realm.name }),
                    type: BootstrapDialog.TYPE_DANGER,
                    message: $.t('console.realms.warningDialog.message'),
                    buttons: [{
                        label: $.t('common.form.delete'),
                        cssClass: 'btn-danger',
                        action: function (dialog) {
                            self.performDeleteRealm(realm.location).done(function () {
                                dialog.close();
                            });
                        }
                    }, {
                        label: $.t('common.form.cancel'),
                        action: function (dialog) {
                            dialog.close();
                        }
                    }]
                });
            } else {
                self.performDeleteRealm(realm.location);
            }
        },
        performDeleteRealm: function (location) {
            var self = this;

            return SMSGlobalDelegate.realms.remove(location)
            .done(function () {
                self.render();
            });
        },
        saveRealm: function (realm) {
            var self = this;

            SMSGlobalDelegate.realms.save(realm)
            .done(function () {
                self.render();
            });
        },
        getRealmFromList: function (location) {
            return _.findWhere(this.data.realms, { location: location });
        },
        render: function (args, callback) {
            var self = this;

            SMSGlobalDelegate.realms.all()
            .done(function (data) {
                var result = _.findWhere(data.result, { name: '/' });
                if (result) {
                    result.name = $.t('console.realms.topLevelRealm');
                }
                self.data.realms = data.result;

                self.parentRender(function () {
                    if (callback) {
                        callback();
                    }
                });
            })
            .fail(function () {
                // TODO: Add failure condition
            });
        }
    });

    return new RealmsView();
});
