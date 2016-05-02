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
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/components/SelectComponent",
    "org/forgerock/openam/ui/common/components/TemplateComponent",
    "text!templates/admin/views/realms/authentication/SelectModuleItem.html",
    "text!templates/admin/views/realms/authentication/SelectModuleOption.html"
], ($, _, AbstractView, BootstrapDialog, UIUtils, SelectComponent, TemplateComponent, SelectModuleItemTemplate,
    SelectModuleOptionTemplate) => {

    SelectComponent = SelectComponent.default;
    TemplateComponent = TemplateComponent.default;

    var EditLinkView = AbstractView.extend({
        editLinkTemplate: "templates/admin/views/realms/authentication/chains/EditLinkTemplate.html",
        editLinkTableTemplate: "templates/admin/views/realms/authentication/chains/EditLinkTableTemplate.html",
        show (view) {
            this.data = view.data;
            var self = this,
                newLink = !self.data.linkConfig,
                linkConfig = self.data.linkConfig || { module: "", options: {}, criteria: "" },
                formData = self.data,
                title = linkConfig.module ? $.t("console.authentication.editChains.editModule")
                    : $.t("console.authentication.editChains.newModule");

            UIUtils.fillTemplateWithData(self.editLinkTemplate, {}, function (template) {
                UIUtils.fillTemplateWithData(self.editLinkTableTemplate, {
                    linkConfig
                }, function (tableTemplate) {
                    BootstrapDialog.show({
                        message () {
                            var $template = $("<div></div>").append(template);
                            $template.find("#editLinkOptions").append(tableTemplate);
                            return $template;
                        },
                        title,
                        closable: false,
                        buttons: [{
                            label: $.t("common.form.cancel"),
                            action (dialog) {
                                view.parent.validateChain();
                                dialog.close();
                            }
                        }, {
                            label: $.t("common.form.ok"),
                            cssClass: "btn-primary",
                            id: "saveBtn",
                            action (dialog) {
                                if (newLink) {
                                    view.data.linkConfig = linkConfig;
                                    view.parent.data.form.chainData.authChainConfiguration.push(linkConfig);
                                    view.parent.addItemToList(view.element);
                                }

                                view.render();
                                dialog.close();
                            }
                        }],
                        onshow (dialog) {
                            dialog.getButton("saveBtn").disable();

                            const itemComponent = new TemplateComponent({
                                template: SelectModuleItemTemplate
                            });

                            const optionComponent = new TemplateComponent({
                                template: SelectModuleOptionTemplate
                            });

                            self.moduleSelect = new SelectComponent({
                                options: formData.allModules,
                                selectedOption: _.find(formData.allModules, "_id", linkConfig.module),
                                onChange (module) {
                                    linkConfig.module = module._id;
                                    linkConfig.type = module.type;
                                    dialog.options.validateDialog(dialog);
                                },
                                itemComponent,
                                optionComponent,
                                searchFields: ["_id", "typeDescription"]
                            });
                            dialog.getModalBody().find("[data-module-select]")
                                .append(self.moduleSelect.render().el);

                            const criteriaOptions = _.map(formData.allCriteria, (value, key) => ({ key, value }));
                            self.criteriaSelect = new SelectComponent({
                                options: criteriaOptions,
                                selectedOption: _.find(criteriaOptions, "key", linkConfig.criteria),
                                onChange (option) {
                                    linkConfig.criteria = option.key;
                                    dialog.options.validateDialog(dialog);
                                },
                                labelField: "value",
                                searchFields: ["value"]
                            });
                            dialog.getModalBody().find("[data-criteria-select]")
                                .append(self.criteriaSelect.render().el);

                            dialog.getModalBody().on("click", "[data-add-option]", function (e) {
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

                            dialog.getModalBody().on("click", "[data-delete-option]", function (e) {
                                var optionsKey = $(e.target).closest("tr").find(".optionsKey").html();
                                if (_.has(linkConfig.options, optionsKey)) {
                                    delete linkConfig.options[optionsKey];
                                }
                                dialog.options.refreshOptionsTab(dialog);
                                dialog.options.validateDialog(dialog);
                            });
                        },
                        validateDialog (dialog) {
                            if (linkConfig.module.length === 0 || linkConfig.criteria.length === 0) {
                                dialog.getButton("saveBtn").disable();
                            } else {
                                dialog.getButton("saveBtn").enable();
                            }
                        },
                        refreshOptionsTab (dialog) {
                            UIUtils.fillTemplateWithData(self.editLinkTableTemplate, {
                                linkConfig
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
