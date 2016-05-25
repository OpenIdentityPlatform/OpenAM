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
    "backbone",
    "backbone.paginator",
    "backgrid-filter",
    "org/forgerock/commons/ui/common/backgrid/Backgrid",
    "org/forgerock/commons/ui/common/backgrid/extension/ThemeablePaginator",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/RealmHelper",
    "org/forgerock/openam/ui/user/uma/views/backgrid/cells/PermissionsCell",
    "org/forgerock/openam/ui/user/uma/services/UMAService"
], function ($, Backbone, BackbonePaginator, BackgridFilter, Backgrid, ThemeablePaginator,
             AbstractView, BackgridUtils, Configuration, Constants, RealmHelper, PermissionsCell, UMAService) {
    var ListRequest = AbstractView.extend({
        template: "templates/user/uma/views/request/ListRequestTemplate.html",

        render (args, callback) {
            var self = this,
                columns,
                grid,
                paginator,
                RequestsCollection;

            RequestsCollection = Backbone.PageableCollection.extend({
                url: RealmHelper.decorateURIWithRealm(`/${Constants.context}/json/__subrealm__/users/${
                     Configuration.loggedUser.get("username")
                    }/uma/pendingrequests`),
                state: {
                    pageSize: 10,
                    sortKey: "user"
                },
                queryParams: {
                    pageSize: "_pageSize",
                    _sortKeys: BackgridUtils.sortKeys,
                    _queryId: "*",
                    _queryFilter: "true",
                    _pagedResultsOffset: BackgridUtils.pagedResultsOffset
                },
                parseState: BackgridUtils.parseState,
                parseRecords: BackgridUtils.parseRecords,
                sync: BackgridUtils.sync
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

                        // TODO: Code that works with Backbone MultiSelect
                        // var anySelected = this.$el.find("li.active").length > 0;
                        // this.$el.parent().find("[data-permission=allow]").prop("disabled", !anySelected);
                    }
                }),
                editable: false
            }, {
                name: "actions",
                label: "",
                cell: BackgridUtils.TemplateCell.extend({
                    template: "templates/user/uma/backgrid/cell/ActionsCell.html",
                    events: {
                        "click button[data-permission=allow]": "allow",
                        "click button[data-permission=deny]": "deny"
                    },
                    allow () {
                        UMAService.approveRequest(this.model.get("_id"), this.model.get("permissions"))
                        .done(function () {
                            self.data.requests.fetch({ reset: true, processData: false }); // TODO: DRY
                        });
                    },
                    deny () {
                        UMAService.denyRequest(this.model.get("_id")).done(function () {
                            self.data.requests.fetch({ reset: true, processData: false }); // TODO: DRY
                        });
                    }
                }),
                editable: false
            }];

            this.data.requests = new RequestsCollection();
            this.data.requests.on("backgrid:sort", BackgridUtils.doubleSortFix);

            grid = new Backgrid.Grid({
                columns,
                className: "backgrid table",
                collection: self.data.requests,
                emptyText: $.t("console.common.noResults")
            });

            paginator = new Backgrid.Extension.ThemeablePaginator({
                collection: self.data.requests,
                windowSize: 3
            });

            self.parentRender(function () {
                self.$el.find(".table-container").append(grid.render().el);
                self.$el.find(".panel-body").append(paginator.render().el);

                self.data.requests.fetch({ reset: true, processData: false }).done(function () {
                    if (callback) { callback(); }
                });
            });
        }
    });

    return new ListRequest();
});
