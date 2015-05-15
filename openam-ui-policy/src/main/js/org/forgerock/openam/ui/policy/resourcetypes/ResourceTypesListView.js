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

/*global define, $, _, Backgrid, Backbone */

define("org/forgerock/openam/ui/policy/resourcetypes/ResourceTypesListView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/policy/common/GenericGridView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/common/util/RealmHelper",
    "org/forgerock/openam/ui/policy/delegates/PolicyDelegate",
    "org/forgerock/openam/ui/policy/resourcetypes/ResourceTypeModel",
    "org/forgerock/openam/ui/policy/util/BackgridUtils"
], function (AbstractView, GenericGridView, UIUtils, Router, Constants, Configuration, EventManager, Messages, URLHelper,
             RealmHelper, PolicyDelegate, ResourceTypeModel, BackgridUtils) {

    var ResourceTypesListView = AbstractView.extend({
        template: "templates/policy/resourcetypes/ResourceTypesListTemplate.html",
        events: {
            'click #deleteResTypes': 'deleteResourceTypes'
        },

        render: function (args, callback) {
            var self = this,
                ResourceTypes,
                columns,
                grid,
                paginator,
                ClickableRow;

            this.data.realm = Configuration.globalData.auth.realm;
            this.data.selectedResourceTypes = [];

            ResourceTypes = Backbone.PageableCollection.extend({
                url: URLHelper.substitute("__api__/resourcetypes"),
                model: ResourceTypeModel,
                queryParams: {
                    _queryFilter: BackgridUtils.queryFilter,
                    pageSize: null,  // todo implement pagination
                    _pagedResultsOffset: null //todo implement pagination
                },

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

                    if ($target.is('input') || $target.parents('.select-row-cell').length === 1) {
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
                    editable: false
                },
                {
                    name: "description",
                    label: $.t("policy.resourceTypes.list.grid.1"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    editable: false
                },
                {
                    name: "patterns",
                    label: $.t("policy.resourceTypes.list.grid.2"),
                    cell: BackgridUtils.ArrayCell,
                    headerCell: BackgridUtils.FilterHeaderCell,
                    editable: false
                },
                {
                    name: "actions",
                    label: $.t("policy.resourceTypes.list.grid.3"),
                    cell: BackgridUtils.ObjectCell,
                    headerCell: BackgridUtils.FilterHeaderCell,
                    editable: false
                }
            ];

            self.data.resourceTypes = new ResourceTypes();

            self.data.resourceTypes.on("backgrid:selected", function (model, selected) {
                self.onRowSelect(model, selected);
            });

            grid = new Backgrid.Grid({
                columns: columns,
                row: ClickableRow,
                collection: self.data.resourceTypes,
                emptyText: $.t("policy.resourceTypes.list.noResults")
            });

            paginator = new Backgrid.Extension.Paginator({
                collection: self.data.resourceTypes,
                windowSize: 3
            });

            this.parentRender(function () {
                this.renderToolbar();

                this.$el.find("#backgridContainer").append(grid.render().el);
                this.$el.find("#paginationContainer").append(paginator.render().el);

                this.data.resourceTypes.fetch({reset: true}).done(function (xhr) {
                    if (callback) {
                        callback();
                    }
                });
            });
        },

        onRowSelect: function (model, selected) {
            if (selected) {
                this.data.selectedResourceTypes.push(model.id);
            } else {
                this.data.selectedResourceTypes = _.without(this.data.selectedResourceTypes, model.id);
            }

            this.renderToolbar();
        },

        renderToolbar: function () {
            this.$el.find('#gridToolbar').html(UIUtils.fillTemplateWithData(
                "templates/policy/resourcetypes/ResourceTypesListToolbarTemplate.html", this.data));
        },

        deleteResourceTypes: function (e) {
            e.preventDefault();

            if ($(e.target).hasClass('inactive')) {
                return;
            }

            var self = this,
                i = 0,
                app,
                onAppDestroy = function () {
                    self.data.selectedResourceTypes = [];
                    self.data.resourceTypes.fetch({reset: true});
                    self.renderToolbar();
                },
                onSuccess = function (model, response, options) {
                    onAppDestroy();
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, 'deleteSuccess');
                },
                onError = function (model, response, options) {
                    onAppDestroy();
                    Messages.messages.addMessage({message: response.responseJSON.message, type: 'error'});
                };

            for (; i < this.data.selectedResourceTypes.length; i++) {
                app = this.data.resourceTypes.get(this.data.selectedResourceTypes[i]);

                app.destroy({
                    success: onSuccess,
                    error: onError
                });
            }
        }
    });

    return new ResourceTypesListView();
});