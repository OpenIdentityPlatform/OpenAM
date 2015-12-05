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

define("org/forgerock/openam/ui/uma/views/resource/MyLabelsPage", [
    "jquery",
    "backbone",
    "org/forgerock/commons/ui/common/backgrid/Backgrid",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "org/forgerock/openam/ui/uma/views/resource/BasePage",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/common/util/RealmHelper",
    "org/forgerock/openam/ui/uma/delegates/UMADelegate"
], function ($, Backbone, Backgrid, BackgridUtils, BasePage, BootstrapDialog, Configuration, Constants, EventManager,
             Router, RealmHelper, UMADelegate) {
    var MyLabelsPage = BasePage.extend({
        template: "templates/uma/views/resource/MyLabelsPageTemplate.html",
        partials: [
            "templates/uma/views/resource/_DeleteLabelButton.html"
        ],
        events: {
            "click button#deleteLabel": "deleteLabel"
        },
        deleteLabel: function () {
            var self = this,
                buttons = [{
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

                        UMADelegate.labels.remove(self.data.label._id).done(function () {
                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteLabelSuccess");

                            dialog.close();
                            Router.routeTo(Router.configuration.routes.umaResourcesMyResources, {
                                trigger: true,
                                args: []
                            });
                        }).fail(function () {
                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteLabelFail");

                            dialog.enableButtons(true);
                            dialog.getButton("ok").text($.t("common.form.ok"));
                        });
                    }
                }];

            BootstrapDialog.show({
                type: BootstrapDialog.TYPE_DANGER,
                title: $.t("uma.resources.myLabels.deleteLabel.dialog.title"),
                message: $.t("uma.resources.myLabels.deleteLabel.dialog.message"),
                closable: false,
                buttons: buttons
            });
        },
        recordsPresent: function () {
            this.$el.find("button#deleteLabel").prop("disabled", false);
        },
        render: function (args, callback) {
            var labelId = args[0],
                self = this;

            UMADelegate.labels.get(labelId).then(function (result) {
                self.data.label = result;
                if (result) {
                    self.renderGrid(self.createLabelCollection(labelId), self.createColumns("mylabels/" + labelId),
                        callback);
                } else {
                    self.parentRender(callback);
                }
            });
        }
    });

    return MyLabelsPage;
});
