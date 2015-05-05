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

/**
 * @author Eugenia Sergueeva
 * @author Pavel Shapovalov
 */

/*global define, $, _ */

define("org/forgerock/openam/ui/policy/resourcetypes/ResourceTypesListView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/policy/common/GenericGridView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/policy/delegates/PolicyDelegate"
], function (AbstractView, GenericGridView, UIUtils, Router, Constants, Configuration, PolicyDelegate) {
    var ResourceTypesListView = AbstractView.extend({
        template: "templates/policy/resourcetypes/ResourceTypesListTemplate.html",
        events: {
            'click #deleteResTypes': 'deleteResourceTypes'
        },

        render: function (args, callback) {
            this.data.realm = Configuration.globalData.auth.realm;

            this.parentRender(function () {
                this.subrealm = this.data.realm !== "/" ? this.data.realm : "";
                this.resTypesGridView = new GenericGridView();
                this.resTypesGridView.render(this.data, {
                    rowUid: 'uuid',
                    element: '#manageResTypes',
                    tpl: 'templates/policy/resourcetypes/ResourceTypesListGridTemplate.html',
                    actionsTpl: 'templates/policy/resourcetypes/ResourceTypesListGridActionsTemplate.html',
                    gridId: 'resTypes',
                    initOptions: this.getGridInitOptions(),
                    additionalOptions: this.getGridAdditionalOptions()
                }, callback);
            });
        },

        getGridInitOptions: function () {
            var self = this;

            return {
                url: '/' + Constants.context + '/json' + this.subrealm + '/resourcetypes',
                colNames: ['', 'Name', 'Description', 'Level', 'Pattern', 'Actions', 'uuid'],
                colModel: [
                    {name: 'iconChB', width: 40, sortable: false, formatter: this.resTypesGridView.checkBoxFormatter, frozen: true, title: false, search: false, hidedlg: true},
                    {name: 'name', width: 240, frozen: true, hidedlg: true},
                    {name: 'description', width: 232, sortable: false},
                    {name: 'level', width: 240, hidden: true},
                    {name: 'patterns', width: 231, sortable: false, formatter: UIUtils.commonJQGridFormatters.arrayFormatter},
                    {name: 'actions', width: 150, sortable: false, search: false, formatter: UIUtils.commonJQGridFormatters.objectFormatter},
                    {name: 'uuid', hidden: true, hidedlg: true}
                ],
                beforeSelectRow: function (rowId, e) {
                    var checkBoxCellSelected = self.resTypesGridView.isCheckBoxCellSelected(e);
                    if (!checkBoxCellSelected) {
                        self.editResourceType(e);
                    }
                    return checkBoxCellSelected;
                },
                onSelectRow: function (rowid, status, e) {
                    self.resTypesGridView.onRowSelect(rowid, status, e);
                },
                sortname: 'name',
                autowidth: true,
                shrinkToFit: false,
                pager: '#resTypesPager'
            };
        },

        getGridAdditionalOptions: function () {
            var self = this;
            return {
                search: true,
                columnChooserOptions: {
                    width: 501,
                    height: 230
                },
                storageKey: 'PE-mng-restypes-sel-' + this.data.realm,
                // TODO: completely remove serializeGridData() from here once AME-4925 is ready.
                serializeGridData: function (postedData) {
                    var colNames = _.pluck($(this).jqGrid('getGridParam', 'colModel'), 'name');
                    return self.resTypesGridView.serializeDataToFilter(postedData, colNames);
                },
                callback: function () {
                    self.resTypesGridView.grid.on('jqGridAfterInsertRow', function (e, rowid, rowdata) {
                        self.resTypesGridView.selectRow(e, rowid, rowdata);
                    });
                    self.resTypesGridView.grid.jqGrid('setFrozenColumns');
                }
            };
        },

        editResourceType: function (e) {
            var uuid = this.resTypesGridView.data.resTypes.result[this.resTypesGridView.getSelectedRowId(e) - 1].uuid;
            Router.routeTo(Router.configuration.routes.editResourceType, {args: [uuid], trigger: true});
        },

        deleteResourceTypes: function (e) {
            e.preventDefault();

            if ($(e.target).hasClass('inactive')) {
                return;
            }

            var self = this, i, length, promises = [];
            for (i = 0, length = this.resTypesGridView.selectedItems.length; i < length; i++) {
                promises.push(PolicyDelegate.deleteResourceType(self.resTypesGridView.selectedItems[i]));
            }

            this.resTypesGridView.deleteItems(e, promises);
        }
    });

    return new ResourceTypesListView();
});