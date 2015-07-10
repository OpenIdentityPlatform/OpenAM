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

/*global define*/
define("org/forgerock/openam/ui/uma/views/RequestsView", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "backbone",
    "backgrid",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/RealmHelper",
    "org/forgerock/openam/ui/uma/views/backgrid/cells/PermissionsCell"
], function ($, AbstractView, Backbone, Backgrid, BackgridUtils, Configuration, Constants, RealmHelper, PermissionsCell) {
    var RequestsView = AbstractView.extend({
        template: "templates/uma/views/RequestsTemplate.html",

        render: function (args, callback) {
            var self = this,
                columns,
                grid,
                paginator,
                RequestsCollection;

            RequestsCollection = Backbone.PageableCollection.extend({
                // FIXME: Update when server implemenation is complete
                url: RealmHelper.decorateURIWithRealm("/" + Constants.context + "/json/__subrealm__/users/" + Configuration.loggedUser.username + "/oauth2/resourcesets"),
                state: {
                    pageSize: 10,
                    sortKey: "user"
                },
                queryParams: {
                    pageSize: "_pageSize",
                    _sortKeys: BackgridUtils.sortKeys,
                    _queryId: "*",
                    _queryFilter: BackgridUtils.queryFilter,
                    _pagedResultsOffset: BackgridUtils.pagedResultsOffset,
                    _fields: ["_id", "icon_uri", "name", "resourceServer", "type"]
                },
                parseState: BackgridUtils.parseState,
                sync: BackgridUtils.sync
            });

            columns = [{
                name: "user",
                label: $.t("uma.requests.grid.header.0"),
                headerCell: BackgridUtils.FilterHeaderCell.extend({
                    addClassName: "col-md-2"
                }),
                cell: "string",
                editable: false
            }, {
                name: "resource",
                label: $.t("uma.requests.grid.header.1"),
                headerCell: BackgridUtils.ClassHeaderCell.extend({
                    className: "col-md-2"
                }),
                cell: "string",
                editable: false
            }, {
                name: "when",
                label: $.t("uma.requests.grid.header.2"),
                headerCell: BackgridUtils.ClassHeaderCell.extend({
                    className: "col-md-2"
                }),
                cell: BackgridUtils.DatetimeAgoCell,
                editable: false
            }, {
                name: "permissions",
                label: $.t("uma.requests.grid.header.3"),
                headerCell: BackgridUtils.ClassHeaderCell.extend({
                    className: "col-md-3"
                }),
                cell: PermissionsCell.extend({
                    onChange: function () {
                        var anySelected = this.$el.find("li.active").length > 0;
                        this.$el.parent().find(".permissionAllow").prop("disabled", !anySelected);
                    }
                }),
                editable: false
            }, {
                name: "actions",
                label: "",
                headerCell: BackgridUtils.ClassHeaderCell.extend({
                    className: "col-md-1"
                }),
                cell: BackgridUtils.TemplateCell.extend({
                    template: "templates/uma/backgrid/cell/ActionsCell.html",
                    events: {
                        "click #allow": "allow",
                        "click #deny": "deny"
                    },
                    allow: function () {
                        // TODO:
                    },
                    deny: function () {
                        // TODO:
                    }
                }),
                editable: false
            }];

            this.data.requests = new RequestsCollection();
            this.data.requests.on("backgrid:sort", BackgridUtils.doubleSortFix);

            // FIXME: Remove when server implemenation is complete
            this.data.requests.add({
                user: "Bob",
                resource: "Photo1",
                when: "",
                permissions: ["View", "Delete", "Read", "Update", "Execute"]
            });

            grid = new Backgrid.Grid({
                columns: columns,
                className: "backgrid table table-striped",
                collection: self.data.requests,
                emptyText: $.t("uma.all.grid.empty")
            });

            paginator = new Backgrid.Extension.Paginator({
                collection: self.data.requests,
                windowSize: 3
            });

            self.parentRender(function() {
                self.$el.find("#backgridContainer").append( grid.render().el );
                self.$el.find("#paginationContainer").append( paginator.render().el );
                // FIXME: Comment back in when server implemenation is complete
                // self.data.requests.fetch({reset: true, processData: false});
                if (callback) { callback(); }
            });
        }
    });

    return new RequestsView();
});
