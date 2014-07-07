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
    "org/forgerock/openam/ui/policy/ListView",
    "org/forgerock/openam/ui/policy/PolicyDelegate"
], function (AbstractView, listView, policyDelegate) {
    var ManageApplicationsView = AbstractView.extend({
        baseTemplate: "templates/policy/BaseTemplate.html",
        template: "templates/policy/ManageApplicationsTemplate.html",

        render: function (args, callback) {
            var self = this;

            this.parentRender(function () {
                policyDelegate.getAllApplications().done(function (data) {
                    self.listApplications(data, callback, self.$el.find('#manageApps'));
                });
            });
        },

        listApplications: function (data, callback, element) {
            listView.render(data, callback, element, "templates/policy/ListApplicationsTemplate.html");
        }
    });

    return new ManageApplicationsView();
});
