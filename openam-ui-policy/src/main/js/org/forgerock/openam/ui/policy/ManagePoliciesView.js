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
    "org/forgerock/commons/ui/common/main/AbstractView"
], function (AbstractView) {
    var ManagePoliciesView = AbstractView.extend({
        baseTemplate: "templates/policy/BaseTemplate.html",
        template: "templates/policy/ManagePoliciesTemplate.html",

        render: function (args, callback) {
            var self = this,
                appName = args[0],

                policyLinkFormatter = function (cellvalue, options, rowObject) {
                    return '<a href="#app/' + appName + '/policy/' + cellvalue + '">' + cellvalue + '</a>';
                };

            this.parentRender(function () {
                self.$el.find('#newPolicy').attr("href", "#app/" + appName + "/policy/");
                self.$el.find('#managePoliciesTitle').text("Manage " + appName + " Policies");

                self.$el.find("#managePolicies").jqGrid({
                    url: '/openam/json/policies?_queryFilter=' + encodeURIComponent('applicationName eq "' + appName + '"'),
                    datatype: "json",
                    loadBeforeSend: function (jqXHR) {
                        jqXHR.setRequestHeader('Accept-API-Version', 'protocol=1.0,resource=1.0');
                    },
                    colNames: ['Name', 'Last Modified'],
                    colModel: [
                        {name: 'name', formatter: policyLinkFormatter},
                        {name: 'lastModified'}
                    ],
                    height: 'auto',
                    jsonReader: {
                        root: function (obj) {
                            return obj.result;
                        }
                    },
                    search: null,
                    prmNames: {
                        nd: null,
                        order: null,
                        sort: null,
                        search: null,
                        rows: null, // number of records to fetch
                        page: null // page number
                    },
                    loadComplete: function (data) {
                        _.extend(self.data, data);
                    }
                });

                self.$el.find("#managePolicies").on("jqGridAfterGridComplete", function () {
                    if (callback) {
                        callback();
                    }
                });
            });
        }
    });

    return new ManagePoliciesView();
});
