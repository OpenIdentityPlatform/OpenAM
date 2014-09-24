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

/*global window, define, $, form2js, _, js2form, document, console, sessionStorage */

define("org/forgerock/openam/ui/policy/ManageApplicationsView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/policy/PolicyDelegate"
], function (AbstractView, uiUtils, router, constants, eventManager, conf, policyDelegate) {
    var ManageApplicationsView = AbstractView.extend({
        baseTemplate: "templates/policy/BaseTemplate.html",
        template: "templates/policy/ManageApplicationsTemplate.html",

        events: {
            'click .icon-pencil': 'editApplication',
            'click .icon-file': 'viewPolicies',
            'click #deleteApp': 'deleteApplications'
        },  

        grid: null,
        gridButtonSet: null,
        selectedApps: [],
        storageSelectedAppsKey: 'fr-openam-policy-manage-apps-selected',
        
        render: function (args, callback) {
            var self = this,
                actionsFormatter = function (cellvalue, options, rowObject) {
                    return uiUtils.fillTemplateWithData("templates/policy/ApplicationTableCellActionsTemplate.html",
                        {appName: rowObject.name});
                },
                storedApps = JSON.parse(sessionStorage.getItem(self.storageSelectedAppsKey));

            if (storedApps) {
                self.selectedApps = storedApps;
            }

            this.parentRender(function () {
                self.gridButtonSet = self.$el.find('#appsActions');

                var options = {
                    url: '/openam/json/applications?_queryFilter=true',
                    colNames: ['', 'Name', 'Realm', 'Description', 'Application Base', 'Author', 'Created', 'Last Modified'],
                    colModel: [
                        {name: 'actions', width: 60, sortable: false, formatter: actionsFormatter, frozen: true, title: false},
                        {name: 'name', width: 230, frozen: true},
                        {name: 'realm', width: 150},
                        {name: 'description', width:170, sortable: false},
                        {name: 'resources', width: 250, sortable: false, formatter: uiUtils.commonJQGridFormatters.arrayFormatter},
                        {name: 'createdBy', width: 250, hidden: true},
                        {name: 'creationDate', width: 150, formatter: uiUtils.commonJQGridFormatters.dateFormatter, hidden: true},
                        {name: 'lastModifiedDate', width: 150, formatter: uiUtils.commonJQGridFormatters.dateFormatter, hidden: true}
                    ],
                    onSelectRow: function (rowid, status, e) {
                        if (!$(e.target).is('[class*="icon"]')) {
                            if (status) {
                                self.selectedApps.push(self.data.result[rowid - 1].name);
                            } else {
                                self.selectedApps = _.without(self.selectedApps, self.data.result[rowid - 1].name);
                            }

                            sessionStorage.setItem(self.storageSelectedAppsKey, JSON.stringify(self.selectedApps));
                            self.reloadGlobalTableActionsTemplate();
                        }
                    },
                    multiselect: true,
                    sortname: 'name',
                    width: 920,
                    shrinkToFit: false,
                    pager: '#appsPager'
                },


                additionalOptions = {
                    columnChooserOptions: {
                        width: 501,
                        height: 180
                    },
                    preProcessing: function(data){
                        var defaultApplicatons = conf.globalData.policyEditorConfig.defaultApplicatons,
                            difference = _.difference(defaultApplicatons.defaultApplicatonList, defaultApplicatons.config.exceptThese);
                        if ( defaultApplicatons.config.hideByDefault ){
                            data.result = _.removeByValues(data.result, 'name', difference);
                        } else {
                            data.result = _.findByValues(data.result, 'name', difference); 
                        }
       
                    }
                };

                self.grid = uiUtils.buildRestResponseBasedJQGrid(this, '#manageApps', options, additionalOptions, callback);

                self.grid.on('jqGridAfterInsertRow', function (e, rowid, rowdata) {
                    if (self.selectedApps) {
                        if (self.selectedApps.indexOf(rowdata.name) !== -1) {
                            self.grid.jqGrid('setSelection', rowid, false);
                        }
                    }
                });

                self.grid.jqGrid('setFrozenColumns');
                self.reloadGlobalTableActionsTemplate();
            });
        },

        editApplication: function (e) {
            router.routeTo(router.configuration.routes.editApp,
                {args: [e.target.getAttribute('data-app-name')], trigger: true});
        },

        viewPolicies: function (e) {
            router.routeTo(router.configuration.routes.managePolicies,
                {args: [e.target.getAttribute('data-app-name')], trigger: true});
        },

        deleteApplications: function (e) {
            e.preventDefault();

            var self = this, i, promises = [];

            if ($(e.target).hasClass('inactive')) {
                return;
            }

            for (i = 0; i < this.selectedApps.length; i++) {
                promises.push(policyDelegate.deleteApplication(this.selectedApps[i]));
            }

            $.when.apply($, promises).then(function deleteSuccessClb() {
                self.handleAppsDelete('deleteApplicationsSuccess');
            }, function deleteFailClb() {
                self.handleAppsDelete('deleteApplicationsFail');
            });
        },

        handleAppsDelete: function (message) {
            sessionStorage.removeItem(this.storageSelectedAppsKey);
            this.selectedApps = [];

            this.reloadGlobalTableActionsTemplate();

            this.grid.trigger('reloadGrid');
            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, message);
        },

        reloadGlobalTableActionsTemplate: function () {
            this.gridButtonSet.html(uiUtils.fillTemplateWithData("templates/policy/ApplicationTableGlobalActionsTemplate.html",
                {appNumber: this.selectedApps.length}));
        }
    });

    return new ManageApplicationsView();
})
;