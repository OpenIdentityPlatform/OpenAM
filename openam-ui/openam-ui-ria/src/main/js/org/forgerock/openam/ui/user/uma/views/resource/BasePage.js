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
    "backbone.paginator",
    "org/forgerock/commons/ui/common/backgrid/Backgrid",
    "backgrid-filter",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/openam/ui/user/uma/views/share/CommonShare",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/RealmHelper",
    "org/forgerock/commons/ui/common/backgrid/extension/ThemeablePaginator"
], function ($, AbstractView, Backbone, BackbonePaginator, Backgrid, BackgridFilter,
            BackgridUtils, CommonShare, Configuration, Constants, RealmHelper) {
    var BasePage = AbstractView.extend({
        createCollection (url, queryFilters) {
            var self = this;

            return Backbone.PageableCollection.extend({
                url,
                queryParams: BackgridUtils.getQueryParams({
                    _sortKeys: BackgridUtils.sortKeys,
                    _queryFilter: queryFilters,
                    _pagedResultsOffset: BackgridUtils.pagedResultsOffset
                }),
                state: BackgridUtils.getState(),
                parseState: BackgridUtils.parseState,
                parseRecords (data) {
                    if (data.result.length) {
                        self.recordsPresent();
                    }

                    return data.result;
                },
                sync: BackgridUtils.sync
            });
        },
        createLabelCollection (labelId) {
            var filters = [`resourceOwnerId eq \"${Configuration.loggedUser.get("username")}\"`];

            if (labelId) {
                filters.push(`labels eq \"${labelId}\"`);
            }

            return this.createCollection(
                RealmHelper.decorateURIWithRealm(`/${Constants.context}/json/__subrealm__/users/${
                    Configuration.loggedUser.get("username")
                    }/oauth2/resources/sets`),
                filters
            );
        },
        createSetCollection (notResourceOwner) {
            var filters = [`resourceOwnerId eq \"${Configuration.loggedUser.get("username")}\"`];

            if (notResourceOwner) {
                filters[0] = `! ${filters[0]}`;
            }

            return this.createCollection(
                RealmHelper.decorateURIWithRealm(`/${Constants.context}/json/__subrealm__/users/${
                    Configuration.loggedUser.get("username")
                    }/oauth2/resources/sets`),
                filters
            );
        },
        createColumns (pathToResource) {
            return [{
                name: "name",
                label: $.t("uma.resources.grid.header.0"),
                cell: BackgridUtils.UriExtCell,
                headerCell: BackgridUtils.FilterHeaderCell,
                href (rawValue, formattedValue, model) {
                    return `#uma/resources/${pathToResource}/${model.get("_id")}`;
                },
                editable: false
            }, {
                name: "resourceServer",
                label: $.t("uma.resources.grid.header.1"),
                cell: "string",
                editable: false,
                headerCell: BackgridUtils.ClassHeaderCell
            }, {
                name: "type",
                label: $.t("uma.resources.grid.header.2"),
                cell: "string",
                headerCell: BackgridUtils.ClassHeaderCell,
                editable: false
            }, {
                name: "share",
                label: $.t("uma.resources.grid.header.3"),
                cell: Backgrid.Cell.extend({
                    className: "fr-col-btn-1 fa fa-share",
                    events: { "click": "share" },
                    share () {
                        var shareView = new CommonShare();
                        shareView.renderDialog({
                            _id: this.model.get("_id"),
                            toBeCreated: this.model.toBeCreated,
                            share: () => {
                                this.model.toBeCreated = false;
                            }
                        });
                    },
                    render () {
                        this.$el.attr({ "title": $.t("uma.share.shareResource") });
                        this.delegateEvents();
                        return this;
                    }
                }),
                editable: false,
                sortable: false,
                headerCell: BackgridUtils.ClassHeaderCell
            }];
        },
        recordsPresent () {
            // Override in child
        },
        renderGrid (Collection, columns, callback) {
            var self = this, grid, paginator;

            this.data.collection = new Collection();
            this.data.collection.on("backgrid:sort", BackgridUtils.doubleSortFix);

            grid = new Backgrid.Grid({
                columns,
                className: "backgrid table",
                collection: this.data.collection,
                emptyText: $.t("console.common.noResults")
            });

            paginator = new Backgrid.Extension.ThemeablePaginator({
                collection: this.data.collection,
                windowSize: 3
            });

            this.parentRender(function () {
                self.$el.find(".table-container").append(grid.render().el);
                self.$el.find(".panel-body").append(paginator.render().el);

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
