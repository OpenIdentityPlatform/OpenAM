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

/*global define, $, _*/
define("org/forgerock/openam/ui/admin/views/console/realms/authentication/Authentication", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openam/ui/admin/models/Form",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/delegates/SMSDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function(AbstractView, BootstrapDialog, Configuration, Constants, EventManager, Form, FormHelper, MessageManager, Router, SMSDelegate, UIUtils) {
    var Authentication = AbstractView.extend({
        template: "templates/admin/views/console/realms/authentication/AuthenticationTemplate.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        events: {
            // Tabs
            'show.bs.tab a[href="#settings"]': 'renderSettingsTab',
            'show.bs.tab a[href="#chains"]'  : 'renderChainsTab',
            'show.bs.tab a[href="#modules"]' : 'renderModulesTab',
            // Settings
            'click #saveChanges': 'save',
            // Chains
            'change #chains input[data-chain-name]' : 'chainSelected',
            'click  #chains button[data-active]'    : 'warningBeforeDeleteChain',
            'click  #chains button[data-chain-name]:not([data-active])': 'deleteChain',
            'click  #chains #deleteChains'          : 'deleteChains',
            'click  #chains #addChain'              : 'addChain',
            // Modules
            'click #addModule':   'addModule',
            'change #modules input[data-module-name]' : 'moduleSelected',
            'click  #modules #editModule': 'editModule',
            'click  #modules button[data-module-name]:not([data-active])': 'deleteModule',
            'click  #modules #deleteModules': 'deleteModules'
        },
        data: {
            rows: []
        },
        addChain: function(e) {
            e.preventDefault();
            var href = $(e.currentTarget).attr('href');
            BootstrapDialog.show({
                title: "Enter the chain name",
                message: '<p>Some helpful text here explaining that you need to name your chain before you can configure it</p><br/><input type="text" id="newName" class="form-control" placeholder="Enter Name"  value="">',
                buttons: [{
                    label: $.t("common.form.next"),
                    cssClass: "btn-primary",
                    action: function(dialog) {
                        dialog.close();
                        //TODO Check name first.
                        Router.navigate( href + dialog.getModalBody().find('#newName').val(), { trigger: true });
                    }
                },{
                    label: $.t("common.form.cancel"),
                    action: function(dialog) {
                        dialog.close();
                    }
                }]
            });
        },
        addModule: function(e) {
            e.preventDefault();
            var self = this;

            UIUtils.fillTemplateWithData("templates/admin/views/console/realms/authentication/AddModuleTemplate.html", self.data, function(html) {
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
                                        Router.navigate(Router.getLink(Router.configuration.routes.module) + moduleName, {trigger: true});
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
        chainSelected: function(event) {
            var hasChainsSelected = this.$el.find('#chains input[type=checkbox]').is(':checked'),
                row = $(event.currentTarget).closest('tr'),
                checked = $(event.currentTarget).is(':checked');

            this.$el.find('#deleteChains').prop('disabled', !hasChainsSelected);
            if (checked) {
                row.addClass('selected');
            } else {
                row.removeClass('selected');
            }
        },
        deleteChain: function(event) {
            var self = this,
                chainName = $(event.currentTarget).attr('data-chain-name');

            SMSDelegate.RealmAuthenticationChain.remove(chainName)
            .done(function(data) {
                self.renderChainsTab();
            })
            .fail(function() {
                // TODO: Add failure condition
            });
        },
        deleteChains: function() {
            var self = this,
                chainNames = self.$el.find('#chains input[type=checkbox]:checked').toArray().map(function(element) {
                    return $(element).attr('data-chain-name');
                }),
                promises = chainNames.map(function(name) {
                    return SMSDelegate.RealmAuthenticationChain.remove(name);
                });

            $.when(promises)
            .done(function(data) {
                self.renderChainsTab();
            })
            .fail(function() {
                // TODO: Add failure condition
            });
        },
        editChain: function(e) {
            e.preventDefault();
            Router.navigate( '#console/chaining/', {trigger: true});
        },
        moduleSelected: function(event) {
            var hasModuleSelected = this.$el.find('#modules input[type=checkbox]').is(':checked'),
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
          Router.navigate(Router.getLink(Router.configuration.routes.module) + moduleName, {trigger: true});


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
                moduleNames = self.$el.find('#modules input[type=checkbox]:checked').toArray().map(function(element) {
                    return $(element).attr('data-module-name');
                }),
                promises = moduleNames.map(function(name) {
                    return SMSDelegate.RealmAuthenticationModule.remove(name);
                });

            $.when(promises)
            .done(function(data) {
                self.renderModulesTab();
            })
            .fail(function() {
                // TODO: Add failure condition
            });
        },
        render: function(args, callback) {
            var self = this;

            this.data.realm = Configuration.globalData.auth.subRealm || "Top level Realm";
            this.data.consolePath = Constants.CONSOLE_PATH;

            this.parentRender(function() {
                self.renderSettingsTab();

                if(callback) {
                    callback();
                }
            });
        },
        renderSettingsTab: function(event) {
            var self = this;

            SMSDelegate.RealmAuthentication.get()
            .done(function(data) {
                UIUtils.fillTemplateWithData("templates/admin/views/console/realms/authentication/SettingsTemplate.html", data.values.result, function(html) {
                    self.$el.find('#settings').html(html);
                    self.data.form = new Form(self.$el.find('#settings .panel-body').get(0), {
                        type: 'object',
                        properties: {
                            adminAuthModule: data.schema.properties.core.properties.adminAuthModule,
                            loginSuccessUrl: data.schema.properties.postauthprocess.properties.loginSuccessUrl,
                            orgConfig: data.schema.properties.core.properties.orgConfig
                        }
                    }, data.values);

                });
            })
            .fail(function() {
                // TODO: Add failure condition
            });
        },
        renderChainsTab: function(event) {
            var self = this,
                sortedChains = [];

            SMSDelegate.RealmAuthenticationChains.getWithDefaults()
            .done(function(data) {

                _.each(data.values.result, function(obj) {
                    // Add default chains to top of list.
                    if ( obj.active) {
                        sortedChains.unshift(obj);
                    } else {
                    sortedChains.push(obj);
                    }
                });

                UIUtils.fillTemplateWithData("templates/admin/views/console/realms/authentication/ChainsTemplate.html", sortedChains, function(html) {
                    self.$el.find('#chains').html(html);
                });
            })
            .fail(function() {
                // TODO: Add failure condition
            });

        },
        renderModulesTab: function(event) {
            var self = this;

            SMSDelegate.RealmAuthenticationModules.get()
            .done(function(data) {
                UIUtils.fillTemplateWithData("templates/admin/views/console/realms/authentication/ModulesTemplate.html", data.values.result, function(html) {
                    self.$el.find('#modules').html(html);
                    self.$el.find('[data-toggle="tooltip"]').tooltip();
                });
            })
            .fail(function() {
                // TODO: Add failure condition
            });
        },
        save: function(event) {
            var promise = SMSDelegate.RealmAuthentication.save(this.data.form.data());

            FormHelper.bindSavePromiseToElement(promise, event.target);
        },
        warningBeforeDeleteChain: function(event) {
            var self = this,
                chainName = $(event.currentTarget).attr('data-chain-name');

            BootstrapDialog.show({
                title: "Delete " + chainName,
                type: BootstrapDialog.TYPE_DANGER,
                message: '<p>This chain is being used as one of the default chains. Deleting may result in locking out the administors or the users.</p><p>Are you sure you want to continue?</p>',
                buttons: [{
                    label: "Delete",
                    cssClass: "btn-danger",
                    action: function(dialog) {
                        self.deleteChain(event);
                    }
                }, {
                    label: "Cancel",
                    action: function(dialog) {
                        dialog.close();
                    }
                }]
            });
        }
    });

    return new Authentication();
});
