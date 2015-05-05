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

/**
 * @author Aleanora Kaladzinskaya
 * @author Eugenia Sergueeva
 */

/*global window, define, $, _, document, console, sessionStorage */

define("org/forgerock/openam/ui/policy/policies/PoliciesListView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/policy/common/GenericGridView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/policy/delegates/PolicyDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/RealmHelper"
], function (AbstractView, GenericGridView, UIUtils, Router, PolicyDelegate, EventManager, Constants, Conf, RealmHelper) {
    var PoliciesListView = AbstractView.extend({
        template: "templates/policy/policies/PoliciesListTemplate.html",
        events: {
            'click #deletePolicies': 'deletePolicies',
            'click #deleteRef': 'deleteReferrals',
            'click .nav-tabs li a': 'showTab'
        },
        POLICIES_TAB: 0,
        REFERRALS_TAB: 1,

        render: function (args, callback) {
            var self = this,
                appPromise = PolicyDelegate.getApplicationByName(args[0]);
            this.data.realm = Conf.globalData.auth.realm;
            this.data.appName = args[0];
            this.data.referralsEnabled = Conf.globalData && Conf.globalData.referralsEnabled === "true";

            this.parentRender(function () {
                this.tabs = this.$el.find('.tab-content .tab-pane');
                this.tabLinks = this.$el.find('.nav-tabs li');

                appPromise.done(function(app){
                    var data = { appEditable: app.editable, appName: app.name };
                    UIUtils.fillTemplateWithData("templates/policy/policies/PoliciesListHeaderTemplate.html", data, function(html){
                        self.$el.find('#appNameHeader').html(html);
                    });
                });

                this.policyGridView = new GenericGridView();
                this.policyGridView.render(this.data, {
                    rowUid: 'name',
                    element: '#managePolicies',
                    tpl: 'templates/policy/policies/PoliciesListGridTemplate.html',
                    actionsTpl: 'templates/policy/policies/PoliciesListGridActionsTemplate.html',
                    gridId: 'policies',
                    initOptions: this.getPolicyGridInitOptions(),
                    additionalOptions: this.getPolicyGridAdditionalOptions()
                }, callback);

                if (this.data.referralsEnabled) {
                    this.refGridView = new GenericGridView();
                    this.refGridView.render(this.data, {
                        rowUid: 'name',
                        element: '#manageRefs',
                        tpl: 'templates/policy/referrals/ManageReferralsGridTemplate.html',
                        actionsTpl: 'templates/policy/referrals/ManageReferralsGridActionsTemplate.html',
                        gridId: 'refs',
                        initOptions: this.getRefGridInitOptions(),
                        additionalOptions: this.getRefGridAdditionalOptions()
                    }, callback);

                    if (args[1] === 'referrals') {
                        this.setActiveTab(this.REFERRALS_TAB);
                    }
                } else if (!this.data.referralsEnabled && args[1] === 'referrals') {
                    Router.routeTo(Router.configuration.routes.managePolicies, {args: args, replace: true});
                }
            });
        },

        getPolicyGridInitOptions: function () {
            var self = this,
                datePick = function(elem) { return self.policyGridView.datePicker(self.policyGridView, elem); };

            return {
                url: RealmHelper.decorateURLWithOverrideRealm('/' + Constants.context + '/json/policies'),
                colNames: ['', 'Name', 'Description', 'Author', 'Created', 'Modified By', 'Last Modified', 'Actions', 'Resources', 'Resource Attributes', 'Subject'],
                colModel: [

                    {name: 'iconChB',        width: 40, sortable: false, formatter: this.policyGridView.checkBoxFormatter, frozen: true, title: false, search: false, hidedlg: true},
                    {name: 'name',           width: 285, frozen: true, hidedlg: true},
                    {name: 'description',    width: 285, hidden: true, sortable: false},
                    {name: 'createdBy',      width: 250, hidden: true},
                    {name: 'creationDate',   width: 150, index: 'creationDate', hidden: true, formatter: UIUtils.commonJQGridFormatters.dateFormatter, searchoptions: {searchhidden: true, dataInit: datePick, sopt: ['gt','lt','ge','le','eq']}},
                    {name: 'lastModifiedBy', width: 250, hidden: true},
                    {name: 'lastModifiedDate',   width: 150, index: 'lastModifiedDate', hidden: true, formatter: UIUtils.commonJQGridFormatters.dateFormatter, searchoptions: {searchhidden: true, dataInit: datePick, sopt: ['gt','lt','ge','le','eq']}},
                    {name: 'actionValues',   width: 285, sortable: false, search: false, formatter: UIUtils.commonJQGridFormatters.objectFormatter},
                    {name: 'resources',      width: 285, sortable: false, search: false, formatter: UIUtils.commonJQGridFormatters.arrayFormatter},
                    {name: 'resourceAttributes', width: 285, sortable: false, hidden: true, search: false, formatter: UIUtils.commonJQGridFormatters.arrayFormatter},
                    {name: 'subject',        width: 285, sortable: false, hidden: true, formatter: UIUtils.commonJQGridFormatters.objectFormatter}

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
                    if (UIUtils.responseMessageMatch(xhr.responseText, "Unable to retrieve policy")) {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unableToRetrievePolicy");
                    } else {
                        console.log('loadError', xhr.responseText, status, error);
                    }
                },
                search: true,
                sortname: 'name',
                autowidth: true,
                shrinkToFit: false,
                pager: '#policiesPager'
            };
        },

        getRefGridInitOptions: function () {
            var self = this;
            return {
                url: RealmHelper.decorateURLWithOverrideRealm('/' + Constants.context + '/json/referrals'),
                colNames: ['', 'Name', 'Resources', 'Realms', 'Created', 'Last Modified', 'Created By', 'ModifiedBy'],
                colModel: [
                    {name: 'iconChB',        width: 40,  sortable: false, formatter: this.refGridView.checkBoxFormatter, frozen: true, title: false, search: false, hidedlg: true},
                    {name: 'name',           width: 285, frozen: true, hidedlg: true},
                    {name: 'resources',      width: 285, sortable: false, formatter: this.referralResourceFormatter},
                    {name: 'realms',         width: 285, sortable: false, formatter: UIUtils.commonJQGridFormatters.arrayFormatter},
                    {name: 'creationDate',   width: 150, search: false, hidden: true, formatter: UIUtils.commonJQGridFormatters.dateFormatter},
                    {name: 'lastModified',   width: 150, search: false, hidden: true, formatter: UIUtils.commonJQGridFormatters.dateFormatter},
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
                autowidth: true,
                shrinkToFit: false,
                pager: '#refsPager'
            };
        },

        referralResourceFormatter: function (cellvalue, options, rowObject) {
            var key;
            for (key in cellvalue) {
                if (cellvalue.hasOwnProperty(key)) {
                    return UIUtils.commonJQGridFormatters.arrayFormatter(cellvalue[key], options, rowObject);
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
                apiVersion: 'protocol=1.0,resource=2.0',
                // TODO: completely remove serializeGridData() from here once AME-4925 is ready.
                serializeGridData: function (postedData) {
                    var colNames = _.pluck($(this).jqGrid('getGridParam', 'colModel'), 'name');
                    return self.policyGridView.serializeDataToFilter(postedData, colNames);
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
            this.navigate(Router.configuration.routes.editPolicy,
                [this.data.appName, this.policyGridView.grid.getRowData(this.policyGridView.getSelectedRowId(e)).name]);
        },

        editReferral: function (e) {
            this.navigate(Router.configuration.routes.editReferral,
                [this.data.appName, this.refGridView.grid.getRowData(this.refGridView.getSelectedRowId(e)).name]);
        },

        navigate: function (route, args) {
            Router.routeTo(route, {args: args, trigger: true});
        },

        deletePolicies: function (e) {
            e.preventDefault();

            if ($(e.target).hasClass('inactive')) {
                return;
            }

            var self = this, i, length, promises = [];
            for (i = 0, length = this.policyGridView.selectedItems.length; i < length; i++) {
                promises.push(PolicyDelegate.deletePolicy(self.policyGridView.selectedItems[i]));
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
                promises.push(PolicyDelegate.deleteReferral(self.refGridView.selectedItems[i]));
            }

            this.refGridView.deleteItems(e, promises);
        },

        showTab: function (e) {
            e.preventDefault();

            var index = $(e.currentTarget).parent().index(),
                route = index === this.POLICIES_TAB ? Router.configuration.routes.managePolicies : Router.configuration.routes.manageReferrals;

            this.setActiveTab(index);
            Router.routeTo(route, {args: [this.data.appName], replace: true});
        },

        setActiveTab: function (index) {
            this.tabLinks.not(':eq(' + index + ')').removeClass('active');
            this.tabLinks.eq(index).addClass('active');

            this.tabs.not(':eq(' + index + ')').removeClass('active');
            this.tabs.eq(index).addClass('active');
        }
    });

    return new PoliciesListView();
});
