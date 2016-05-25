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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "backbone",
    "org/forgerock/commons/ui/common/backgrid/Backgrid",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/RealmHelper",
    "org/forgerock/openam/ui/user/uma/views/backgrid/cells/PermissionsCell",
    "org/forgerock/openam/ui/user/uma/services/UMAService",
    "org/forgerock/commons/ui/common/main/Router"
], function ($, AbstractView, Backbone, Backgrid, BackgridUtils, Configuration, Constants, RealmHelper,
             PermissionsCell, UMAService, Router) {
    var EditRequest = AbstractView.extend({
        template: "templates/user/uma/views/request/EditRequestTemplate.html",
        events: {
            "click button[data-permission=allow]": "allowRequest",
            "click button[data-permission=deny]": "denyRequest"
        },

        allowRequest () {
            UMAService.approveRequest(this.model.get("_id"), this.model.get("permissions")).done(function () {
                Router.routeTo(Router.configuration.routes.umaRequestList, {
                    args: [],
                    trigger: true
                });
            });
        },

        denyRequest () {
            UMAService.denyRequest(this.model.get("_id")).done(function () {
                Router.routeTo(Router.configuration.routes.umaRequestList, {
                    args: [],
                    trigger: true
                });
            });
        },

        render (args, callback) {
            var self = this,
                id = null,
                columns,
                grid,
                RequestCollection;

            id = args[0];

            RequestCollection = Backbone.Collection.extend({
                url: RealmHelper.decorateURIWithRealm(`/${Constants.context}/json/__subrealm__/users/${
                     Configuration.loggedUser.get("username")
                    }/uma/pendingrequests/${id}`)
            });

            columns = [{
                name: "user",
                label: $.t("uma.requests.grid.header.0"),
                cell: "string",
                editable: false
            }, {
                name: "resource",
                label: $.t("uma.requests.grid.header.1"),
                cell: "string",
                editable: false
            }, {
                name: "when",
                label: $.t("uma.requests.grid.header.2"),
                cell: BackgridUtils.DatetimeAgoCell,
                editable: false
            }, {
                name: "permissions",
                label: $.t("uma.requests.grid.header.3"),
                headerCell: BackgridUtils.ClassHeaderCell.extend({
                    className: "col-xs-7 col-md-6"
                }),
                cell: PermissionsCell.extend({
                    onChange (value) {
                        this.model.set("permissions", value, { silent: true });
                        var anySelected = value !== null;
                        this.$el.parent().find("[data-permission=allow]").prop("disabled", !anySelected);
                    }
                }),
                editable: false
            }];

            this.data.requests = new RequestCollection();

            grid = new Backgrid.Grid({
                columns,
                className: "backgrid table",
                collection: this.data.requests,
                emptyText: $.t("console.common.noResults")
            });

            this.parentRender(function () {
                self.$el.find(".table-container").append(grid.render().el);
                self.data.requests.fetch({ reset: true, processData: false }).done(function () {
                    self.model = self.data.requests.findWhere({ _id: id });
                    if (callback) { callback(); }
                }).fail(function () {
                    self.$el.find("button[data-permission]").prop("disabled", true);
                });
            });
        }
    });

    return new EditRequest();
});
