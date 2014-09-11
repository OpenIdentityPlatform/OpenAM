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
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Router"
], function (AbstractView, uiUtils, router) {
    var ManageApplicationsView = AbstractView.extend({
        baseTemplate: "templates/policy/BaseTemplate.html",
        template: "templates/policy/ManageApplicationsTemplate.html",

        events: {
            'click .icon-pencil': 'editApplication',
            'click .icon-file': 'viewPolicies'
        },

        render: function (args, callback) {
            var self = this,
                actionsFormatter = function (cellvalue, options, rowObject) {
                    return uiUtils.fillTemplateWithData("templates/policy/ApplicationTableActionsTemplate.html",
                        {appName: encodeURI(rowObject.name)});
                };

            this.parentRender(function () {
                var options = {
                        url: '/openam/json/applications?_queryFilter=true',
                        colNames: ['', 'Name', 'Description', 'Realm', 'Resources', 'Author', 'Created', 'Last Modified'],
                        colModel: [
                            {name: 'actions', width: 60, sortable: false, formatter: actionsFormatter, frozen: true, title: false},
                            {name: 'name', width: 230, frozen: true},
                            {name: 'description', sortable: false},
                            {name: 'realm', width: 150},
                            {name: 'resources', width: 250, sortable: false, formatter: uiUtils.commonJQGridFormatters.arrayFormatter},
                            {name: 'createdBy', width: 250},
                            {name: 'creationDate', width: 150, formatter: uiUtils.commonJQGridFormatters.dateFormatter},
                            {name: 'lastModifiedDate', width: 150, formatter: uiUtils.commonJQGridFormatters.dateFormatter}
                        ],
                        multiselect: true,
                        sortname: 'name',
                        width: 920,
                        shrinkToFit: false,
                        pager: '#appsPager'
                    },
                    columnChooserOptions = {
                        width: 501,
                        height: 300
                    },
                    grid = uiUtils.buildRestResponseBasedJQGrid(this, '#manageApps', options, columnChooserOptions, callback);

                grid.jqGrid('setFrozenColumns');
            });
        },

        editApplication: function (e) {
            router.routeTo(router.configuration.routes.editApp,
                {args: [e.target.getAttribute('data-app-name')], trigger: true});
        },

        viewPolicies: function (e) {
            router.routeTo(router.configuration.routes.managePolicies,
                {args: [e.target.getAttribute('data-app-name')], trigger: true});
        }
    });

    return new ManageApplicationsView();
});
