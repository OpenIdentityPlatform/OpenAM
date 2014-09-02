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

/*global window, define, $, form2js, _, js2form, document, console */

define("org/forgerock/openam/ui/policy/ManageApplicationsView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function (AbstractView, uiUtils) {
    var ManageApplicationsView = AbstractView.extend({
        baseTemplate: "templates/policy/BaseTemplate.html",
        template: "templates/policy/ManageApplicationsTemplate.html",

        render: function (args, callback) {
            var appLinkFormatter = function (cellvalue, options, rowObject) {
                    return '<a href="#app/' + encodeURI(cellvalue) + '">' + cellvalue + '</a>';
                },
                policyLinkFormatter = function (cellvalue, options, rowObject) {
                    return '<a href="#app/' + encodeURI(rowObject.name) + '/policies/" class="icon-search"></a>';
                };

            this.parentRender(function () {
                var options = {
                        url: '/openam/json/applications?_queryFilter=true',
                        colNames: ['Name', 'Description', 'Realm', 'Type', 'Author', 'Created', 'Modified By',
                            'Last Modified', 'Actions', 'Conditions', 'Resources', 'Subjects', 'Override Rule', 'Policies'],
                        colModel: [
                            {name: 'name', width: 250, formatter: appLinkFormatter, frozen: true},
                            {name: 'description', sortable: false, width: 150},
                            {name: 'realm', width: 150},
                            {name: 'applicationType', width: 250},
                            {name: 'createdBy', width: 250},
                            {name: 'creationDate', width: 150, formatter: uiUtils.commonJQGridFormatters.dateFormatter},
                            {name: 'lastModifiedBy', width: 250},
                            {name: 'lastModifiedDate', width: 150, formatter: uiUtils.commonJQGridFormatters.dateFormatter},
                            {name: 'actions', width: 250, sortable: false, formatter: uiUtils.commonJQGridFormatters.objectFormatter},
                            {name: 'conditions', width: 150, sortable: false, formatter: uiUtils.commonJQGridFormatters.arrayFormatter},
                            {name: 'resources', width: 250, sortable: false, formatter: uiUtils.commonJQGridFormatters.arrayFormatter},
                            {name: 'subjects', width: 150, sortable: false, formatter: uiUtils.commonJQGridFormatters.arrayFormatter},
                            {name: 'entitlementCombiner', width: 100},
                            {name: 'policy', width: 60, sortable: false, formatter: policyLinkFormatter}
                        ],
                        sortname: 'name',
                        width: 920,
                        shrinkToFit: false,
                        pager: '#appsPager'

                    },
                    grid = uiUtils.buildRestResponseBasedJQGrid(this, '#manageApps', options, callback);

                grid.jqGrid('setFrozenColumns');
            });
        }
    });

    return new ManageApplicationsView();
});
