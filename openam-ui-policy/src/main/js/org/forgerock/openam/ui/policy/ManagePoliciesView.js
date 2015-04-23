/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
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

/*global window, define, $, _, document, console, sessionStorage */

define("org/forgerock/openam/ui/policy/ManagePoliciesView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/policy/GenericGridView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/policy/PolicyDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function (AbstractView, GenericGridView, uiUtils, router, policyDelegate, eventManager, constants, conf, RealmHelper) {
    var ManagePoliciesView = AbstractView.extend({
        baseTemplate: 'templates/policy/BaseTemplate.html',
        template: "templates/policy/ManagePoliciesTemplate.html",

        events: {
            'click #deletePolicies': 'deletePolicies',
            'click #deleteRef': 'deleteReferrals',
            'click .tab-links a': 'showTab'
        },

        render: function (args, callback) {
            this.data.realm = conf.globalData.auth.realm;
            this.data.appName = decodeURI(args[0]);
            this.data.referralsEnabled = conf.globalData.serverInfo && conf.globalData.serverInfo.referralsEnabled === "true";

            this.parentRender(function () {
                this.policyGridView = new GenericGridView();
                this.policyGridView.render({
                    element: '#managePolicies',
                    tpl: 'templates/policy/ManagePoliciesGridTemplate.html',
                    actionsTpl: 'templates/policy/ManagePoliciesGridActionsTemplate.html',
                    gridId: 'policies',
                    initOptions: this.getPolicyGridInitOptions(),
                    additionalOptions: this.getPolicyGridAdditionalOptions()
                }, callback);

                if (this.data.referralsEnabled) {
                    this.refGridView = new GenericGridView();
                    this.refGridView.render({
                        element: '#manageRefs',
                        tpl: 'templates/policy/ManageRefsGridTemplate.html',
                        actionsTpl: 'templates/policy/ManageRefsGridActionsTemplate.html',
                        gridId: 'refs',
                        initOptions: this.getRefGridInitOptions(),
                        additionalOptions: this.getRefGridAdditionalOptions()
                    }, callback);
                }
            });
        },

        getPolicyGridInitOptions: function () {
            var self = this;
            return {
                url: RealmHelper.decorateURLWithOverrideRealm('/' + constants.context + '/json/policies'),
                colNames: ['', 'Name', 'Description', 'Author', 'Created', 'Modified By', 'Last Modified', 'Actions', 'Resources', 'Resource Attributes', 'Subject'],
                colModel: [

                    {name: 'iconChB',        width: 40, sortable: false, formatter: this.policyGridView.checkBoxFormatter, frozen: true, title: false, search: false, hidedlg: true},
                    {name: 'name',           width: 285, frozen: true, hidedlg: true},
                    {name: 'description',    width: 285, hidden: true, sortable: false},
                    {name: 'createdBy',      width: 250, hidden: true},
                    {name: 'creationDate',   width: 150, search: false, hidden: true, formatter: uiUtils.commonJQGridFormatters.dateFormatter},
                    {name: 'lastModifiedBy', width: 250, hidden: true},
                    {name: 'lastModified',   width: 150, search: false, hidden: true, formatter: uiUtils.commonJQGridFormatters.dateFormatter},
                    {name: 'actionValues',   width: 285, sortable: false, search: false, formatter: uiUtils.commonJQGridFormatters.objectFormatter},
                    {name: 'resources',      width: 285, sortable: false, search: false, formatter: uiUtils.commonJQGridFormatters.arrayFormatter},
                    {name: 'resourceAttributes', width: 285, sortable: false, hidden: true, search: false, formatter: uiUtils.commonJQGridFormatters.arrayFormatter},
                    {name: 'subject',        width: 285, sortable: false, hidden: true, formatter: uiUtils.commonJQGridFormatters.objectFormatter}

                ],
                beforeSelectRow: function (rowId, e) {
                    var checkBoxCellSelected = self.policyGridView.isCheckBoxCellSelected(e);
                    if (!checkBoxCellSelected) {
                        self.editPolicy(e);
                    }
                    return checkBoxCellSelected;
                },
                onSelectRow: function (rowid, status, e) {
                    self.policyGridView.onRowSelect(rowid, status, e);
                },
                loadError: function (xhr, status, error) {
                    if (uiUtils.responseMessageMatch(xhr.responseText, "Unable to retrieve policy")) {
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unableToRetrievePolicy");
                    } else {
                        console.log('loadError', xhr.responseText, status, error);
                    }
                },
                sortname: 'name',
                width: 915,
                shrinkToFit: false,
                pager: '#policiesPager'
            };
        },

        getRefGridInitOptions: function () {
            var self = this;
            return {
                url: RealmHelper.decorateURLWithOverrideRealm('/' + constants.context + '/json/referrals'),
                colNames: ['', 'Name', 'Resources', 'Realms', 'Created', 'Last Modified', 'Created By', 'ModifiedBy'],
                colModel: [
                    {name: 'iconChB',        width: 40,  sortable: false, formatter: this.refGridView.checkBoxFormatter, frozen: true, title: false, search: false, hidedlg: true},
                    {name: 'name',           width: 285, frozen: true, hidedlg: true},
                    {name: 'resources',      width: 285, sortable: false, formatter: this.referralResourceFormatter},
                    {name: 'realms',         width: 285, sortable: false, formatter: uiUtils.commonJQGridFormatters.arrayFormatter},
                    {name: 'creationDate',   width: 150, search: false, hidden: true, formatter: uiUtils.commonJQGridFormatters.dateFormatter},
                    {name: 'lastModified',   width: 150, search: false, hidden: true, formatter: uiUtils.commonJQGridFormatters.dateFormatter},
                    {name: 'createdBy',      width: 250, hidden: true},
                    {name: 'lastModifiedBy', width: 250, hidden: true}
                ],
                beforeSelectRow: function (rowId, e) {
                    var checkBoxCellSelected = self.refGridView.isCheckBoxCellSelected(e);
                    if (!checkBoxCellSelected) {
                        self.editReferral(e);
                    }
                    return checkBoxCellSelected;
                },
                onSelectRow: function (rowid, status, e) {
                    self.refGridView.onRowSelect(rowid, status, e);
                },
                sortname: 'name',
                width: 915,
                shrinkToFit: false,
                pager: '#refsPager'
            };
        },

        referralResourceFormatter: function (cellvalue, options, rowObject) {
            var key;
            for (key in cellvalue) {
                if (cellvalue.hasOwnProperty(key)) {
                    return uiUtils.commonJQGridFormatters.arrayFormatter(cellvalue[key], options, rowObject);
                }
            }
        },

        getPolicyGridAdditionalOptions: function () {
            var self = this;
            return {
                search: true,
                searchFilter: [
                    {field: 'applicationName', op: 'eq', val: this.data.appName}
                ],
                columnChooserOptions: {
                    width: 501,
                    height: 230
                },
                storageKey: 'PE-mng-pols-sel-' + this.data.appName,
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
                    self.policyGridView.grid.on('jqGridAfterInsertRow', function (e, rowid, rowdata) {
                        self.policyGridView.selectRow(e, rowid, rowdata);
                    });
                    self.policyGridView.grid.jqGrid('setFrozenColumns');
                }
            };
        },

        getRefGridAdditionalOptions: function () {
            var self = this;
            return {
                search: true,
                searchFilter: [
                    {field: 'applicationName', op: 'eq', val: this.data.appName}
                ],
                columnChooserOptions: {
                    width: 501,
                    height: 230
                },
                storageKey: 'PE-mng-ref-sel-' + this.data.appName,
                callback: function () {
                    self.refGridView.grid.on('jqGridAfterInsertRow', function (e, rowid, rowdata) {
                        self.refGridView.selectRow(e, rowid, rowdata);
                    });
                    self.refGridView.grid.jqGrid('setFrozenColumns');
                }
            };
        },

        editPolicy: function (e) {
            this.navigate(router.configuration.routes.editPolicy,
                [this.data.appName, this.policyGridView.grid.getRowData(this.policyGridView.getSelectedRowId(e)).name]);
        },

        editReferral: function (e) {
            this.navigate(router.configuration.routes.editReferral,
                [this.data.appName, this.refGridView.grid.getRowData(this.refGridView.getSelectedRowId(e)).name]);
        },

        navigate: function (route, args) {
            router.routeTo(route, {args: args, trigger: true});
        },

        deletePolicies: function (e) {
            e.preventDefault();

            if ($(e.target).hasClass('inactive')) {
                return;
            }

            var self = this, i, length, promises = [];
            for (i = 0, length = this.policyGridView.selectedItems.length; i < length; i++) {
                promises.push(policyDelegate.deletePolicy(self.policyGridView.selectedItems[i]));
            }

            this.policyGridView.deleteItems(e, promises);
        },

        deleteReferrals: function (e) {
            e.preventDefault();

            if ($(e.target).hasClass('inactive')) {
                return;
            }

            var self = this, i, length, promises = [];
            for (i = 0, length = this.refGridView.selectedItems.length; i < length; i++) {
                promises.push(policyDelegate.deleteReferral(self.refGridView.selectedItems[i]));
            }

            this.refGridView.deleteItems(e, promises);
        },

        showTab: function (e) {
            e.preventDefault();

            var index = $(e.currentTarget).parent().index(),
                tabs =  this.$el.find('.tab-content .tab'),
                tabLinks = this.$el.find('.tab-links li');

            tabLinks.not(':eq('+ index +')').removeClass('active-tab');
            tabLinks.eq(index).addClass('active-tab');

            tabs.not(':eq('+ index +')').addClass('inactive-tab');
            tabs.eq(index).removeClass('inactive-tab');
        }
    });

    return new ManagePoliciesView();
});
