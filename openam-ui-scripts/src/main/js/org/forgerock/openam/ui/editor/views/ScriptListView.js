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

define("org/forgerock/openam/ui/editor/views/ScriptListView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "backgrid",
    "org/forgerock/openam/ui/editor/util/BackgridUtils"
], function (AbstractView, conf, eventManager, uiUtils, constants, router, Backgrid, BackgridUtils) {

    var ScriptListView = AbstractView.extend({
        template: "templates/editor/views/ScriptListTemplate.html",

        render: function (args, callback) {
            var self = this,
                columns,
                grid,
                paginator,
                Scripts,
                realm = conf.globalData.auth.realm !== "/" ? conf.globalData.auth.realm : "";

            this.data.selectedUUIDs = [];

            Scripts = Backbone.PageableCollection.extend({
                url: "/" + constants.context + "/json" + realm + "/scripts",
                queryParams: {
                    _queryFilter: BackgridUtils.queryFilter,
                    pageSize: null,  // todo implement pagination
                    _pagedResultsOffset: null //todo implement pagination
                },

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
                    cell: BackgridUtils.UriExtCell,
                    headerCell: BackgridUtils.FilterHeaderCell,
                    href: function (rawValue, formattedValue, model) {
                        return "#edit/" + model.get('uuid');
                    },
                    editable: false
                },
                {
                    name: "context",
                    label: $.t("scripts.list.grid.1"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    editable: false
                },
                {
                    name: "language",
                    label: $.t("scripts.list.grid.2"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    editable: false
                },
                {
                    name: "script",
                    label: $.t("scripts.list.grid.3"),
                    cell: "string",
                    editable: false
                }
            ];

            self.data.scripts = new Scripts();

            self.data.scripts.on("backgrid:selected", function (model, selected) {
                self.onRowSelect(model, selected);
            });

            grid = new Backgrid.Grid({
                columns: columns,
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
                this.data.selectedUUIDs.push(model.attributes.uuid);
            } else {
                this.data.selectedUUIDs = _.without(this.data.selectedUUIDs, model.attributes.uuid);
            }

            this.renderToolbar();
        },

        renderToolbar: function () {
            this.$el.find('#gridToolbar').html(uiUtils.fillTemplateWithData("templates/editor/views/ScriptListBtnToolbarTemplate.html", this.data));
        }
    });

    return new ScriptListView();
});
