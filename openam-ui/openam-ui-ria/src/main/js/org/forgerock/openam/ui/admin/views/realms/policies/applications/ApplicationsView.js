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

/*global define, FileReader*/

define("org/forgerock/openam/ui/admin/views/realms/policies/applications/ApplicationsView", [
    "jquery",
    "underscore",
    "backbone",
    "backgrid",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/admin/models/policies/ApplicationModel",
    "org/forgerock/openam/ui/admin/views/realms/policies/common/AbstractListView",
    "org/forgerock/openam/ui/admin/delegates/PoliciesDelegate"
], function ($, _, Backbone, Backgrid, Configuration, EventManager, Router, Constants, UIUtils, BackgridUtils, URLHelper,
             ApplicationModel, AbstractListView, PoliciesDelegate) {
    return AbstractListView.extend({
        template: "templates/admin/views/realms/policies/applications/ApplicationsTemplate.html",
        // Used in AbstractListView
        toolbarTemplate: "templates/admin/views/realms/policies/applications/ApplicationsToolbarTemplate.html",

        render: function (args, callback) {
            var self = this,
                Apps,
                columns,
                grid,
                paginator,
                ClickableRow,
                resourceTypesPromise = PoliciesDelegate.listResourceTypes();

            this.realmPath = args[0];
            this.data.selectedItems = [];

            _.extend(this.events, {
                "click #addNewApp": "addNewApplication",
                "click #importPolicies": "startImportPolicies",
                "click #exportPolicies": "exportPolicies",
                "change [name=upload]": "readImportFile"
            });

            Apps = Backbone.PageableCollection.extend({
                url: URLHelper.substitute("__api__/applications"),
                model: ApplicationModel,
                state: BackgridUtils.getState(),
                queryParams: BackgridUtils.getQueryParams({
                    filterName: "eq",
                    _queryFilter: self.getDefaultFilter()
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

                    Router.routeTo(Router.configuration.routes.realmsApplicationEdit, {
                        args: [encodeURIComponent(self.realmPath), encodeURIComponent(this.model.id)],
                        trigger: true
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
                    label: $.t("console.policies.applications.list.grid.0"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortType: "toggle",
                    editable: false
                },
                {
                    name: "description",
                    label: $.t("console.policies.applications.list.grid.1"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortable: false,
                    editable: false
                },
                {
                    name: "resourceTypeUuids",
                    label: $.t("console.policies.applications.list.grid.2"),
                    cell: BackgridUtils.ArrayCell.extend({
                        render: function () {
                            this.$el.empty();

                            var uuids = this.model.get(this.column.attributes.name),
                                names = [],
                                i = 0;

                            for (; i < uuids.length; i++) {
                                names.push(_.findWhere(self.data.resTypes, {uuid: uuids[i]}).name);
                            }

                            this.$el.append(this.buildHtml(names));

                            this.delegateEvents();
                            return this;
                        }
                    }),
                    sortable: false,
                    editable: false
                }
            ];

            this.data.items = new Apps();

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

            this.parentRender(function () {
                this.renderToolbar();

                this.$el.find("#backgridContainer").append(grid.render().el);
                this.$el.find("#paginationContainer").append(paginator.render().el);

                resourceTypesPromise.done(function (xhr) {
                    self.data.resTypes = xhr.result;

                    self.data.items.fetch({reset: true}).done(function () {
                        if (callback) {
                            callback();
                        }
                    });
                });
            });
        },

        // TODO: this configuration is not present, need to delete these applications on server
        getDefaultFilter: function () {
            var exceptions = "",
                defaultApplications,
                returnList = [];

            if (Configuration.globalData.policyEditor) {
                defaultApplications = Configuration.globalData.policyEditor.defaultApplications;
                if (defaultApplications.config.hideByDefault) {
                    exceptions = _.difference(defaultApplications.defaultApplicationList, defaultApplications.config.exceptThese);
                } else {
                    exceptions = defaultApplications.config.exceptThese;
                }
            }

            _.each(exceptions, function (string) {
                returnList.push('name+eq+"^(?!' + string + '$).*"');
            });

            return returnList;
        },

        startImportPolicies: function () {
            this.$el.find("[name=upload]").trigger("click");
        },

        importPolicies: function (e) {
            PoliciesDelegate.importPolicies(e.target.result)
                .done(function () {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policiesUploaded");
                })
                .fail(function (e) {
                    var applicationNotFoundInRealm = " application not found in realm",
                        responseText = e ? e.responseText : '',
                        message = $($.parseXML(responseText)).find("message").text(),
                        index = message ? message.indexOf(applicationNotFoundInRealm) : -1;

                    if (index > -1) {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, {key: "policiesImportFailed",
                            applicationName: message.slice(0, index)});
                    } else {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policiesUploadFailed");
                    }
                });
        },

        readImportFile: function () {
            var file = this.$el.find("[name=upload]")[0].files[0],
                reader = new FileReader();
            reader.onload = this.importPolicies;
            if (file) {
                reader.readAsText(file, "UTF-8");
            }
        },

        exportPolicies: function () {
            this.$el.find("#exportPolicies").attr("href", Constants.host + "/" + Constants.context + "/xacml/policies");
        },

        addNewApplication: function (e) {
            Router.routeTo(Router.configuration.routes.realmsApplicationEdit, {
                args: [encodeURIComponent(this.realmPath)],
                trigger: true
            });
        }
    });
});
