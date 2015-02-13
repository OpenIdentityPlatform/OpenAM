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

define("org/forgerock/openam/ui/uma/HistoryView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/uma/util/BackgridUtils"
], function(AbstractView, conf, eventManager, uiUtils, constants, backgridUtils) {

    var HistoryView = AbstractView.extend({
        template: "templates/uma/HistoryTemplate.html",
        baseTemplate: 'templates/policy/BaseTemplate.html',
        events: {},

        render: function(args, callback) {

            var self = this,
                historyCollection,
                columns,
                grid,
                paginator,
                HistoryCollection;

            HistoryCollection = Backbone.PageableCollection.extend({
                // a link is needed
                url: "/",
                state: {
                    pageSize: 10,
                    sortKey: "lastModifiedDate",
                    order: 1,
                    _pagedResultsOffset : 0
                },
                queryParams: {
                    pageSize: "_pageSize",
                    sortKey: "_sortKeys",
                    _queryFilter: backgridUtils.queryFilter,
                    _pagedResultsOffset: backgridUtils.pagedResultsOffset
                },

                parseState: backgridUtils.parseState,
                parseRecords: backgridUtils.parseRecords,
                sync: backgridUtils.sync
            });

            historyCollection = new HistoryCollection();

            columns = [
                {
                    name: "username_who_accessed",
                    label: $.t("policy.uma.history.grid.0"),
                    cell: backgridUtils.UriExtCell,
                    href: function(rawValue, formattedValue, model){
                        return "#uma/users/" + formattedValue + "/activity/";
                    },
                    editable: false
                },
                {
                    name: "resource_name",
                    label: $.t("policy.uma.history.grid.1"),
                    cell: 'string',
                    editable: false
                },
                {
                    name: "appname",
                    label: $.t("policy.uma.history.grid.2"),
                    cell: 'uri',
                    href: function(rawValue, formattedValue, model){
                        return "#uma/apps/" + formattedValue + "/activity/";
                    },
                    editable: false
                },
                {
                    name: "lastModifiedDate",
                    label: $.t("policy.uma.history.grid.3"),
                    cell: backgridUtils.DatetimeAgoCell,
                    editable: false
                }

            ];

            grid = new Backgrid.Grid({
                columns: columns,
                emptyText: $.t("policy.uma.all.grid.empty"),
                collection: historyCollection
            });

            paginator = new Backgrid.Extension.Paginator({
                collection: historyCollection,
                windowSize: 3
            });

            self.parentRender(function() {
                self.$el.find("#backgridContainer").append( grid.render().el );
                self.$el.find("#paginationConatiner").append( paginator.render().el );
                historyCollection.fetch({reset: true, processData: false});
            });
        }

    });


    return new HistoryView();
});
