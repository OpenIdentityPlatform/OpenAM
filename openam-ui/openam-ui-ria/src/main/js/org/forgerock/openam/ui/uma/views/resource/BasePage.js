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
define("org/forgerock/openam/ui/uma/views/resource/BasePage", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "backbone",
    "backgrid",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/openam/ui/uma/views/share/CommonShare",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function($, AbstractView, Backbone, Backgrid, BackgridUtils, CommonShare, Configuration, Constants, RealmHelper) {
    var BasePage = AbstractView.extend({
        createCollection: function(url, filters) {
            var self = this,
                queryFilter = [ "resourceOwnerId eq \"" + Configuration.loggedUser.username + "\"" ];

            if(filters && filters.length) {
                queryFilter = queryFilter.concat(filters);
            }

            return Backbone.PageableCollection.extend({
                url: url,
                queryParams: BackgridUtils.getQueryParams({
                    // _sortKeys: BackgridUtils.sortKeys, // TODO: Enable when end point supports
                    _queryFilter: queryFilter//,
                    // _pagedResultsOffset: BackgridUtils.pagedResultsOffset // TODO: Enable when end point supports
                }),
                state: BackgridUtils.getState(),
                parseState: BackgridUtils.parseState,
                parseRecords: function(data) {
                    if(data.result.length) {
                        self.recordsPresent();
                    }

                    return data.result;
                },
                sync: BackgridUtils.sync
            });
        },
        createLabelCollection: function(labelId) {
            var filters = [];

            if(labelId) {
                filters.push("labels eq \"" + labelId + "\"");
            }

            return this.createCollection(RealmHelper.decorateURIWithRealm("/" + Constants.context +
                                                                   "/json/__subrealm__/users/" +
                                                                   Configuration.loggedUser.username +
                                                                   "/oauth2/resources/labels"), filters);
        },
        createSetCollection: function() {
            return this.createCollection(RealmHelper.decorateURIWithRealm("/" + Constants.context +
                                                                   "/json/__subrealm__/users/" +
                                                                   Configuration.loggedUser.username +
                                                                   "/oauth2/resources/sets"));
        },
        createColumns: function(section, labelName) {
            return [{
                name: "name",
                label: $.t("uma.resources.grid.header.0"),
                cell: BackgridUtils.UriExtCell,
                headerCell: BackgridUtils.FilterHeaderCell.extend({
                    addClassName: "col-md-5"
                }),
                href: function(rawValue, formattedValue, model){
                    return "#uma/resources/" + section + "/" + labelName + "/" + model.get("_id");
                },
                editable: false
            }, {
                name: "resourceServer",
                label: $.t("uma.resources.grid.header.1"),
                cell: "string",
                editable: false,
                headerCell: BackgridUtils.ClassHeaderCell.extend({
                    className: "col-md-1"
                })
            }, {
                name: "type",
                label: $.t("uma.resources.grid.header.2"),
                cell: "string",
                headerCell: BackgridUtils.ClassHeaderCell.extend({
                    className: "col-md-4"
                }),
                editable: false
            }, {
                name: "share",
                label: $.t("uma.resources.grid.header.3"),
                cell: Backgrid.Cell.extend({
                    className: "fa fa-share",
                    events: { "click": "share" },
                    share: function() {
                        var shareView = new CommonShare();
                        shareView.renderDialog(this.model.get("_id"));
                    },
                    render: function () {
                        this.$el.attr({"title": $.t("uma.share.shareResource")});
                        this.delegateEvents();
                        return this;
                    }
                }),
                editable: false,
                headerCell: BackgridUtils.ClassHeaderCell.extend({
                    className: "col-md-1"
                })
            }];
        },
        recordsPresent: function() {
            //
        },
        renderGrid: function(Collection, columns, callback) {
            var self = this, grid, paginator;

            this.data.collection = new Collection();
            this.data.collection.on("backgrid:sort", BackgridUtils.doubleSortFix);

            grid = new Backgrid.Grid({
                columns: columns,
                className: "backgrid table table-striped",
                collection: this.data.collection,
                emptyText: $.t("console.common.noResults")
            });

            paginator = new Backgrid.Extension.Paginator({
                collection: this.data.collection,
                windowSize: 3
            });

            this.parentRender(function() {
                self.$el.find(".backgrid-container").append(grid.render().el);
                self.$el.find(".pagination-container").append(paginator.render().el);

                self.data.collection.fetch({ reset: true, processData: false }).done(function () {
                    if (callback) {
                        callback();
                    }
                });
            });
        }
    });

    return BasePage;
});
