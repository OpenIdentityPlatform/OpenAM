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

define("org/forgerock/openam/ui/admin/views/realms/policies/policies/PoliciesView", [
    "jquery",
    "underscore",
    "backbone",
    "backgrid",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/admin/delegates/PoliciesDelegate",
    "org/forgerock/openam/ui/admin/models/policies/PolicyModel",
    "org/forgerock/openam/ui/admin/views/realms/policies/common/AbstractListView",
    "org/forgerock/openam/ui/admin/views/realms/policies/policies/EditPolicyView"
], function ($, _, Backbone, Backgrid, Configuration, Router, BackgridUtils, UIUtils, URLHelper, PoliciesDelegate,
             PolicyModel, AbstractListView, EditPolicyView) {

    var PoliciesListView = AbstractListView.extend({
        element: "#policies",
        template: "templates/admin/views/realms/policies/policies/PoliciesTemplate.html",
        toolbarTemplate: "templates/admin/views/realms/policies/policies/PoliciesToolbarTemplate.html",

        render: function (data, callback) {
            var self = this,
                Policies,
                columns,
                grid,
                paginator,
                ClickableRow;

            _.extend(this.data, data);

            this.data.selectedItems = [];

            _.extend(this.events, {
                "click #addNewPolicy": "addNewPolicy"
            });

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

                    if ($target.is("input") || $target.is(".select-row-cell")) {
                        return;
                    }

                    EditPolicyView.render({
                        applicationModel: self.data.applicationModel,
                        policyModel: this.model,
                        savePolicyCallback: function () {
                            self.data.items.fetch({reset: true});
                        }
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
                    label: $.t("console.policies.policies.list.grid.0"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortType: "toggle",
                    editable: false
                },
                {
                    name: "description",
                    label: $.t("console.policies.policies.list.grid.1"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortable: false,
                    editable: false
                },
                {
                    name: "resources",
                    label: $.t("console.policies.policies.list.grid.2"),
                    cell: BackgridUtils.ArrayCell,
                    sortable: false,
                    editable: false
                },
                {
                    name: "actionValues",
                    label: $.t("console.policies.policies.list.grid.3"),
                    cell: BackgridUtils.ObjectCell,
                    sortable: false,
                    editable: false
                }
            ];

            this.data.items = new Policies();

            grid = new Backgrid.Grid({
                columns: columns,
                row: ClickableRow,
                collection: self.data.items,
                className: "backgrid table",
                emptyText: $.t("console.common.noResults")
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

                this.data.items.fetch({reset: true}).done(function () {
                    if (callback) {
                        callback();
                    }
                });
            });
        },

        addNewPolicy: function () {
            var self = this;
            EditPolicyView.render({
                    savePolicyCallback: function () {
                        self.data.items.fetch({reset: true});
                    },
                    applicationModel: this.data.applicationModel
                },
                function () {
                    self.data.items.fetch({reset: true});
                });
        }
    });

    return new PoliciesListView();
});