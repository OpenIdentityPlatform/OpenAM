/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014-2015 ForgeRock AS.
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

/*global window, define, $, _, Backbone */

define("org/forgerock/openam/ui/policy/policies/PoliciesListView", [
    "backgrid",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/policy/applications/ApplicationModel",
    "org/forgerock/openam/ui/policy/common/AbstractListView",
    "org/forgerock/openam/ui/policy/policies/PolicyModel",
    "org/forgerock/openam/ui/policy/util/BackgridUtils"
], function (Backgrid, Configuration, Router, UIUtils, URLHelper, ApplicationModel, AbstractListView, PolicyModel,
             BackgridUtils) {

    var PoliciesListView = AbstractListView.extend({
        template: 'templates/policy/policies/PoliciesListTemplate.html',
        toolbarTemplate: 'templates/policy/policies/PoliciesListToolbarTemplate.html',

        render: function (args, callback) {
            var self = this,
                Policies,
                columns,
                grid,
                paginator,
                ClickableRow,
                application = new ApplicationModel({name: args[0]}).fetch();

            this.data.realm = Configuration.globalData.auth.realm;
            this.data.appName = args[0];
            this.data.selectedItems = [];

            Policies = Backbone.PageableCollection.extend({
                url: URLHelper.substitute("__api__/policies"),
                model: PolicyModel,
                state: BackgridUtils.getState(),
                queryParams: BackgridUtils.getQueryParams({
                    _queryFilter: 'applicationName+eq+"' + args[0] + '"'
                }),
                parseState: BackgridUtils.parseState,
                parseRecords: BackgridUtils.parseRecords,
                sync: function (method, model, options) {
                    options.beforeSend = function (xhr) {
                        xhr.setRequestHeader('Accept-API-Version', 'protocol=1.0,resource=2.0');
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

                    Router.routeTo(Router.configuration.routes.editPolicy,
                        {args: [self.data.appName, encodeURIComponent(this.model.id)], trigger: true});
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
                    label: $.t("policy.policies.list.grid.0"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortType: "toggle",
                    editable: false
                },
                {
                    name: "description",
                    label: $.t("policy.policies.list.grid.1"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortable: false,
                    editable: false
                },
                {
                    name: "resources",
                    label: $.t("policy.policies.list.grid.2"),
                    cell: BackgridUtils.ArrayCell,
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortable: false,
                    editable: false
                },
                {
                    name: "actionValues",
                    label: $.t("policy.policies.list.grid.3"),
                    cell: BackgridUtils.ObjectCell,
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortable: false,
                    editable: false
                }
                // TODO: add other columns
            ];

            this.data.items = new Policies();

            grid = new Backgrid.Grid({
                columns: columns,
                row: ClickableRow,
                collection: self.data.items,
                emptyText: $.t("policy.policies.list.noResults")
            });

            paginator = new Backgrid.Extension.Paginator({
                collection: self.data.items,
                windowSize: 3
            });

            this.bindDefaultHandlers();

            this.parentRender(function () {
                application.done(function (app) {
                    var data = { appEditable: app.editable, appName: app.name };
                    UIUtils.fillTemplateWithData("templates/policy/policies/PoliciesListHeaderTemplate.html", data, function (html) {
                        self.$el.find('#appNameHeader').html(html);
                    });
                });

                UIUtils.fillTemplateWithData(this.toolbarTemplate, this.data, function (tpl) {
                    self.$el.find(self.toolbarTemplateID).html(tpl);
                });

                this.$el.find("#backgridContainer").append(grid.render().el);
                this.$el.find("#paginationContainer").append(paginator.render().el);

                this.data.items.fetch({reset: true});

                if (callback) {
                    callback();
                }
            });
        }
    });

    return new PoliciesListView();
});