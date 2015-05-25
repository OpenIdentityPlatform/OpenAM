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

/*global define, $, _, FileReader, Backbone */

define("org/forgerock/openam/ui/policy/applications/ApplicationsListView", [
    "backgrid",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/policy/applications/ApplicationModel",
    "org/forgerock/openam/ui/policy/common/AbstractListView",
    "org/forgerock/openam/ui/policy/delegates/PolicyDelegate",
    "org/forgerock/openam/ui/policy/util/BackgridUtils"
], function (Backgrid, Configuration, EventManager, Router, Constants, UIUtils, URLHelper, ApplicationModel,
             AbstractListView, PolicyDelegate, BackgridUtils) {

    var ApplicationsListView = AbstractListView.extend({
        template: 'templates/policy/applications/ApplicationsListTemplate.html',
        toolbarTemplate: 'templates/policy/applications/ApplicationsListToolbarTemplate.html',

        render: function (args, callback) {
            var self = this,
                Apps,
                columns,
                grid,
                paginator,
                ClickableRow;

            this.data.realm = Configuration.globalData.auth.realm;
            this.data.selectedItems = [];

            _.extend(this.events, {
                'click #importPolicies': 'startImportPolicies',
                'click #exportPolicies': 'exportPolicies',
                'change #realImport': 'readImportFile'
            });

            Apps = Backbone.PageableCollection.extend({
                url: URLHelper.substitute("__api__/applications"),
                model: ApplicationModel,
                state: BackgridUtils.getState(),
                queryParams: BackgridUtils.getQueryParams(),
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

                    if ($target.is('a') || $target.is('input') || $target.is('.select-row-cell') ||
                        $target.is('.fa') || $target.is('.template-cell')) {
                        return;
                    }
                    Router.routeTo(Router.configuration.routes.managePolicies, {args: [encodeURIComponent(this.model.id)], trigger: true});
                }
            });

            columns = [
                {
                    name: "",
                    cell: "select-row",
                    headerCell: "select-all"
                },
                {
                    name: "",
                    label: $.t("policy.applications.list.grid.0"),
                    cell: BackgridUtils.TemplateCell.extend({
                        callback: function (e, modelId) {
                            Router.routeTo(Router.configuration.routes.editApp, {args: [encodeURIComponent(modelId)],
                                trigger: true});
                        },
                        additionalClassName: 'edit-cell',
                        template: "templates/policy/applications/ApplicationsListActionsCellTemplate.html"
                    }),
                    sortable: false,
                    editable: false
                },
                {
                    name: "name",
                    label: $.t("policy.applications.list.grid.1"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortType: "toggle",
                    editable: false
                },
                {
                    name: "description",
                    label: $.t("policy.applications.list.grid.2"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortType: "toggle",
                    editable: false
                },
                {
                    name: "resourceTypeUuids",
                    label: $.t("policy.applications.list.grid.3"),
                    cell: BackgridUtils.ArrayCell,
                    headerCell: BackgridUtils.FilterHeaderCell,
                    sortType: "toggle",
                    editable: false
                }
                // TODO: add other columns
            ];

            this.data.items = new Apps();

            grid = new Backgrid.Grid({
                columns: columns,
                row: ClickableRow,
                collection: self.data.items,
                emptyText: $.t("policy.applications.list.noResults")
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

                this.data.items.fetch({reset: true}).done(function (xhr) {
                    self.$el.find('.fa[data-toggle="popover"]').popover();

                    if (callback) {
                        callback();
                    }
                });
            });
        },

        getDefaultFilter: function () {
            var exceptions = '',
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

            return returnList.join('+AND+');
        },

        startImportPolicies: function () {
            // Triggering the click on the hidden input with type "file" to upload the file
            this.$el.find("#realImport").trigger("click");
        },

        importPolicies: function (e) {
            PolicyDelegate.importPolicies(e.target.result)
                .done(function () {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policiesUploaded");
                })
                .fail(function (e) {
                    var applicationNotFoundInRealm = " application not found in realm",
                        responseText = e ? e.responseText : '',
                        message = $($.parseXML(responseText)).find('message').text(),
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
            var file = this.$el.find("#realImport")[0].files[0],
                reader = new FileReader();
            reader.onload = this.importPolicies;
            if (file) {
                reader.readAsText(file, "UTF-8");
            }
        },

        exportPolicies: function () {
            this.$el.find("#exportPolicies").attr('href', Constants.host + "/" + Constants.context + "/xacml/policies");
        }

    });

    return new ApplicationsListView();
});