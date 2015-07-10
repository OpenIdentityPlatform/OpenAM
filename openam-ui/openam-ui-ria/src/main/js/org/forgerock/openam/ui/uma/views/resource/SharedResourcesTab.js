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

/*global define*/

define("org/forgerock/openam/ui/uma/views/resource/SharedResourcesTab", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "backbone",
    "backgrid",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/openam/ui/uma/views/share/CommonShare",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function($, _, AbstractView, Backbone, Backgrid, BackgridUtils, CommonShare, Configuration, Constants, RealmHelper) {

    var SharedResourcesTab = AbstractView.extend({
        template: "templates/uma/views/resource/ListResourceTab.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        element: "#sharedResources",

        render: function(args, callback) {
            var self = this,
                columns,
                grid,
                paginator,
                ResourceSetCollection;

            // TODO: change endpoint
            ResourceSetCollection = Backbone.PageableCollection.extend({
                url: RealmHelper.decorateURIWithRealm("/" + Constants.context + "/json/__subrealm__/users/" + Configuration.loggedUser.username + '/oauth2/resourcesets'),
                state: {
                    pageSize: 10,
                    sortKey: "name"
                },
                queryParams: {
                    pageSize: "_pageSize",
                    _sortKeys: BackgridUtils.sortKeys,
                    _queryId: "*",
                    _queryFilter: BackgridUtils.queryFilter,
                    _pagedResultsOffset:  BackgridUtils.pagedResultsOffset,
                    _fields: ['_id', 'icon_uri', 'name', 'resourceServer', 'permission']
                },

                parseState: BackgridUtils.parseState,
                parseRecords: function(data, options){
                    self.$el.find("button#revokeAll").prop("disabled", data.result.length === 0);
                    return data.result;
                },
                sync: BackgridUtils.sync
            });

            columns = [
                {
                    name: "name",
                    label: $.t("uma.resources.list.sharedResources.grid.0"),
                    cell: BackgridUtils.UriExtCell,
                    headerCell: BackgridUtils.FilterHeaderCell.extend({
                        addClassName: "col-md-5"
                    }),
                    href: function(rawValue, formattedValue, model){
                        return "#uma/resources/" + model.get('_id');
                    },
                    editable: false
                },
                {
                    name: "resourceServer",
                    label: $.t("uma.resources.list.sharedResources.grid.1"),
                    cell: "string",
                    editable: false,
                    headerCell : BackgridUtils.ClassHeaderCell.extend({
                        className: "col-md-1"
                    })
                },
                {
                    name: "permission",
                    label: $.t("uma.resources.list.sharedResources.grid.2"),
                    cell: "string",
                    headerCell : BackgridUtils.ClassHeaderCell.extend({
                        className: "col-md-4"
                    }),
                    editable: false
                },
                {
                    name: "share",
                    label: "",
                    cell: Backgrid.Cell.extend({
                        className: "fa fa-share-alt",
                        events: { "click": "share" },
                        share: function(e) {
                            var shareView = new CommonShare();
                            shareView.renderDialog(this.model.get('_id'));
                        },
                        render: function () {
                            this.$el.attr({"title": $.t("uma.share.shareResource")});
                            this.delegateEvents();
                            return this;
                        }
                    }),
                    editable: false,
                    headerCell : BackgridUtils.ClassHeaderCell.extend({
                        className: "col-md-1"
                    })
                }
            ];

            self.data.resourceSetCollection = new ResourceSetCollection();
            self.data.resourceSetCollection.on("backgrid:sort", BackgridUtils.doubleSortFix);

            grid = new Backgrid.Grid({
                columns: columns,
                className:"backgrid table table-striped",
                collection: self.data.resourceSetCollection,
                emptyText: $.t("console.common.noResults")
            });

            paginator = new Backgrid.Extension.Paginator({
                collection: self.data.resourceSetCollection,
                windowSize: 3
            });

            self.parentRender(function() {
                self.$el.find(".backgrid-container").append(grid.render().el);
                self.$el.find(".pagination-container").append(paginator.render().el);
                self.data.resourceSetCollection.fetch({reset: true, processData: false}).done(function () {
                    if (callback) {
                        callback();
                    }
                });
            });
        }
    });

    return new SharedResourcesTab();
});
