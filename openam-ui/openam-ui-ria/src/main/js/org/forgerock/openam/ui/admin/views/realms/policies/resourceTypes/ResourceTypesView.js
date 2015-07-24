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

define("org/forgerock/openam/ui/admin/views/realms/policies/resourceTypes/ResourceTypesView", [
    "jquery",
    "underscore",
    "backbone",
    "backgrid",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/admin/views/realms/policies/common/AbstractListView",
    "org/forgerock/openam/ui/admin/models/policies/ResourceTypeModel",
    "org/forgerock/openam/ui/common/util/BackgridUtils"
], function ($, _, Backbone, Backgrid, Router, UIUtils, URLHelper, AbstractListView, ResourceTypeModel, BackgridUtils) {
    return AbstractListView.extend({
        template: "templates/admin/views/realms/policies/resourceTypes/ResourceTypesTemplate.html",
        // Used in AbstractListView
        toolbarTemplate: "templates/admin/views/realms/policies/resourceTypes/ResourceTypesToolbarTemplate.html",

        render: function (args, callback) {
            var self = this,
                ResourceTypes,
                columns,
                grid,
                paginator,
                ClickableRow;

            this.realmPath = args[0];
            // selectedItems are used in parent class AbstractListView
            this.data.selectedItems = [];

            _.extend(this.events, {
                "click #addNewRes": "addNewResourceType"
            });

            ResourceTypes = Backbone.PageableCollection.extend({
                url: URLHelper.substitute("__api__/resourcetypes"),
                model: ResourceTypeModel,
                state: BackgridUtils.getState(),
                queryParams: BackgridUtils.getQueryParams(),
                parseState: BackgridUtils.parseState,
                parseRecords: BackgridUtils.parseRecords,
                sync: function (method, model, options) {
                    options.beforeSend = function (xhr) {
                        xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=1.0");
                    };
                    return BackgridUtils.sync(method, model, options);
                }
            });

            ClickableRow = BackgridUtils.ClickableRow.extend({
                callback: function (e) {
                    var $target = $(e.target);

                    if ($target.is("input") || $target.is(".select-row-cell")) {
                        return;
                    }

                    Router.routeTo(Router.configuration.routes.realmsResourceTypeEdit, {
                        args: [encodeURIComponent(self.realmPath), encodeURIComponent(this.model.id)],
                        trigger: true
                    });
                }
            });

            columns = [
                {
                    name: "",
                    cell: "select-row",
                    headerCell: "select-all"
                },
                {
                    name: "name",
                    label: $.t("console.policies.resourceTypes.list.grid.0"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortType: "toggle",
                    editable: false
                },
                {
                    name: "description",
                    label: $.t("console.policies.resourceTypes.list.grid.1"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortable: false,
                    editable: false
                },
                {
                    name: "patterns",
                    label: $.t("console.policies.resourceTypes.list.grid.2"),
                    cell: BackgridUtils.ArrayCell,
                    sortable: false,
                    editable: false
                },
                {
                    name: "actions",
                    label: $.t("console.policies.resourceTypes.list.grid.3"),
                    cell: BackgridUtils.ObjectCell,
                    sortable: false,
                    editable: false
                }
            ];

            this.data.items = new ResourceTypes();

            grid = new Backgrid.Grid({
                columns: columns,
                row: ClickableRow,
                collection: self.data.items,
                className: "backgrid table table-hover",
                emptyText: $.t("console.common.noResults")
            });

            paginator = new Backgrid.Extension.Paginator({
                collection: self.data.items,
                windowSize: 3
            });

            this.bindDefaultHandlers();

            this.parentRender(function () {
                this.renderToolbar();

                this.$el.find("#backgridContainer").append(grid.render().el);
                this.$el.find("#paginationContainer").append(paginator.render().el);

                this.data.items.fetch({reset: true}).done(function (xhr) {
                    if (callback) {
                        callback();
                    }
                });
            });
        },

        addNewResourceType: function (e) {
            Router.routeTo(Router.configuration.routes.realmsResourceTypeEdit, {
                args: [encodeURIComponent(this.realmPath)],
                trigger: true
            });
        }
    });
});