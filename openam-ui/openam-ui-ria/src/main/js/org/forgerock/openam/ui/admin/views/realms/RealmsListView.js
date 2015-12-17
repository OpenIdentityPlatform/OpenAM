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

define("org/forgerock/openam/ui/admin/views/realms/RealmsListView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/backgrid/Backgrid",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/openam/ui/admin/views/realms/CreateUpdateRealmDialog",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/delegates/SMSGlobalDelegate"
], function ($, _, AbstractView, Backgrid, BackgridUtils, BootstrapDialog, CreateUpdateRealmDialog, Form, FormHelper,
             Messages, Router, SMSGlobalDelegate) {
    var RealmsView = AbstractView.extend({
        template: "templates/admin/views/realms/RealmsListTemplate.html",
        editDetailsDialogTemplate: "templates/admin/views/realms/RealmPropertiesDialogTemplate.html",
        events: {
            "click .delete-realm"        : "deleteRealm",
            "click #addRealm"            : "addRealm",
            "click .edit-realm"          : "editRealm",
            "click .toggle-realm-active" : "toggleRealmActive"
        },
        partials: [
            "partials/alerts/_Alert.html" // needed in CreateUpdateRealmDialog
        ],
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
        deleteRealm: function (event) {
            event.preventDefault();
            var self = this,
                realm = this.getRealmFromEvent(event),
                buttons = [{
                    label: $.t("common.form.cancel"),
                    action: function (dialog) {
                        dialog.close();
                    }
                }, {
                    label: $.t("common.form.delete"),
                    cssClass: "btn-danger",
                    action: function (dialog) {
                        self.performDeleteRealm(realm.path).always(function () {
                            dialog.close();
                        });
                    }
                }];

            if (!realm.canDelete) {
                return false;
            }

            if (realm.active) {
                buttons.splice(1, 0, {
                    label: $.t("common.form.deactivate"),
                    action: function (dialog) {
                        realm.active = false;
                        SMSGlobalDelegate.realms.update(realm).then(null, function (response) {
                            Messages.addMessage({
                                type: Messages.TYPE_DANGER,
                                response: response
                            });
                        }).always(function () {
                            self.render();
                            dialog.close();
                        });
                    }
                });
            }

            BootstrapDialog.show({
                title: $.t("console.realms.warningDialog.title", { realmName: realm.name }),
                type: BootstrapDialog.TYPE_DANGER,
                message: realm.active ? $.t("console.realms.warningDialog.activateMessage")
                    : $.t("console.realms.warningDialog.deactivateMessage"),
                buttons: buttons
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
        getActiveTabIndex: function () {
            var tabIndex = this.$el.find(".tab-pane.active").index();
            return tabIndex > 0 ? tabIndex : 0;
        },
        getRealmFromEvent: function (event) {
            var path = $(event.currentTarget).closest("div[data-realm-path]").data("realm-path"),
                realm = _.find(this.data.realms, { path: path });

            return realm;
        },
        getRealmFromList: function (path) {
            return _.find(this.data.realms, { path: path });
        },
        performDeleteRealm: function (path) {
            var self = this;

            return SMSGlobalDelegate.realms.remove(path).then(function () {
                return self.render();
            }, function (response) {
                if (response && response.status === 409) {
                    Messages.addMessage({
                        message: $.t("console.realms.parentRealmCannotDeleted"),
                        type: Messages.TYPE_DANGER
                    });
                } else {
                    Messages.addMessage({
                        response: response,
                        type: Messages.TYPE_DANGER
                    });
                }
            });
        },
        canRealmBeDeleted: function (realm) {
            return realm.path === "/" ? false : true;
        },
        render: function (args, callback) {
            var self = this;

            SMSGlobalDelegate.realms.all().then(function (data) {
                var result = _.find(data.result, { name: "/" }),
                    activeTabIndex = self.getActiveTabIndex();

                if (result) {
                    result.name = $.t("console.common.topLevelRealm");
                }
                self.data.realms = data.result;
                self.data.allRealmPaths = [];

                _.each(self.data.realms, function (realm) {
                    realm.canDelete = self.canRealmBeDeleted(realm);
                    self.data.allRealmPaths.push(realm.path);
                });

                self.parentRender(function () {
                    this.$el.find(".tab-pane").eq(activeTabIndex).addClass("active");
                    this.$el.find(".tab-toggles").eq(activeTabIndex).addClass("active");

                    if (callback) {
                        callback();
                    }
                });
            }, function (response) {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response: response
                });
            });
        },
        toggleRealmActive: function (event) {
            event.preventDefault();
            var self = this,
                realm = this.getRealmFromEvent(event);

            realm.active = !realm.active;
            SMSGlobalDelegate.realms.update(realm).then(null, function (response) {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response: response
                });
            }).always(function () {
                self.render();
            });
        }
    });

    return new RealmsView();
});
