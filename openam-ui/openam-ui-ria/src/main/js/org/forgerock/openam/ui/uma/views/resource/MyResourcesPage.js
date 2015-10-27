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

define("org/forgerock/openam/ui/uma/views/resource/MyResourcesPage", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/openam/ui/uma/views/resource/BasePage",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openam/ui/uma/delegates/UMADelegate"
], function ($, _, BootstrapDialog, BasePage, Configuration, Constants, EventManager, UMADelegate) {
    var MyResourcesPage = BasePage.extend({
        template: "templates/uma/views/resource/MyResourcesPageTemplate.html",
        partials: [
            "templates/uma/views/resource/_UnshareAllResourcesButton.html"
        ],
        events: {
            "click button#unshareAllResources": "unshareAllResources"
        },
        recordsPresent: function () {
            this.$el.find("button#unshareAllResources").prop("disabled", false);
        },
        render: function (args, callback) {
            var self = this,
                labelId = args[1],
                topLevel = args[1] === "";

            this.data.topLevel = topLevel;

            if (topLevel) {
                this.renderGrid(this.createSetCollection(),
                                this.createColumns("myresources/all"),
                                callback);
            } else {
                // Resolve label ID to name
                UMADelegate.labels.get(labelId).done(function (data) {
                    var columns = self.createColumns("myresources/" + encodeURIComponent(data.id));

                    // Splice out the "Hosts" column
                    columns.splice(1, 1);

                    self.data.labelName = data.name;

                    self.renderGrid(self.createLabelCollection(labelId),
                                    columns,
                                    callback);
                }).fail(function () {
                    //
                });
            }
        },
        unshareAllResources: function () {
            var buttons = [{
                label: $.t("common.form.cancel"),
                action: function (dialog) {
                    dialog.close();
                }
            }, {
                id: "ok",
                label: $.t("common.form.ok"),
                cssClass: "btn-primary btn-danger",
                action: function (dialog) {
                    dialog.enableButtons(false);
                    dialog.getButton("ok").text($.t("common.form.working"));

                    UMADelegate.unshareAllResources().done(function () {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unshareAllResourcesSuccess");

                        dialog.close();
                    }).fail(function () {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unshareAllResourcesFail");

                        dialog.enableButtons(true);
                        dialog.getButton("ok").text($.t("common.form.ok"));
                    });
                }
            }];

            BootstrapDialog.show({
                type: BootstrapDialog.TYPE_DANGER,
                title: $.t("uma.resources.myresources.unshareAllResources.dialog.title"),
                message: $.t("uma.resources.myresources.unshareAllResources.dialog.message"),
                closable: false,
                buttons: buttons
            });
        }
    });

    return MyResourcesPage;
});
