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
 * Portions copyright 2014-2016 ForgeRock AS.
 */


define([
    "jquery",
    "lodash",
    "backbone",
    "backbone.paginator",
    "backgrid-filter",
    "org/forgerock/commons/ui/common/backgrid/Backgrid",
    "org/forgerock/commons/ui/common/backgrid/extension/ThemeablePaginator",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/admin/services/realm/PoliciesService",
    "org/forgerock/openam/ui/admin/models/authorization/PolicyModel",
    "org/forgerock/openam/ui/admin/views/realms/authorization/common/AbstractListView"
], function ($, _, Backbone, BackbonePaginator, BackgridFilter, Backgrid, ThemeablePaginator, Configuration,
             EventManager, Router, Constants, BackgridUtils, URLHelper, PoliciesService, PolicyModel,
             AbstractListView) {
    var PoliciesView = AbstractListView.extend({
        element: "#policiesPanel",
        template: "templates/admin/views/realms/authorization/policies/PoliciesTemplate.html",
        // Used in AbstractListView
        toolbarTemplate: "templates/admin/views/realms/authorization/policies/PoliciesToolbarTemplate.html",
        events: {
            "click [data-add-entity]": "addNewPolicy"
        },
        render (data, callback) {
            var self = this,
                Policies,
                columns,
                grid,
                paginator,
                ClickableRow;

            _.extend(this.data, data);

            Policies = Backbone.PageableCollection.extend({
                url: URLHelper.substitute("__api__/policies"),
                model: PolicyModel,
                state: BackgridUtils.getState(),
                queryParams: BackgridUtils.getQueryParams({
                    filterName: "eq",
                    _queryFilter: [`applicationName+eq+"${encodeURIComponent(this.data.policySetModel.id)}"`]
                }),
                parseState: BackgridUtils.parseState,
                parseRecords: BackgridUtils.parseRecords,
                sync (method, model, options) {
                    options.beforeSend = function (xhr) {
                        xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=2.0");
                    };
                    return BackgridUtils.sync(method, model, options);
                }
            });

            ClickableRow = BackgridUtils.ClickableRow.extend({
                callback (e) {
                    var $target = $(e.target);

                    if ($target.parents().hasClass("fr-col-btn-2")) {
                        return;
                    }

                    Router.routeTo(Router.configuration.routes.realmsPolicyEdit, {
                        args: _.map([self.data.realmPath, self.data.policySetModel.id, this.model.id],
                            encodeURIComponent),
                        trigger: true
                    });
                }
            });

            columns = [
                {
                    name: "name",
                    label: $.t("console.authorization.policies.list.grid.0"),
                    cell: BackgridUtils.TemplateCell.extend({
                        iconClass: "fa-gavel",
                        template: "templates/admin/backgrid/cell/IconAndNameCell.html",
                        rendered () {
                            this.$el.find("i.fa").addClass(this.iconClass);
                        }
                    }),
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortType: "toggle",
                    editable: false
                },
                {
                    name: "active",
                    label: $.t("console.authorization.policies.list.grid.1"),
                    cell: BackgridUtils.TemplateCell.extend({
                        template: "templates/admin/backgrid/cell/StatusCell.html"
                    }),
                    sortable: false,
                    editable: false
                },
                {
                    name: "",
                    cell: BackgridUtils.TemplateCell.extend({
                        className: "fr-col-btn-2",
                        template: "templates/admin/backgrid/cell/RowActionsCell.html",
                        events: {
                            "click [data-edit-item]": "editItem",
                            "click [data-delete-item]": "deleteItem"
                        },
                        editItem () {
                            Router.routeTo(Router.configuration.routes.realmsPolicyEdit, {
                                args: _.map([self.data.realmPath, self.data.policySetModel.id, this.model.id],
                                    encodeURIComponent),
                                trigger: true
                            });
                        },
                        deleteItem (e) {
                            self.onDeleteClick(e, { type: $.t("console.authorization.common.policy") },
                                this.model.id,
                                function () {
                                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                                        route: Router.currentRoute,
                                        args: _.map([self.data.realmPath, self.data.policySetModel.id],
                                                encodeURIComponent)
                                    });
                                }
                            );
                        }
                    }),
                    sortable: false,
                    editable: false
                }
            ];

            this.data.items = new Policies();

            grid = new Backgrid.Grid({
                columns,
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

            this.data.items.fetch({ reset: true }).done(function () {
                self.parentRender(function () {

                    if (self.data.items.length) {
                        self.renderToolbar();
                        self.$el.find(".table-container").append(grid.render().el);
                        self.$el.find("#paginationContainer").append(paginator.render().el);
                    }

                    if (callback) {
                        callback();
                    }
                });
            });
        },

        addNewPolicy () {
            Router.routeTo(Router.configuration.routes.realmsPolicyNew, {
                args: _.map([this.data.realmPath, this.data.policySetModel.id], encodeURIComponent),
                trigger: true
            });
        }
    });

    return new PoliciesView();
});
