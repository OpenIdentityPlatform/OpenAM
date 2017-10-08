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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/admin/utils/RedirectToLegacyConsole"
], function ($, _, AbstractView, RedirectToLegacyConsole) {
    var DashboardTasksView = AbstractView.extend({
        template: "templates/admin/views/realms/dashboard/DashboardTasksTemplate.html",
        data: {},
        element: "[data-common-tasks-container]",
        events: {
            "click [data-panel-card] a" : "cardClick",
            "click [data-common-tasks]" : "commonTasksBtnClick"
        },
        render (args, callback) {
            this.realmPath = args[0];
            this.parentRender(callback);
        },

        cardClick (e) {
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

        commonTasksBtnClick (e) {
            e.preventDefault();
            this.data.taskGroup = {};
            this.data.taskGroup.tasks = this.data.allTasks;
            this.parentRender();
        }
    });

    return DashboardTasksView;
});
