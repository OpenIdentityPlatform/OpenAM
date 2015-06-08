/*
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

/*global, define*/
define("org/forgerock/openam/ui/admin/views/realms/authentication/ModulesView", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/delegates/SMSDelegate",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, AbstractView, BootstrapDialog, Configuration, EventManager, Router, Constants, SMSDelegate, Form, FormHelper, MessageManager, UIUtils) {
    var ModulesView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/ModulesTemplate.html",
        events: {
            'click #addModule':   'addModule',
            'change input[data-module-name]' : 'moduleSelected',
            'click  #editModule': 'editModule',
            'click  button[data-module-name]:not([data-active])': 'deleteModule',
            'click  #deleteModules': 'deleteModules'
        },
        data:{},
        addModule: function(e) {
            e.preventDefault();
            var self = this;

            UIUtils.fillTemplateWithData("templates/admin/views/realms/authentication/modules/AddModuleTemplate.html", self.data, function(html) {
                BootstrapDialog.show({
                    title: $.t("console.authentication.modules.addModuleDialogTitle"),
                    message: $(html),
                    buttons: [{
                        id: "nextButton",
                        label: $.t("common.form.next"),
                        cssClass: "btn-primary",
                        action: function(dialog) {
                            if (self.addModuleDialogValidation(dialog)) {
                                var moduleName = dialog.getModalBody().find('#newModuleName').val();
                                SMSDelegate.RealmAuthenticationModule.hasModuleName(moduleName)
                                .done(function(result) {
                                    if (result) {
                                        dialog.close();
                                        Router.routeTo(Router.configuration.routes.EditModuleView, {
                                            args: [encodeURIComponent(self.data.realmName), encodeURIComponent(moduleName)],
                                            trigger: true
                                        });
                                    } else {
                                        MessageManager.messages.addMessage({
                                            message: $.t("console.authentication.modules.addModuleDialogError"),
                                            type: "error"
                                        });
                                    }
                                  }
                                ).fail(function(error) {
                                  // TODO: Add failure condition
                                });
                            }
                        }
                    },{
                        label: $.t("common.form.cancel"),
                        action: function(dialog) {
                            dialog.close();
                        }
                    }],
                    onshow: function(dialog) {
                        dialog.getButton('nextButton').disable();
                        dialog.$modalBody.find('#newModuleType').selectize();
                        self.enableOrDisableNextButton(dialog);
                    },
                    onshown: function(dialog) {
                    }
                });
            });

        },
        addModuleDialogValidation: function(dialog) {
            var nameValid = dialog.$modalBody.find('#newModuleName').val().length > 0,
                typeValid = dialog.$modalBody.find('#newModuleType')[0].selectize.getValue().length > 0;
            return (nameValid && typeValid);
        },
        enableOrDisableNextButton: function(dialog) {
            var self = this;
            dialog.$modalBody
            .on('change','#newModuleName, #newModuleType',function(e) {
              if (self.addModuleDialogValidation(dialog)) {
                dialog.getButton('nextButton').enable();
              } else {
                dialog.getButton('nextButton').disable();
              }
            });
        },
        moduleSelected: function(event) {
            var hasModuleSelected = this.$el.find('input[type=checkbox]').is(':checked'),
                row = $(event.currentTarget).closest('tr'),
                checked = $(event.currentTarget).is(':checked');

            this.$el.find('#deleteModules').prop('disabled', !hasModuleSelected);
            if (checked) {
                row.addClass('selected');
            } else {
                row.removeClass('selected');
            }
        },
        editModule: function(event) {
          event.preventDefault();
          var moduleName = $(event.currentTarget).closest('td').find('button[data-module-name]').attr('data-module-name');
          Router.routeTo(Router.configuration.routes.EditModuleView, {
              args: [encodeURIComponent(this.data.realmName), encodeURIComponent(moduleName)],
              trigger: true
          });
        },
        deleteModule: function(event) {
            var self = this,
                moduleName = $(event.currentTarget).attr('data-module-name');

            SMSDelegate.RealmAuthenticationModule.remove(moduleName)
            .done(function(data) {
                self.renderModulesTab();
            })
            .fail(function() {
                // TODO: Add failure condition
            });
        },
        deleteModules: function() {
            var self = this,
                moduleNames = self.$el.find('input[type=checkbox]:checked').toArray().map(function(element) {
                    return $(element).attr('data-module-name');
                }),
                promises = moduleNames.map(function(name) {
                    return SMSDelegate.RealmAuthenticationModule.remove(name);
                });

            $.when(promises)
            .done(function(data) {
                self.render();
            })
            .fail(function() {
                // TODO: Add failure condition
            });
        },
        render: function(args, callback) {
            var self = this;
            this.data.realmName = (args) ? args[0] : " ";

            SMSDelegate.RealmAuthenticationModules.get()
            .done(function(data) {
              self.data.formData = data.values.result;
                self.$el.find('[data-toggle="tooltip"]').tooltip();
                self.parentRender(function () {
                    if (callback) {
                        callback();
                    }
                });
            })
            .fail(function() {
                // TODO: Add failure condition
            });

        },
        save: function(event) {
            var promise = SMSDelegate.RealmAuthentication.save(this.data.form.data());

            FormHelper.bindSavePromiseToElement(promise, event.target);
        }
    });

    return ModulesView;
});
