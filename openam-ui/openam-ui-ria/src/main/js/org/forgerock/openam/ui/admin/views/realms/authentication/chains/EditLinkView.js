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

define("org/forgerock/openam/ui/admin/views/realms/authentication/chains/EditLinkView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/commons/ui/common/util/UIUtils",

    // jquery dependencies
    "selectize"
], function ($, _, AbstractView, BootstrapDialog, UIUtils) {

    var EditLinkView = AbstractView.extend({
        editLinkTemplate: "templates/admin/views/realms/authentication/chains/EditLinkTemplate.html",
        editLinkTableTemplate: "templates/admin/views/realms/authentication/chains/EditLinkTableTemplate.html",
        show: function (view) {
            this.data = view.data;
            var self = this,
                newLink = !self.data.linkConfig,
                linkConfig = self.data.linkConfig || { module: "", options: {}, criteria: "" },
                formData = self.data,
                title = linkConfig.module ? $.t("console.authentication.editChains.editModule")
                    : $.t("console.authentication.editChains.newModule");

            UIUtils.fillTemplateWithData(self.editLinkTemplate, {
                linkConfig: linkConfig,
                allModules: formData.allModules,
                allCriteria: formData.allCriteria
            }, function (template) {
                UIUtils.fillTemplateWithData(self.editLinkTableTemplate, {
                    linkConfig: linkConfig
                }, function (tableTemplate) {
                    BootstrapDialog.show({
                        message: function () {
                            var $template = $("<div></div>").append(template);
                            $template.find("#editLinkOptions").append(tableTemplate);
                            return $template;
                        },
                        title: title,
                        closable: false,
                        buttons: [{
                            label: $.t("common.form.cancel"),
                            action: function (dialog) {
                                view.parent.validateChain();
                                dialog.close();
                            }
                        }, {
                            label: $.t("common.form.ok"),
                            cssClass: "btn-primary",
                            id: "saveBtn",
                            action: function (dialog) {
                                var moduleSelectize = dialog.getModalBody().find("#selectModule")[0].selectize;

                                linkConfig.module = moduleSelectize.getValue();
                                linkConfig.type = _.find(moduleSelectize.options, {
                                    _id: moduleSelectize.getValue()
                                }).type;
                                linkConfig.criteria =
                                    dialog.getModalBody().find("#selectCriteria")[0].selectize.getValue();

                                if (newLink) {
                                    view.data.linkConfig = linkConfig;
                                    view.parent.data.form.chainData.authChainConfiguration.push(linkConfig);
                                    view.parent.addItemToList(view.element);
                                }

                                view.render();
                                dialog.close();
                            }
                        }],
                        onshow: function (dialog) {
                            dialog.getButton("saveBtn").disable();
                            dialog.getModalBody().find("#selectModule").selectize({
                                options: formData.allModules,
                                searchField: ["_id", "typeDescription"],
                                render: {
                                    item: function (item) {
                                        return "<div>" + item._id + " - <span class='text-muted'><em>" +
                                            item.typeDescription + "</em></span></div>";
                                    },
                                    option: function (item) {
                                        return "<div><div>" + item._id + "</div><div class='small text-muted'><em>" +
                                            item.typeDescription + "</em></div></div>";
                                    }
                                },
                                onChange: function () {
                                    dialog.options.validateDialog(dialog);
                                }

                            });

                            dialog.getModalBody().find("#selectCriteria").selectize({
                                onChange: function () {
                                    dialog.options.validateDialog(dialog);
                                }
                            });

                            dialog.getModalBody().on("click", "#addOption", function (e) {
                                var
                                    $tr = $(e.target).closest("tr"),
                                    optionsKey = $tr.find("#optionsKey").val(),
                                    optionsValue = $tr.find("#optionsValue").val(),
                                    options = {};

                                options[optionsKey] = optionsValue;
                                if (optionsKey && optionsValue && !_.has(linkConfig.options, optionsKey)) {
                                    _.extend(linkConfig.options, options);
                                    dialog.options.refreshOptionsTab(dialog);
                                    dialog.options.validateDialog(dialog);
                                }
                            });

                            dialog.getModalBody().on("click", ".delete-option", function (e) {
                                var optionsKey = $(e.target).closest("tr").find(".optionsKey").html();
                                if (_.has(linkConfig.options, optionsKey)) {
                                    delete linkConfig.options[optionsKey];
                                }
                                dialog.options.refreshOptionsTab(dialog);
                                dialog.options.validateDialog(dialog);
                            });
                        },
                        validateDialog: function (dialog) {
                            var moduleValue = dialog.getModalBody().find("#selectModule")[0].selectize.getValue(),
                                criteriaValue = dialog.getModalBody().find("#selectCriteria")[0].selectize.getValue();
                            if (moduleValue.length === 0 || criteriaValue.length === 0) {
                                dialog.getButton("saveBtn").disable();
                            } else {
                                dialog.getButton("saveBtn").enable();
                            }
                        },
                        refreshOptionsTab: function (dialog) {
                            UIUtils.fillTemplateWithData(self.editLinkTableTemplate, {
                                linkConfig: linkConfig
                            }, function (tableTemplate) {
                                dialog.getModalBody().find("#editLinkOptions").html(tableTemplate);
                            });
                        }

                    });
                });
            });
        }
    });
    return new EditLinkView();
});
