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

/*global define, $, _, Backbone*/

define("org/forgerock/openam/ui/editor/views/ScriptListView", [
    "backgrid",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/editor/util/BackgridUtils",
    "org/forgerock/openam/ui/editor/models/ScriptModel"
], function (Backgrid, AbstractView, Router, UIUtils, URLHelper, BackgridUtils, Script) {

    var ScriptListView = AbstractView.extend({
        template: "templates/editor/views/ScriptListTemplate.html",

        render: function (args, callback) {
            var self = this,
                columns,
                grid,
                paginator,
                ClickableRow,
                Scripts;

            this.data.selectedUUIDs = [];

            Scripts = Backbone.PageableCollection.extend({
                url: URLHelper.substitute("__api__/scripts"),
                model: Script,
                state: BackgridUtils.getState(),
                queryParams: BackgridUtils.getQueryParams(),
                parseState: BackgridUtils.parseState,
                parseRecords: BackgridUtils.parseRecords,
                sync: BackgridUtils.sync
            });

            columns = [
                {
                    name: "",
                    cell: "select-row",
                    headerCell: "select-all"
                },
                {
                    name: "name",
                    label: $.t("scripts.list.grid.0"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortType: "toggle",
                    editable: false
                },
                {
                    name: "context",
                    label: $.t("scripts.list.grid.1"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortType: "toggle",
                    editable: false
                },
                {
                    name: "language",
                    label: $.t("scripts.list.grid.2"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortType: "toggle",
                    editable: false
                },
                {
                    name: "script",
                    label: $.t("scripts.list.grid.3"),
                    cell: "string",
                    sortable: false,
                    editable: false
                }
            ];

            ClickableRow = BackgridUtils.ClickableRow.extend({
                callback: function (e) {
                    var $target = $(e.target);

                    if ($target.is('input') || $target.is('.select-row-cell')) {
                        return;
                    }

                    Router.routeTo(Router.configuration.routes.editScript, {args: [encodeURIComponent(this.model.id)], trigger: true});
                }
            });

            this.data.scripts = new Scripts();

            this.data.scripts.on("backgrid:selected", function (model, selected) {
                self.onRowSelect(model, selected);
            });

            this.data.scripts.on("backgrid:sort", BackgridUtils.doubleSortFix);

            grid = new Backgrid.Grid({
                columns: columns,
                row: ClickableRow,
                collection: self.data.scripts,
                emptyText: $.t("scripts.grid.noResults")
            });

            paginator = new Backgrid.Extension.Paginator({
                collection: self.data.scripts,
                windowSize: 3
            });

            this.parentRender(function () {
                this.renderToolbar();

                this.$el.find("#backgridContainer").append(grid.render().el);
                this.$el.find("#paginationContainer").append(paginator.render().el);

                this.data.scripts.fetch({reset: true});

                if (callback) {
                    callback();
                }
            });
        },

        onRowSelect: function (model, selected) {
            if (selected) {
                if (!_.contains(this.data.selectedUUIDs, model.id)) {
                    this.data.selectedUUIDs.push(model.id);
                }
            } else {
                this.data.selectedUUIDs = _.without(this.data.selectedUUIDs, model.id);
            }

            this.renderToolbar();
        },

        renderToolbar: function () {
            this.$el.find('#gridToolbar').html(UIUtils.fillTemplateWithData(
                "templates/editor/views/ScriptListBtnToolbarTemplate.html", this.data));
        }
    });

    return new ScriptListView();
});
