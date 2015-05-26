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

/*global define, $, _, Backbone */

define("org/forgerock/openam/ui/policy/resourcetypes/ResourceTypesListView", [
    "backgrid",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/policy/common/AbstractListView",
    "org/forgerock/openam/ui/policy/resourcetypes/ResourceTypeModel",
    "org/forgerock/openam/ui/policy/util/BackgridUtils"
], function (Backgrid, Router, Configuration, UIUtils, URLHelper, AbstractListView, ResourceTypeModel, BackgridUtils) {

    var ResourceTypesListView = AbstractListView.extend({
        template: 'templates/policy/resourcetypes/ResourceTypesListTemplate.html',
        toolbarTemplate: 'templates/policy/resourcetypes/ResourceTypesListToolbarTemplate.html',

        render: function (args, callback) {
            var self = this,
                ResourceTypes,
                columns,
                grid,
                paginator,
                ClickableRow;

            this.data.realm = Configuration.globalData.auth.realm;
            this.data.selectedItems = [];

            ResourceTypes = Backbone.PageableCollection.extend({
                url: URLHelper.substitute("__api__/resourcetypes"),
                model: ResourceTypeModel,
                state: BackgridUtils.getState(),
                queryParams: BackgridUtils.getQueryParams(),
                parseState: BackgridUtils.parseState,
                parseRecords: BackgridUtils.parseRecords,
                sync: function (method, model, options) {
                    options.beforeSend = function (xhr) {
                        xhr.setRequestHeader('Accept-API-Version', 'protocol=1.0,resource=1.0');
                    };
                    return BackgridUtils.sync(method, model, options);
                }
            });

            ClickableRow = BackgridUtils.ClickableRow.extend({
                callback: function (e) {
                    var $target = $(e.target);

                    if ($target.is('input') || $target.is('.select-row-cell')) {
                        return;
                    }
                    Router.routeTo(Router.configuration.routes.editResourceType, {args: [this.model.id], trigger: true});
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
                    label: $.t("policy.resourceTypes.list.grid.0"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortType: "toggle",
                    editable: false
                },
                {
                    name: "description",
                    label: $.t("policy.resourceTypes.list.grid.1"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortable: false,
                    editable: false
                },
                {
                    name: "patterns",
                    label: $.t("policy.resourceTypes.list.grid.2"),
                    cell: BackgridUtils.ArrayCell,
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortable: false,
                    editable: false
                },
                {
                    name: "actions",
                    label: $.t("policy.resourceTypes.list.grid.3"),
                    cell: BackgridUtils.ObjectCell,
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortable: false,
                    editable: false
                }
            ];

            this.data.items = new ResourceTypes();

            grid = new Backgrid.Grid({
                columns: columns,
                row: ClickableRow,
                collection: self.data.items,
                emptyText: $.t("policy.resourceTypes.list.noResults")
            });

            paginator = new Backgrid.Extension.Paginator({
                collection: self.data.items,
                windowSize: 3
            });

            this.bindDefaultHandlers();

            this.parentRender(function () {
                UIUtils.fillTemplateWithData(this.toolbarTemplate, this.data, function (tpl) {
                    self.$el.find(self.toolbarTemplateID).html(tpl);
                });

                this.$el.find("#backgridContainer").append(grid.render().el);
                this.$el.find("#paginationContainer").append(paginator.render().el);

                this.data.items.fetch({reset: true}).done(function (xhr) {
                    if (callback) {
                        callback();
                    }
                });
            });
        }
    });

    return new ResourceTypesListView();
});