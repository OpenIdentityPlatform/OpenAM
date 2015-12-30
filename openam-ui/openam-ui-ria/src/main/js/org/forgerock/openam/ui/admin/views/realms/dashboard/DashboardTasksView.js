/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */


define("org/forgerock/openam/ui/admin/views/realms/dashboard/DashboardTasksView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/admin/utils/RedirectToLegacyConsole"
], function ($, _, AbstractView, RedirectToLegacyConsole) {
    var DashboardTasksView = AbstractView.extend({
        template: "templates/admin/views/realms/dashboard/DashboardTasksTemplate.html",
        data: {},
        element: "#commonTasks",
        events:{
            "click .am-panel-card a" : "cardClick",
            "click span#commonTasksBtn" : "commonTasksBtnClick"
        },
        render: function (args, callback) {
            this.realmPath = args[0];
            this.parentRender(callback);
        },

        cardClick: function (e) {
            var dataset = $(e.currentTarget).data();
            if (!dataset.taskLink || dataset.taskLink.indexOf("http") !== 0) {
                e.preventDefault();
                if (dataset.taskGroup) {
                    this.data.taskGroup = _.find(this.data.allTasks, { _id: dataset.taskGroup });
                    this.parentRender();
                } else {
                    RedirectToLegacyConsole.commonTasks(this.realmPath, dataset.taskLink);
                }
            }
        },

        commonTasksBtnClick: function (e) {
            e.preventDefault();
            this.data.taskGroup = {};
            this.data.taskGroup.tasks = this.data.allTasks;
            this.parentRender();
        }
    });

    return DashboardTasksView;
});
