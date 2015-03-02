/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define, $, _, Backgrid, Backbone*/

define("org/forgerock/openam/ui/uma/views/history/ListHistory", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/uma/util/BackgridUtils",
    "org/forgerock/openam/ui/uma/util/UmaUtils",
    "backgrid"

], function(AbstractView, conf, eventManager, uiUtils, constants, backgridUtils, umaUtils, Backgrid) {
    var HistoryView = AbstractView.extend({
        template: "templates/uma/views/history/ListHistory.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        events: {},

        render: function(args, callback) {
            var self = this,
                collection,
                grid,
                paginator,
                realm = umaUtils.getRealm();

            collection = new (Backbone.PageableCollection.extend({
                url: "/" + constants.context + "/json" + realm + "/users/" + conf.loggedUser.username + '/uma/auditHistory',
                state: {
                    pageSize: 10,
                    sortKey: "eventTime",
                    order: 1
                },
                queryParams: {
                    pageSize: "_pageSize",
                    _sortKeys: backgridUtils.sortKeys,
                    _queryFilter: backgridUtils.queryFilter,
                    _pagedResultsOffset: backgridUtils.pagedResultsOffset
                },
                parseState: backgridUtils.parseState,
                parseRecords: backgridUtils.parseRecords,
                sync: backgridUtils.sync
            }))();

            grid = new Backgrid.Grid({
                columns: [{
                    name: "requestingPartyId",
                    label: $.t("uma.history.grid.header.0"),
                    headerCell: backgridUtils.FilterHeaderCell,
                    cell: 'string',
                    editable: false,
                    sortType: "toggle"
                }, {
                    name: "resourceSetId",
                    label: $.t("uma.history.grid.header.1"),
                    headerCell: backgridUtils.FilterHeaderCell,
                    cell: backgridUtils.UriExtCell,
                    // TODO: Link this cell through to the Resources page by mapping the resourceSetId to the policy (if possible)
                    editable: false,
                    sortType: "toggle"
                }, {
                    name: "type",
                    label: $.t("uma.history.grid.header.2"),
                    cell: "string",
                    formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                        fromRaw: function(rawValue, model) {
                            return $.t("uma.history.grid.types." + rawValue.toLowerCase());
                        }
                    }),
                    editable: false,
                    sortType: "toggle"
                }, {
                    name: "eventTime",
                    label: $.t("uma.history.grid.header.3"),
                    cell: backgridUtils.DatetimeAgoCell,
                    editable: false,
                    sortType: "toggle"
                }],
                emptyText: $.t("uma.all.grid.empty"),
                collection: collection
            });

            collection.on("backgrid:sort", backgridUtils.doubleSortFix);

            paginator = new Backgrid.Extension.Paginator({
                collection: collection,
                windowSize: 3
            });

            self.parentRender(function() {
                self.$el.find("#backgridContainer").append( grid.render().el );
                self.$el.find("#paginationContainer").append( paginator.render().el );
                collection.fetch({ processData: false, reset: true });
            });
        }
    });

    return new HistoryView();
});
