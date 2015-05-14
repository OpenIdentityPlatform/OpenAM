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

/*global define, $, _, sessionStorage, FileReader, Backgrid, Backbone */

define("org/forgerock/openam/ui/policy/applications/ApplicationsListView", [
    "backgrid",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/common/util/RealmHelper",
    "org/forgerock/openam/ui/policy/common/GenericGridView",
    "org/forgerock/openam/ui/policy/delegates/PolicyDelegate",
    "org/forgerock/openam/ui/policy/applications/ApplicationModel",
    "org/forgerock/openam/ui/policy/util/BackgridUtils"
], function (Backgrid, AbstractView, UIUtils, Router, Constants, Configuration, EventManager, Messages, URLHelper,
             RealmHelper, GenericGridView, PolicyDelegate, ApplicationModel, BackgridUtils) {

    var ApplicationsListView = AbstractView.extend({
        template: "templates/policy/applications/ApplicationsListTemplate.html",

        events: {
            'click .fa-pencil': 'editApplication',
            'click #deleteApps': 'deleteApplications',
            'click #importPolicies': 'startImportPolicies',
            'click #exportPolicies': 'exportPolicies',
            'change #realImport': 'readImportFile'
        },

        render: function (args, callback) {
            var self = this,
                Apps,
                columns,
                grid,
                paginator,
                ClickableRow;

            this.data.realm = Configuration.globalData.auth.realm;
            this.data.selectedApplications = [];

            Apps = Backbone.PageableCollection.extend({
                url: URLHelper.substitute("__api__/applications"),
                model: ApplicationModel,
                queryParams: {
                    _queryFilter: BackgridUtils.queryFilter,
                    pageSize: null,  // todo implement pagination
                    _pagedResultsOffset: null //todo implement pagination
                },

                parseRecords: BackgridUtils.parseRecords,
                sync: BackgridUtils.sync
            });

            ClickableRow = Backgrid.Row.extend({
                events: {
                    "click": "onClick"
                },

                onClick: function (e) {
                    var $target = $(e.target);

                    if ($target.is('a') || $target.is('input') || $target.parents('.select-row-cell').length === 1 ||
                        $target.parents('.template-cell').length === 1) {
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
                        template: "templates/policy/applications/ApplicationsListActionsCellTemplate.html"
                    }),
                    editable: false
                },
                {
                    name: "name",
                    label: $.t("policy.applications.list.grid.1"),
                    cell: BackgridUtils.UriExtCell,
                    headerCell: BackgridUtils.FilterHeaderCell,
                    href: function (rawValue, formattedValue, model) {
                        return "#app/" + encodeURIComponent(model.id);
                    },
                    editable: false
                },
                {
                    name: "description",
                    label: $.t("policy.applications.list.grid.2"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    editable: false
                },
                {
                    name: "resourceTypeUuids",
                    label: $.t("policy.applications.list.grid.3"),
                    cell: "string",
                    headerCell: BackgridUtils.FilterHeaderCell,
                    editable: false
                }

                // TODO: add other columns
            ];

            self.data.applications = new Apps();

            self.data.applications.on("backgrid:selected", function (model, selected) {
                self.onRowSelect(model, selected);
            });

            grid = new Backgrid.Grid({
                columns: columns,
                row: ClickableRow,
                collection: self.data.applications,
                emptyText: $.t("policy.applications.list.noResults")
            });

            paginator = new Backgrid.Extension.Paginator({
                collection: self.data.applications,
                windowSize: 3
            });

            this.parentRender(function () {
                this.renderToolbar();

                this.$el.find("#backgridContainer").append(grid.render().el);
                this.$el.find("#paginationContainer").append(paginator.render().el);

                this.data.applications.fetch({reset: true}).done(function (xhr) {
                    self.$el.find('.fa[data-toggle="popover"]').popover();

                    if (callback) {
                        callback();
                    }
                });


                /*
                this.appGridView = new GenericGridView();
                this.appGridView.render(this.data, {
                    rowUid: 'name',
                    element: '#manageApps',
                    tpl: 'templates/policy/applications/AppsListGridTemplate.html',
                    actionsTpl: 'templates/policy/applications/ApplicationsListToolbarTemplate.html',
                    gridId: 'apps',
                    initOptions: this.getGridInitOptions(),
                    additionalOptions: this.getGridAdditionalOptions()
                }, function(){
                    self.$el.find('.fa[data-toggle="popover"]').popover();
                    if (callback) {
                        callback();
                    }
                });
                */
            });
        },

        onRowSelect: function (model, selected) {
            if (selected) {
                this.data.selectedApplications.push(model.id);
            } else {
                this.data.selectedApplications = _.without(this.data.selectedApplications, model.id);
            }

            this.renderToolbar();
        },

        renderToolbar: function () {
            this.$el.find('#gridToolbar').html(UIUtils.fillTemplateWithData("templates/policy/applications/ApplicationsListToolbarTemplate.html", this.data));
        },

        editApplication: function (e) {
            Router.routeTo(Router.configuration.routes.editApp, {args: [encodeURIComponent($(e.target).data('appName'))], trigger: true});
        },

        /*
        getGridInitOptions: function () {
            var self = this,
                actionsFormatter = function (cellVal, options, rowObject) {
                    return UIUtils.fillTemplateWithData("templates/policy/applications/ApplicationsListActionsCellTemplate.html", rowObject);
                },
                datePick = function (elem) {
                    return self.appGridView.datePicker(self.appGridView, elem);
                };

            return {
                url: RealmHelper.decorateURLWithOverrideRealm('/' + Constants.context + '/json/applications'),
                colNames: ['', 'Edit', 'Name', 'Description', 'Application Base', 'Author', 'Created', 'Last Modified'],
                colModel: [
                    {name: 'iconChB', width: 40, sortable: false, formatter: this.appGridView.checkBoxFormatter, frozen: true, title: false, search: false, hidedlg: true},
                    {name: 'tableActions', width: 65, sortable: false, formatter: actionsFormatter, frozen: true, search: false, hidedlg: true},
                    {name: 'name', width: 262, frozen: true, hidedlg: true, searchoptions: {sopt: ['gt', 'lt', 'ge', 'le', 'eq']}},
                    {name: 'description', width: 263, sortable: false, searchoptions: {sopt: ['gt', 'lt', 'ge', 'le', 'eq']}},
                    {name: 'resources', width: 263, sortable: false, search: false, formatter: UIUtils.commonJQGridFormatters.arrayFormatter},
                    {name: 'createdBy', width: 250, hidden: true},
                    {name: 'creationDate', width: 150, index: 'creationDate', formatter: UIUtils.commonJQGridFormatters.dateFormatter, hidden: true, searchoptions: {searchhidden: true, dataInit: datePick, sopt: ['gt', 'lt', 'ge', 'le', 'eq']}},
                    {name: 'lastModifiedDate', width: 150, index: 'lastModifiedDate', formatter: UIUtils.commonJQGridFormatters.dateFormatter, hidden: true, searchoptions: {searchhidden: true, dataInit: datePick, sopt: ['gt', 'lt', 'ge', 'le', 'eq']}}
                ],
                beforeSelectRow: function (rowId, e) {
                    var checkBoxCellSelected = self.appGridView.isCheckBoxCellSelected(e);
                    if (!checkBoxCellSelected && !$(e.target).hasClass('fa-pencil')) {
                        self.viewPolicies(e);
                    }
                    return checkBoxCellSelected;
                },
                onSelectRow: function (rowid, status, e) {
                    self.appGridView.onRowSelect(rowid, status, e);
                },
                search: true,
                sortname: 'name',
                autowidth: true,
                shrinkToFit: false,
                pager: '#appsPager'
            };
        },

        getGridAdditionalOptions: function () {
            var self = this;

            return {
                search: true,
                searchFilter: this.getDefaultFilter(),
                columnChooserOptions: {
                    width: 501,
                    height: 180
                },
                storageKey: 'PE-mng-apps-sel-' + this.data.realm,
                apiVersion: 'protocol=1.0,resource=2.0',
                // TODO: completely remove serializeGridData() from here once AME-4925 is ready.
                serializeGridData: function (postedData) {
                    var colNames = _.pluck($(this).jqGrid('getGridParam', 'colModel'), 'name');
                    return self.appGridView.serializeDataToFilter(postedData, colNames);
                },
                callback: function () {
                    self.appGridView.grid.on('jqGridAfterInsertRow', function (e, rowid, rowdata) {
                        self.appGridView.selectRow(e, rowid, rowdata);
                    });

                    self.appGridView.grid.jqGrid('setFrozenColumns');
                }
            };
        },

        viewPolicies: function (e) {
            Router.routeTo(Router.configuration.routes.managePolicies, {args: [this.getAppName(e)], trigger: true});
        },

        getAppName: function (e) {
            return this.appGridView.grid.getRowData(this.appGridView.getSelectedRowId(e)).name;
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
                returnList.push({field: 'name', op: 'eq', val: '^(?!' + string + '$).*'});
            });

            return returnList;
        },
        */

        deleteApplications: function (e) {
            e.preventDefault();

            if ($(e.target).hasClass('inactive')) {
                return;
            }

            var self = this,
                i = 0,
                app,
                onAppDestroy = function () {
                    self.data.selectedApplications = [];
                    self.data.applications.fetch({reset: true});
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

            for (; i < this.data.selectedApplications.length; i++) {
                app = this.data.applications.get(this.data.selectedApplications[i]);

                app.destroy({
                    success: onSuccess,
                    error: onError
                });
            }
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
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, {key: "policiesImportFailed", applicationName: message.slice(0, index)});
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