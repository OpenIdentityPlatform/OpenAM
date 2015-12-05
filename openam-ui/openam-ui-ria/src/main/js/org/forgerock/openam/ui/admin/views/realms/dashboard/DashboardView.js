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


define("org/forgerock/openam/ui/admin/views/realms/dashboard/DashboardView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/admin/views/realms/CreateUpdateRealmDialog",
    "org/forgerock/openam/ui/admin/views/realms/dashboard/DashboardTasksView",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/admin/delegates/SMSGlobalDelegate",
    "org/forgerock/openam/ui/admin/delegates/SMSRealmDelegate"
], function ($, _, AbstractView, CreateUpdateRealmDialog, DashboardTasksView, Messages, SMSGlobalDelegate,
             SMSRealmDelegate) {
    var DashboardView = AbstractView.extend({
        template: "templates/admin/views/realms/dashboard/DashboardTemplate.html",
        events: {
            "click #editProperties"        : "editProperties"
        },
        editProperties: function (event) {
            event.preventDefault();
            var self = this;
            CreateUpdateRealmDialog.show({
                realmPath : this.data.realmPath,
                callback : function () {
                    self.render([self.data.realmPath]);
                }
            });
        },
        render: function (args, callback) {
            var self = this,
                realmPromise = SMSGlobalDelegate.realms.get(args[0]),
                tasksPromise = SMSRealmDelegate.dashboard.commonTasks.all(args[0]);

            this.data.realmPath = args[0];

            $.when(realmPromise, tasksPromise).then(function (realmData, tasksData) {
                self.data.realm = {
                    status: realmData.values.active ? $.t("common.form.active") : $.t("common.form.inactive"),
                    active: realmData.values.active,
                    aliases: realmData.values.aliases
                };

                self.parentRender(function () {
                    var dashboardTasks = new DashboardTasksView();
                    dashboardTasks.data.allTasks = tasksData[0].result;
                    dashboardTasks.data.taskGroup = { tasks: tasksData[0].result };
                    dashboardTasks.render(args, callback);
                }, callback);
            }, function (errorRealm, errorTasks) {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response: errorRealm ? errorRealm : errorTasks
                });
            });

        }
    });

    return DashboardView;
});
