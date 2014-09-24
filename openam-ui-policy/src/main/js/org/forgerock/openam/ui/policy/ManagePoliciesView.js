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
 * @author Aleanora Kaladzinskaya
 * @author Eugenia Sergueeva
 */

/*global window, define, $, _, document, console, sessionStorage */

define("org/forgerock/openam/ui/policy/ManagePoliciesView", [
    "org/forgerock/openam/ui/policy/GenericGridView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/policy/PolicyDelegate"
], function (GenericGridView, uiUtils, router, policyDelegate) {
    var ManagePoliciesView = GenericGridView.extend({
        template: "templates/policy/ManagePoliciesTemplate.html",

        events: {
            'click #deleteItems': 'deletePolicies'
        },

        render: function (args, callback) {
            var self = this;

            _.extend(this.data, {appName: args[0]});

            this.initBaseView('templates/policy/PoliciesTableGlobalActionsTemplate.html', 'PE-mng-pols-sel-' + this.data.appName);

            this.parentRender(function () {
                this.setGridButtonSet();

                var options = {
                        url: '/openam/json/policies?_queryFilter=' + encodeURIComponent('applicationName eq "' + this.data.appName + '"'),
                        colNames: ['', 'Name', 'Description', 'Author', 'Created', 'Modified By', 'Last Modified',
                            'Actions', 'Resources', 'Resource Attributes', 'Subject'],
                        colModel: [
                            {name: 'iconChB', width: 40, sortable: false, formatter: self.checkBoxFormatter, frozen: true, title: false},
                            {name: 'name', width: 250, frozen: true},
                            {name: 'description', sortable: false, width: 150},
                            {name: 'createdBy', width: 250, hidden: true},
                            {name: 'creationDate', width: 150, formatter: uiUtils.commonJQGridFormatters.dateFormatter, hidden: true},
                            {name: 'lastModifiedBy', width: 250, hidden: true},
                            {name: 'lastModified', width: 150, formatter: uiUtils.commonJQGridFormatters.dateFormatter, hidden: true},
                            {name: 'actionValues', width: 250, sortable: false, formatter: uiUtils.commonJQGridFormatters.objectFormatter},
                            {name: 'resources', width: 250, sortable: false, formatter: uiUtils.commonJQGridFormatters.arrayFormatter},
                            {name: 'resourceAttributes', width: 150, sortable: false, formatter: uiUtils.commonJQGridFormatters.arrayFormatter, hidden: true},
                            {name: 'subject', width: 150, sortable: false, formatter: uiUtils.commonJQGridFormatters.objectFormatter, hidden: true}
                        ],
                        gridComplete: function () {
                            $(this).jqGrid('hideCol', 'cb');
                        },
                        beforeSelectRow: function (rowId, e) {
                            var checkBoxCellSelected = self.isCheckBoxCellSelected(e);
                            if (!checkBoxCellSelected) {
                                self.editPolicy(e);
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
                        pager: '#policiesPager'
                    },
                    additionalOptions = {
                        columnChooserOptions: {
                            width: 501,
                            height: 230
                        }
                    };

                this.grid = uiUtils.buildRestResponseBasedJQGrid(this, '#managePolicies', options, additionalOptions, callback);

                this.grid.on('jqGridAfterInsertRow', function (e, rowid, rowdata) {
                    self.selectRow(e, rowid, rowdata);
                });

                this.grid.jqGrid('setFrozenColumns');

                this.reloadGlobalActionsTemplate();
            });
        },

        editPolicy: function (e) {
            var policyName = this.grid.getRowData(this.getSelectedRowId(e)).name;

            router.routeTo(router.configuration.routes.editPolicy,
                {args: [this.data.appName, policyName], trigger: true});
        },

        deletePolicies: function (e) {
            e.preventDefault();

            if ($(e.target).hasClass('inactive')) {
                return;
            }

            var self = this, i, promises = [];

            for (i = 0; i < self.selectedItems.length; i++) {
                promises.push(policyDelegate.deletePolicy(self.selectedItems[i]));
            }

            this.deleteItems(e, promises);
        }
    });

    return new ManagePoliciesView();
});
