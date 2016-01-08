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


define("org/forgerock/openam/ui/admin/views/realms/authorization/resourceTypes/ResourceTypesView", [
    "jquery",
    "lodash",
    "backbone",
    "backbone.paginator",
    "backgrid-filter",
    "org/forgerock/commons/ui/common/backgrid/Backgrid",
    "org/forgerock/commons/ui/common/backgrid/extension/ThemeablePaginator",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/admin/views/realms/authorization/common/AbstractListView",
    "org/forgerock/openam/ui/admin/models/authorization/ResourceTypeModel",
    "org/forgerock/openam/ui/common/util/BackgridUtils"
], function ($, _, Backbone, BackbonePaginator, BackgridFilter, Backgrid, ThemeablePaginator, Messages, EventManager,
             Router, Constants, UIUtils, URLHelper, AbstractListView, ResourceTypeModel, BackgridUtils) {
    return AbstractListView.extend({
        template: "templates/admin/views/realms/authorization/resourceTypes/ResourceTypesTemplate.html",
        // Used in AbstractListView
        toolbarTemplate: "templates/admin/views/realms/authorization/resourceTypes/ResourceTypesToolbarTemplate.html",
        partials: [
            "partials/util/_HelpLink.html"
        ],
        events: {
            "click #addNewRes": "addNewResourceType"
        },
        render: function (args, callback) {
            var self = this,
                ResourceTypes,
                columns,
                grid,
                paginator,
                ClickableRow;

            this.realmPath = args[0];

            ResourceTypes = Backbone.PageableCollection.extend({
                url: URLHelper.substitute("__api__/resourcetypes"),
                model: ResourceTypeModel,
                state: BackgridUtils.getState(),
                queryParams: BackgridUtils.getQueryParams({
                    _queryFilter: [
                        "name+eq+" + encodeURIComponent('"^(?!Delegation Service$).*"')
                    ]
                }),
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

                    if ($target.parents().hasClass("fr-col-btn-2")) {
                        return;
                    }

                    Router.routeTo(Router.configuration.routes.realmsResourceTypeEdit, {
                        args: _.map([self.realmPath, this.model.id], encodeURIComponent),
                        trigger: true
                    });
                }
            });

            columns = [
                {
                    name: "name",
                    label: $.t("console.authorization.resourceTypes.list.grid.0"),
                    cell: BackgridUtils.TemplateCell.extend({
                        iconClass: "fa-cube",
                        template: "templates/admin/backgrid/cell/IconAndNameCell.html",
                        rendered: function () {
                            this.$el.find("i.fa").addClass(this.iconClass);
                        }
                    }),
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortType: "toggle",
                    editable: false
                },
                {
                    name: "",
                    cell: BackgridUtils.TemplateCell.extend({
                        className: "fr-col-btn-2",
                        template: "templates/admin/backgrid/cell/RowActionsCell.html",
                        events: {
                            "click .edit-row-item": "editItem",
                            "click .delete-row-item": "deleteItem"
                        },
                        editItem: function (e) {
                            self.editRecord(e, this.model.id, Router.configuration.routes.realmsResourceTypeEdit);
                        },
                        deleteItem: function (e) {
                            self.onDeleteClick(e, { type: $.t("console.authorization.common.resourceType") },
                                this.model.id);
                        }
                    }),
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

            paginator = new Backgrid.Extension.ThemeablePaginator({
                collection: self.data.items,
                windowSize: 3
            });

            this.bindDefaultHandlers();

            this.parentRender(function () {
                this.renderToolbar();

                this.$el.find(".table-container").append(grid.render().el);
                this.$el.find(".panel-body").append(paginator.render().el);

                this.data.items.fetch({ reset: true }).done(function () {
                    if (callback) {
                        callback();
                    }
                }).fail(function () {
                    Router.routeTo(Router.configuration.routes.realms, {
                        args: [],
                        trigger: true
                    });
                });
            });
        },

        addNewResourceType: function () {
            Router.routeTo(Router.configuration.routes.realmsResourceTypeNew, {
                args: [encodeURIComponent(this.realmPath)],
                trigger: true
            });
        }
    });
});
