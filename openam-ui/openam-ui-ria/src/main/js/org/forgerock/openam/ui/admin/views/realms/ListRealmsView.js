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
 * Copyright 2015-2016 ForgeRock AS.
 */

define("org/forgerock/openam/ui/admin/views/realms/ListRealmsView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/openam/ui/admin/views/realms/CreateUpdateRealmDialog",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/common/util/NavigationHelper",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/services/SMSGlobalService",
    "org/forgerock/openam/ui/common/components/TemplateBasedView",
    "org/forgerock/openam/ui/admin/views/common/ToggleCardListView"
], ($, _, AbstractView, BootstrapDialog, CreateUpdateRealmDialog, Messages, NavigationHelper, Router, SMSGlobalService,
    TemplateBasedView, ToggleCardListView) => {
    var ListRealmsView = AbstractView.extend({
        template: "templates/admin/views/realms/ListRealmsTemplate.html",
        editDetailsDialogTemplate: "templates/admin/views/realms/RealmPropertiesDialogTemplate.html",
        events: {
            "click [data-delete-realm]" : "deleteRealm",
            "click [data-add-realm]"    : "addRealm",
            "click [data-edit-realm]"   : "editRealm",
            "click [data-toogle-realm]" : "toggleRealmActive"
        },
        partials: [
            "partials/alerts/_Alert.html", // needed in CreateUpdateRealmDialog
            "partials/util/_Status.html",
            "partials/util/_ButtonLink.html",
            "templates/admin/views/realms/_RealmCard.html"
        ],
        addRealm (event) {
            event.preventDefault();
            var self = this;

            CreateUpdateRealmDialog.show({
                allRealmPaths :  this.data.allRealmPaths,
                callback () {
                    self.render();
                }
            });
        },
        deleteRealm (event) {
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
                        SMSGlobalService.realms.update(realm).then(null, function (response) {
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
        editRealm (event) {
            event.preventDefault();
            var realm = this.getRealmFromEvent(event),
                self = this;

            CreateUpdateRealmDialog.show({
                allRealmPaths :  this.data.allRealmPaths,
                realmPath : realm.path,
                callback () {
                    self.render();
                }
            });
        },
        getRealmFromEvent (event) {
            var path = $(event.currentTarget).closest("div[data-realm-path]").data("realm-path"),
                realm = _.find(this.data.realms, { path: path });

            return realm;
        },
        getRealmFromList (path) {
            return _.find(this.data.realms, { path: path });
        },
        performDeleteRealm (path) {
            var self = this;

            return SMSGlobalService.realms.remove(path).then(() => {
                return self.render();
            }, (response) => {
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
        canRealmBeDeleted (realm) {
            return realm.path === "/" ? false : true;
        },
        render (args, callback) {
            var self = this;

            SMSGlobalService.realms.all().then((data) => {
                var result = _.find(data.result, { name: "/" });

                if (result) {
                    result.name = $.t("console.common.topLevelRealm");
                }
                self.data.realms = data.result;
                self.data.allRealmPaths = [];
                NavigationHelper.populateRealmsDropdown(data);

                _.each(self.data.realms, (realm) => {
                    realm.canDelete = self.canRealmBeDeleted(realm);
                    self.data.allRealmPaths.push(realm.path);
                });

                self.parentRender(() => {

                    const tableData = {
                        "headers": [
                            $.t("console.realms.grid.header.0"), $.t("console.realms.grid.header.1"),
                            $.t("console.realms.grid.header.2"), $.t("console.realms.grid.header.3")
                        ],
                        "items" : self.data.realms
                    };

                    this.toggleView = new ToggleCardListView({
                        el: "#toggleCardList",
                        activeView: this.toggleView ? this.toggleView.getActiveView() : 0,
                        button: {
                            btnClass: "btn-primary",
                            href: "#",
                            dataAttr: "data-add-realm",
                            icon: "fa-plus",
                            title: $.t("console.realms.newRealm")
                        }
                    });

                    this.toggleView.render((toggleView) => {
                        new TemplateBasedView({
                            data: tableData,
                            el: toggleView.getElementA(),
                            template: "templates/admin/views/realms/RealmsCardsTemplate.html"
                        }).render();
                        new TemplateBasedView({
                            data: tableData,
                            el: toggleView.getElementB(),
                            template: "templates/admin/views/realms/RealmsTableTemplate.html"
                        }).render();
                    });

                    if (callback) {
                        callback();
                    }
                });
            }, (response) =>
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                })
            );
        },
        toggleRealmActive (event) {
            event.preventDefault();
            var self = this,
                realm = this.getRealmFromEvent(event);

            realm.active = !realm.active;
            SMSGlobalService.realms.update(realm).then(null, (response) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
            }).always(() => self.render());
        }
    });

    return new ListRealmsView();
});
