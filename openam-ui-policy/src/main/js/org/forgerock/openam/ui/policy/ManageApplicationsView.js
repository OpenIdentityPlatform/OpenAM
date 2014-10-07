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

/*global window, define, $, _, document, console, sessionStorage */

define("org/forgerock/openam/ui/policy/ManageApplicationsView", [
    "org/forgerock/openam/ui/policy/GenericGridView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openam/ui/policy/PolicyDelegate"
], function (GenericGridView, uiUtils, router, constants, conf, eventManager, policyDelegate) {
    var ManageApplicationsView = GenericGridView.extend({
        template: "templates/policy/ManageApplicationsTemplate.html",

        events: {
            'click .icon-pencil': 'editApplication',
            'click .icon-file': 'viewPolicies',
            'click #deleteItems': 'deleteApplications'
        },

        render: function (args, callback) {
            var self = this,
                actionsFormatter = function (cellvalue, options, rowObject) {
                    return uiUtils.fillTemplateWithData("templates/policy/ApplicationTableCellActionsTemplate.html");
                };

            this.initBaseView('templates/policy/ApplicationTableGlobalActionsTemplate.html', 'PE-mng-apps-sel');

            this.parentRender(function () {
                var subrealm = "",
                    options,
                    additionalOptions;

                if (conf.globalData.auth.realm !== "/") {
                    subrealm = conf.globalData.auth.realm;
                }

                this.setGridButtonSet();

                options = {
                        url: '/openam/json' + subrealm + '/applications?_queryFilter=true',
                        colNames: ['', '', 'Name', 'Realm', 'Description', 'Application Base', 'Author', 'Created', 'Last Modified'],
                        colModel: [
                            {name: 'iconChB', width: 40, sortable: false, formatter: self.checkBoxFormatter, frozen: true, title: false},
                            {name: 'actions', width: 60, sortable: false, formatter: actionsFormatter, frozen: true, title: false},
                            {name: 'name', width: 230, frozen: true},
                            {name: 'realm', width: 150},
                            {name: 'description', width: 170, sortable: false},
                            {name: 'resources', width: 240, sortable: false, formatter: uiUtils.commonJQGridFormatters.arrayFormatter},
                            {name: 'createdBy', width: 250, hidden: true},
                            {name: 'creationDate', width: 150, formatter: uiUtils.commonJQGridFormatters.dateFormatter, hidden: true},
                            {name: 'lastModifiedDate', width: 150, formatter: uiUtils.commonJQGridFormatters.dateFormatter, hidden: true}
                        ],
                        gridComplete: function () {
                            $(this).jqGrid('hideCol', 'cb');
                        },
                        beforeSelectRow: function (rowId, e) {
                            var checkBoxCellSelected = self.isCheckBoxCellSelected(e);
                            if (!checkBoxCellSelected && !$(e.target).hasClass('icon-pencil')) {
                                self.viewPolicies(e);
                            }

                            return checkBoxCellSelected;
                        },
                        onSelectRow: function (rowid, status, e) {
                            self.onRowSelect(rowid, status, e);
                        },
                        multiselect: true,
                        sortname: 'name',
                        width: 920,
                        shrinkToFit: false,
                        pager: '#appsPager'
                    };

                additionalOptions = {
                        columnChooserOptions: {
                            width: 501,
                            height: 180
                        },
                        storageKey: constants.OPENAM_STORAGE_KEY_PREFIX + 'PE-mng-apps-sel-col'
                    };

                this.grid = uiUtils.buildRestResponseBasedJQGrid(this, '#manageApps', options, additionalOptions, callback);

                this.grid.on('jqGridAfterInsertRow', function (e, rowid, rowdata) {
                    self.selectRow(e, rowid, rowdata);
                });

                this.grid.jqGrid('setFrozenColumns');

                this.reloadGlobalActionsTemplate();
            });
        },

        editApplication: function (e) {
            router.routeTo(router.configuration.routes.editApp, {args: [this.getAppName(e)], trigger: true});
        },

        viewPolicies: function (e) {
            router.routeTo(router.configuration.routes.managePolicies, {args: [this.getAppName(e)], trigger: true});
        },

        getAppName: function (e) {
            return this.grid.getRowData(this.getSelectedRowId(e)).name;
        },

        deleteApplications: function (e) {
            e.preventDefault();

            if ($(e.target).hasClass('inactive')) {
                return;
            }

            var self = this, i, promises = [];

            for (i = 0; i < self.selectedItems.length; i++) {
                promises.push(policyDelegate.deleteApplication(self.selectedItems[i]));
            }
            this.deleteItems(e, promises);
        }
    });

    return new ManageApplicationsView();
})
;