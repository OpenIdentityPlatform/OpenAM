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

/*global window, define, $, form2js, _, js2form, document, console */

define("org/forgerock/openam/ui/policy/ManagePoliciesView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function (AbstractView, uiUtils) {
    var ManagePoliciesView = AbstractView.extend({
        baseTemplate: "templates/policy/BaseTemplate.html",
        template: "templates/policy/ManagePoliciesTemplate.html",

        render: function (args, callback) {
            var appName = decodeURI(args[0]),
                policyLinkFormatter = function (cellvalue, options, rowObject) {
                    var url = '#app/' + appName + '/policy/' + cellvalue,
                        encodedUrl = encodeURI(url);
                    return '<a href="' + encodedUrl + '">' + cellvalue + '</a>';
                };

            this.parentRender(function () {
                this.$el.find('#newPolicy').attr("href", "#app/" + appName + "/policy/");
                this.$el.find('#managePoliciesTitle').text("Manage " + appName + " Policies");

                var options = {
                        url: '/openam/json/policies?_queryFilter=' + encodeURIComponent('applicationName eq "' + appName + '"'),
                        colNames: ['Name', 'Description', 'Author', 'Created', 'Modified By', 'Last Modified', 'Actions',
                            'Resources', 'Resource Attributes', 'Subject'],
                        colModel: [
                            {name: 'name', width: 250, frozen: true, formatter: policyLinkFormatter},
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
                    },
                    
                    grid = uiUtils.buildRestResponseBasedJQGrid(this, '#managePolicies', options, additionalOptions, callback);

                grid.jqGrid('setFrozenColumns');
            });
        }
    });

    return new ManagePoliciesView();
});
