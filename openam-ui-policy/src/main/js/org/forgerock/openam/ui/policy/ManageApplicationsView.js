/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/**
 * @author Eugenia Sergueeva
 */

/*global define, $, _, sessionStorage, FileReader */

define("org/forgerock/openam/ui/policy/ManageApplicationsView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/policy/GenericGridView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openam/ui/policy/PolicyDelegate"
], function (AbstractView, GenericGridView, uiUtils, router, constants, conf, eventManager, policyDelegate) {
    var ManageApplicationsView = AbstractView.extend({
        baseTemplate: 'templates/policy/BaseTemplate.html',
        template: "templates/policy/ManageAppsTemplate.html",

        events: {
            'click .icon-pencil': 'editApplication',
            'click .icon-file': 'viewPolicies',
            'click #deleteApps': 'deleteApplications',
            'click #importPolicies': 'startImportPolicies',
            'click #exportPolicies': 'exportPolicies',
            'change #realImport': 'readImportFile'
        },

        render: function (args, callback) {
            this.data.realm = conf.globalData.auth.realm;

            this.parentRender(function () {
                this.subrealm = this.data.realm !== "/" ? this.data.realm : "";

                this.appGridView = new GenericGridView();
                this.appGridView.render({
                    element: '#manageApps',
                    tpl: 'templates/policy/ManageAppsGridTemplate.html',
                    actionsTpl: 'templates/policy/ManageAppsGridActionsTemplate.html',
                    gridId: 'apps',
                    initOptions: this.getGridInitOptions(),
                    additionalOptions: this.getGridAdditionalOptions()
                }, callback);
            });
        },

        getGridInitOptions: function () {
            var self = this,
                actionsFormatter = function (cellVal, options, rowObject) {
                    return uiUtils.fillTemplateWithData("templates/policy/ManageAppsGridCellActionsTemplate.html");
                };

            return {
                url: '/' + constants.context + '/json' + this.subrealm + '/applications',
                colNames: ['', '', 'Name', 'Description', 'Application Base', 'Author', 'Created', 'Last Modified'],
                colModel: [
                    {name: 'iconChB',           width: 40,  sortable: false, formatter: this.appGridView.checkBoxFormatter, frozen: true, title: false, search: false, hidedlg: true},
                    {name: 'actions',           width: 65,  sortable: false, formatter: actionsFormatter, frozen: true, title: false, search: false, hidedlg: true},
                    {name: 'name',              width: 262, frozen: true, hidedlg: true},
                    {name: 'description',       width: 263, sortable: false},
                    {name: 'resources',         width: 263, sortable: false, search: false, formatter: uiUtils.commonJQGridFormatters.arrayFormatter},
                    {name: 'createdBy',         width: 250, hidden: true},
                    {name: 'creationDate',      width: 150, formatter: uiUtils.commonJQGridFormatters.dateFormatter, hidden: true, search: false},
                    {name: 'lastModifiedDate',  width: 150, formatter: uiUtils.commonJQGridFormatters.dateFormatter, hidden: true, search: false}
                ],
                beforeSelectRow: function (rowId, e) {
                    var checkBoxCellSelected = self.appGridView.isCheckBoxCellSelected(e);
                    if (!checkBoxCellSelected && !$(e.target).hasClass('icon-pencil')) {
                        self.viewPolicies(e);
                    }
                    return checkBoxCellSelected;
                },
                onSelectRow: function (rowid, status, e) {
                    self.appGridView.onRowSelect(rowid, status, e);
                },
                sortname: 'name',
                width: 918,
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
                // TODO: completely remove serializeGridData() from here once AME-4925 is ready.
                serializeGridData: function (postedData) {
                    var colNames = _.pluck($(this).jqGrid('getGridParam', 'colModel'), 'name'),
                        filter = '';

                    _.each(colNames, function (element, index, list) {
                        if (postedData[element]) {
                            if (filter.length > 0) {
                                filter += ' AND ';
                            }
                            filter = filter.concat(element, ' eq "*', postedData[element], '*"');
                        }
                        delete postedData[element];
                    });

                    return filter;
                },
                callback: function () {
                    self.appGridView.grid.on('jqGridAfterInsertRow', function (e, rowid, rowdata) {
                        self.appGridView.selectRow(e, rowid, rowdata);
                    });

                    self.appGridView.grid.jqGrid('setFrozenColumns');
                }
            };
        },

        editApplication: function (e) {
            router.routeTo(router.configuration.routes.editApp, {args: [this.getAppName(e)], trigger: true});
        },

        viewPolicies: function (e) {
            router.routeTo(router.configuration.routes.managePolicies, {args: [this.getAppName(e)], trigger: true});
        },

        getAppName: function (e) {
            return this.appGridView.grid.getRowData(this.appGridView.getSelectedRowId(e)).name;
        },

        getDefaultFilter: function () {
            var exceptions = '',
                defaultApplications,
                returnList = [];

            if (conf.globalData.policyEditor) {
                defaultApplications = conf.globalData.policyEditor.defaultApplications;
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

        deleteApplications: function (e) {
            e.preventDefault();

            if ($(e.target).hasClass('inactive')) {
                return;
            }

            var self = this, i, promises = [];

            for (i = 0; i < this.appGridView.selectedItems.length; i++) {
                promises.push(policyDelegate.deleteApplication(self.appGridView.selectedItems[i]));
            }
            this.appGridView.deleteItems(e, promises);
        },

        startImportPolicies: function () {
            // Triggering the click on the hidden input with type "file" to upload the file
            this.$el.find("#realImport").trigger("click");
        },

        importPolicies: function (e) {
            policyDelegate.importPolicies(e.target.result)
                .done(function () {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policiesUploaded");
                })
                .fail(function () {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "policiesUploadFailed");
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
            this.$el.find("#exportPolicies").attr('href', constants.host + "/" + constants.context + "/xacml"  + this.subrealm + "/policies");
        }

    });

    return new ManageApplicationsView();
});
