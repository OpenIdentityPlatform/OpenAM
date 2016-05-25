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
    "lodash",
    "backbone",
    "backbone.paginator",
    "backgrid-filter",
    "org/forgerock/commons/ui/common/backgrid/Backgrid",
    "org/forgerock/commons/ui/common/backgrid/extension/ThemeablePaginator",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function ($, _, Backbone, BackbonePaginator, BackgridFilter, Backgrid, ThemeablePaginator, AbstractView,
             Configuration, Constants, BackgridUtils, RealmHelper) {
    var HistoryView = AbstractView.extend({
        template: "templates/user/uma/views/history/ListHistory.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        events: {},

        render () {
            var self = this,
                collection,
                grid,
                paginator;

            collection = new (Backbone.PageableCollection.extend({
                url: RealmHelper.decorateURIWithRealm(`/${Constants.context}/json/__subrealm__/users/${
                    Configuration.loggedUser.get("username")
                    }/uma/auditHistory`),
                state: {
                    pageSize: 10,
                    sortKey: "eventTime",
                    order: 1
                },
                queryParams: {
                    pageSize: "_pageSize",
                    _sortKeys: BackgridUtils.sortKeys,
                    _queryFilter: BackgridUtils.queryFilter,
                    _pagedResultsOffset: BackgridUtils.pagedResultsOffset
                },
                parseState: BackgridUtils.parseState,
                parseRecords: BackgridUtils.parseRecords,
                sync: BackgridUtils.sync
            }))();

            grid = new Backgrid.Grid({
                columns: [{
                    name: "requestingPartyId",
                    label: $.t("uma.history.grid.header.0"),
                    headerCell: BackgridUtils.FilterHeaderCell,
                    cell: "string",
                    editable: false,
                    sortType: "toggle"
                }, {
                    name: "resourceSetName",
                    label: $.t("uma.history.grid.header.1"),
                    headerCell: BackgridUtils.FilterHeaderCell,
                    cell: BackgridUtils.UriExtCell,
                    href (rawValue, formattedValue, model) {
                        return `#uma/resources/myresources/all/${encodeURIComponent(model.get("resourceSetId"))}`;
                    },
                    editable: false,
                    sortType: "toggle"
                }, {
                    name: "type",
                    label: $.t("uma.history.grid.header.2"),
                    cell: "string",
                    formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                        fromRaw (rawValue) {
                            return $.t(`uma.history.grid.types.${rawValue.toLowerCase()}`);
                        }
                    }),
                    editable: false,
                    sortType: "toggle"
                }, {
                    name: "eventTime",
                    label: $.t("uma.history.grid.header.3"),
                    cell: BackgridUtils.DatetimeAgoCell,
                    editable: false,
                    sortType: "toggle"
                }],
                emptyText: $.t("console.common.noResults"),
                className:"backgrid table",
                collection
            });

            collection.on("backgrid:sort", BackgridUtils.doubleSortFix);

            paginator = new Backgrid.Extension.ThemeablePaginator({
                collection,
                windowSize: 3
            });

            self.parentRender(function () {
                self.$el.find(".table-container").append(grid.render().el);
                self.$el.find(".panel-body").append(paginator.render().el);
                collection.fetch({ processData: false, reset: true });
            });
        }
    });

    return new HistoryView();
});
