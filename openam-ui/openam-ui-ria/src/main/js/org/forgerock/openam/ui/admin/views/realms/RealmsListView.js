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
define("org/forgerock/openam/ui/admin/views/realms/RealmsListView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "backgrid",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "bootstrap-dialog",
    "org/forgerock/openam/ui/admin/views/realms/CreateUpdateRealmDialog",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/delegates/SMSGlobalDelegate"
], function ($, _, AbstractView, Backgrid, BackgridUtils, BootstrapDialog, CreateUpdateRealmDialog,
            Form, FormHelper, Router, SMSGlobalDelegate) {
    var RealmsView = AbstractView.extend({
        template: "templates/admin/views/realms/RealmsListTemplate.html",
        editDetailsDialogTemplate: "templates/admin/views/realms/RealmPropertiesDialogTemplate.html",
        events: {
            "click .delete-realm"        : "deleteRealm",
            "click #addRealm"            : "addRealm",
            "click .edit-realm"          : "editRealm",
            "click .toggle-realm-active" : "toggleRealmActive"
        },
        getRealmFromEvent: function (event) {
            var path = $(event.currentTarget).closest("div[data-realm-path]").data("realm-path"),
                realm = _.find(this.data.realms, { path: path });

            return realm;
        },
        addRealm: function (event) {
            event.preventDefault();
            var self = this;

            CreateUpdateRealmDialog.show({
                allRealmPaths :  this.data.allRealmPaths,
                callback : function () {
                    self.render();
                }
            });
        },
        editRealm: function (event) {
            event.preventDefault();
            var realm = this.getRealmFromEvent(event),
                self = this;

            CreateUpdateRealmDialog.show({
                allRealmPaths :  this.data.allRealmPaths,
                realmPath : realm.path,
                callback : function () {
                    self.render();
                }
            });
        },
        toggleRealmActive: function (event) {
            event.preventDefault();
            var self = this,
                realm = this.getRealmFromEvent(event);

            realm.active = !realm.active;
            SMSGlobalDelegate.realms.update(realm).done(function () {
                self.render();
            }).fail(function (e) {
                console.error(e);
                self.render();
            });
        },
        deleteRealm: function (event) {
            event.preventDefault();

            var self = this,
                realm = this.getRealmFromEvent(event),
                buttons = [{
                    label: $.t("common.form.cancel"),
                    action: function (dialog) {
                        dialog.close();
                    }
                },{
                    label: $.t("common.form.delete"),
                    cssClass: "btn-danger",
                    action: function (dialog) {
                        self.performDeleteRealm(realm.path).done(function () {
                            dialog.close();
                        });
                    }
                }];

            if (realm.active) {
                buttons.splice(1, 0, {
                    label: $.t("console.realms.warningDialog.deactivate"),
                    action: function (dialog) {
                        realm.active = false;
                        SMSGlobalDelegate.realms.update(realm).done(function () {
                            self.render();
                            dialog.close();
                        }).fail(function (e) {
                            console.error(e);
                            self.render();
                            dialog.close();
                        });
                    }
                });
            }

            BootstrapDialog.show({
                title: $.t("console.realms.warningDialog.title", { realmName: realm.name }),
                type: BootstrapDialog.TYPE_DANGER,
                message: realm.active ? $.t("console.realms.warningDialog.activeMessage") : $.t("console.realms.warningDialog.inactiveMessage"),
                buttons: buttons
            });

        },
        performDeleteRealm: function (path) {
            var self = this;

            return SMSGlobalDelegate.realms.remove(path).done(function () {
                self.render();
            });
        },
        getRealmFromList: function (path) {
            return _.find(this.data.realms, { path: path });
        },
        render: function (args, callback) {
            var self = this;

            SMSGlobalDelegate.realms.all().done(function (data) {
                var result = _.find(data.result, { name: "/" });
                if (result) {
                    result.name = $.t("console.common.topLevelRealm");
                }
                self.data.realms = data.result;
                self.data.allRealmPaths = [];

                _.each(self.data.realms, function (realm) {
                    self.data.allRealmPaths.push(realm.path);
                });

                self.parentRender(function () {
                    if (callback) {
                        callback();
                    }
                });
            }).fail(function (e) {
                console.error(e);
                // TODO: Add failure condition
            });
        }
    });

    return new RealmsView();
});
