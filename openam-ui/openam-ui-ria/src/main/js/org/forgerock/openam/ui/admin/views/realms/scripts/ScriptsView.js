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
    "org/forgerock/commons/ui/common/backgrid/extension/ThemeableSelectAllCell",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/openam/ui/admin/models/scripts/ScriptModel",
    "org/forgerock/openam/ui/admin/services/global/ScriptsService",
    "org/forgerock/openam/ui/admin/utils/FormHelper"
], function ($, _, Backbone, BackbonePaginator, BackgridFilter, Backgrid, ThemeablePaginator, ThemeableSelectAllCell,
             Messages, AbstractView, EventManager, Router, Constants, UIUtils, URLHelper, BackgridUtils, Script,
             ScriptsService, FormHelper) {

    return AbstractView.extend({
        template: "templates/admin/views/realms/scripts/ScriptsTemplate.html",
        toolbarTemplate: "templates/admin/views/realms/scripts/ScriptsToolbarTemplate.html",
        events: {
            "click [data-add-entity]": "addNewScript",
            "click [data-delete-scripts]": "onDeleteClick"
        },

        render (args, callback) {
            var self = this,
                columns,
                grid,
                paginator,
                ClickableRow,
                Scripts,
                renderTranslatedCell;

            this.realmPath = args[0];
            this.data.selectedUUIDs = [];
            this.contextSchemaPromise = ScriptsService.scripts.getSchema();
            this.languageSchemaPromise = ScriptsService.scripts.getContextSchema();

            Scripts = Backbone.PageableCollection.extend({
                url: URLHelper.substitute("__api__/scripts"),
                model: Script,
                state: BackgridUtils.getState(),
                queryParams: BackgridUtils.getQueryParams(),
                parseState: BackgridUtils.parseState,
                parseRecords: BackgridUtils.parseRecords,
                sync: BackgridUtils.sync
            });

            renderTranslatedCell = function () {
                var id = this.model.get(this.column.get("name")),
                    translation = (this.map && self[this.map]) ? self[this.map][id] : id;

                this.$el.text(translation);
                return this;
            };

            columns = [{
                name: "",
                cell: ThemeableSelectAllCell,
                headerCell: "select-all"
            }, {
                name: "name",
                label: $.t("console.scripts.list.grid.0"),
                cell: "string",
//                headerCell: BackgridUtils.FilterHeaderCell,
                sortType: "toggle",
                editable: false
            }, {
                name: "context",
                label: $.t("console.scripts.list.grid.1"),
                cell: Backgrid.StringCell.extend({
                    map: "contextMap",
                    render: renderTranslatedCell
                }),
//                headerCell: BackgridUtils.FilterHeaderCell,
                sortType: "toggle",
                editable: false
            }, {
                name: "language",
                label: $.t("console.scripts.list.grid.2"),
                cell: Backgrid.StringCell.extend({
                    map: "langMap",
                    render: renderTranslatedCell
                }),
//                headerCell: BackgridUtils.FilterHeaderCell,
                sortType: "toggle",
                editable: false
            }, {
                name: "description",
                label: $.t("console.scripts.list.grid.3"),
                cell: "string",
                sortable: false,
                editable: false
            }];

            ClickableRow = BackgridUtils.ClickableRow.extend({
                attributes: () => ({ tabindex: 0 }),
                callback (e) {
                    var $target = $(e.target);

                    if ($target.is("input") || $target.is(".select-row-cell")) {
                        return;
                    }

                    Router.routeTo(Router.configuration.routes.realmsScriptEdit, {
                        args: [encodeURIComponent(self.realmPath), encodeURIComponent(this.model.id)],
                        trigger: true
                    });
                }
            });

            this.data.scripts = new Scripts();

            this.data.scripts.on("backgrid:selected", function (model, selected) {
                self.onRowSelect(model, selected);
            });

            this.data.scripts.on("backgrid:sort", BackgridUtils.doubleSortFix);

            grid = new Backgrid.Grid({
                columns,
                row: ClickableRow,
                collection: self.data.scripts,
                className: "backgrid table table-hover",
                emptyText: $.t("console.common.noResults")
            });

            paginator = new Backgrid.Extension.ThemeablePaginator({
                collection: self.data.scripts,
                windowSize: 3
            });

            this.parentRender(function () {
                this.renderToolbar();

                this.$el.find(".table-container").append(grid.render().el);
                this.$el.find(".panel-body").append(paginator.render().el);

                $.when(this.contextSchemaPromise, this.languageSchemaPromise).done(function (contSchema, langSchema) {
                    var languageSchema = langSchema[0] ? langSchema[0].properties.languages.items : undefined,
                        contextSchema = contSchema[0] ? contSchema[0].properties.defaultContext : undefined;
                    self.langMap = self.createMapBySchema(languageSchema);
                    self.contextMap = self.createMapBySchema(contextSchema);

                    self.data.scripts.fetch({ reset: true }).done(function () {
                        if (callback) {
                            callback();
                        }
                    });
                });
            });
        },

        onDeleteClick (e) {
            var msg = { message: $.t("console.scripts.list.confirmDeleteText") };
            e.preventDefault();
            FormHelper.showConfirmationBeforeDeleting(msg, _.bind(this.deleteRecords, this));
        },

        deleteRecords () {
            var self = this,
                i = 0,
                item,
                onDestroy = function () {
                    self.data.selectedUUIDs = [];
                    self.data.scripts.fetch({ reset: true });
                    self.renderToolbar();
                },
                onSuccess = function () {
                    onDestroy();
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                };

            for (; i < this.data.selectedUUIDs.length; i++) {
                item = this.data.scripts.get(this.data.selectedUUIDs[i]);

                item.destroy({
                    success: onSuccess,
                    wait: true
                });
            }
        },

        onRowSelect (model, selected) {
            if (selected) {
                if (!_.contains(this.data.selectedUUIDs, model.id)) {
                    this.data.selectedUUIDs.push(model.id);
                }
            } else {
                this.data.selectedUUIDs = _.without(this.data.selectedUUIDs, model.id);
            }

            this.renderToolbar();
        },

        renderToolbar () {
            var self = this;

            UIUtils.fillTemplateWithData(self.toolbarTemplate, self.data, function (tpl) {
                self.$el.find("[data-grid-toolbar]").html(tpl);
            });
        },

        addNewScript () {
            Router.routeTo(Router.configuration.routes.realmsScriptNew, {
                args: [encodeURIComponent(this.realmPath)],
                trigger: true
            });
        },

        // TODO: server side fix is needed instead of this function
        createMapBySchema (schema) {
            var map, i, length;

            if (schema && schema["enum"]) {
                map = {};
                length = schema["enum"].length;

                for (i = 0; i < length; i++) {
                    map[schema["enum"][i]] = schema.options.enum_titles[i];
                }
            }
            return map;
        }
    });
});
