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
 */

/*global window, define, $, form2js, _, js2form, document, console */

define("org/forgerock/openam/ui/policy/ManagePoliciesView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/policy/ListView",
    "org/forgerock/openam/ui/policy/PolicyDelegate"
], function (AbstractView, uiUtils, listView, policyDelegate) {
    var ManagePoliciesView = AbstractView.extend({
        baseTemplate: "templates/policy/BaseTemplate.html",
        template: "templates/policy/ManagePoliciesTemplate.html",

        render: function (args, callback) {

            var appName = uiUtils.getCurrentHash().split('/', 2)[1],
                self = this;

            this.parentRender(function () {
                self.$el.find('#newPolicy').attr("href", "#app/" + appName + "/policy/");
                self.$el.find('#managePoliciesTitle').text("Manage " + appName + " Policies");

                policyDelegate.getApplicationPolicies(appName).done(function (data) {
                    self.listPolicies(data, callback, self.$el.find('#managePolicies'));
                });
            });
        },

        listPolicies: function (data, callback, element) {
            listView.render(data, callback, element, "templates/policy/ListPoliciesTemplate.html");
        }
    });

    return new ManagePoliciesView();
});
