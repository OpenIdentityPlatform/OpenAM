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
 * Portions copyright 2014-2015 ForgeRock AS.
 */

/*global define*/

define("org/forgerock/openam/ui/admin/views/realms/authorization/policies/PoliciesView", [
    "jquery",
    "underscore",
    "backbone",
    "backbone.paginator",
    "backgrid",
    "backgrid.filter",
    "backgrid.paginator",
    "backgrid.selectall",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/admin/delegates/PoliciesDelegate",
    "org/forgerock/openam/ui/admin/models/authorization/PolicyModel",
    "org/forgerock/openam/ui/admin/views/realms/authorization/common/AbstractListView",
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/EditPolicyView"
], function ($, _, Backbone, BackbonePaginator, Backgrid, BackgridFilter, BackgridPaginator, BackgridSelectAll,
             Configuration, EventManager, Router, Constants, BackgridUtils, URLHelper, PoliciesDelegate, PolicyModel,
             AbstractListView,
             EditPolicyView) {

    var PoliciesView = AbstractListView.extend({
        element: "#policiesPanel",
        template: "templates/admin/views/realms/authorization/policies/PoliciesTemplate.html",
        // Used in AbstractListView
        toolbarTemplate: "templates/admin/views/realms/authorization/policies/PoliciesToolbarTemplate.html",
        events: {
            "click #addNewPolicy": "addNewPolicy"
        },
        render: function (data, callback) {
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
                    _queryFilter: ['applicationName+eq+"' + this.data.applicationModel.id + '"']
                }),
                parseState: BackgridUtils.parseState,
                parseRecords: BackgridUtils.parseRecords,
                sync: function (method, model, options) {
                    options.beforeSend = function (xhr) {
                        xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=2.0");
                    };
                    return BackgridUtils.sync(method, model, options);
                }
            });

            ClickableRow = BackgridUtils.ClickableRow.extend({
                callback: function (e) {
                    var $target = $(e.target);

                    if ($target.parents().hasClass("row-actions")) {
                        return;
                    }

                    EditPolicyView.render({
                        applicationModel: self.data.applicationModel,
                        policyModel: this.model,
                        savePolicyCallback: function () {
                            EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                                route: Router.currentRoute,
                                args:[encodeURIComponent(self.data.realmPath), self.data.applicationModel.id]
                            });
                        }
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
                        className: "row-actions",
                        template: "templates/admin/backgrid/cell/RowActionsCell.html",
                        events: {
                            "click .edit-row-item": "editItem",
                            "click .delete-row-item": "deleteItem"
                        },
                        editItem: function (e) {
                            EditPolicyView.render({
                                applicationModel: self.data.applicationModel,
                                policyModel: this.model,
                                savePolicyCallback: function () {
                                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                                        route: Router.currentRoute,
                                        args:[encodeURIComponent(self.data.realmPath), self.data.applicationModel.id]
                                    });
                                }
                            });
                        },
                        deleteItem: function (e) {
                            self.deleteRecord(e, this.model.id, function () {
                                EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                                    route: Router.currentRoute,
                                    args:[encodeURIComponent(self.data.realmPath), self.data.applicationModel.id]
                                });
                            });
                        }
                    }),
                    sortable: false,
                    editable: false
                }
            ];

            this.data.items = new Policies();

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

            this.data.items.fetch({reset: true}).done(function () {
                self.parentRender(function () {

                    if (self.data.items.length) {
                        self.renderToolbar();
                        self.$el.find("#backgridContainer").append(grid.render().el);
                        self.$el.find("#paginationContainer").append(paginator.render().el);
                    }

                    if (callback) {
                        callback(self.data.items.length);
                    }
                });
            });
        },

        addNewPolicy: function () {
            var self = this;
            EditPolicyView.render({
                    savePolicyCallback: function () {
                        EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                            route: Router.currentRoute,
                            args:[encodeURIComponent(self.data.realmPath), self.data.applicationModel.id]
                        });
                    },
                    applicationModel: this.data.applicationModel
                });
        }
    });

    return new PoliciesView();
});