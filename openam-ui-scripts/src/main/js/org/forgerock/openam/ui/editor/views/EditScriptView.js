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

/*global define, $, _, window, FileReader*/

define("org/forgerock/openam/ui/editor/views/EditScriptView", [
    "bootstrap-dialog",
    "libs/codemirror/lib/codemirror",
    "libs/codemirror/mode/groovy/groovy",
    "libs/codemirror/mode/javascript/javascript",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Base64",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/editor/models/ScriptModel",
    "org/forgerock/openam/ui/editor/delegates/ScriptsDelegate"
], function (BootstrapDialog, CodeMirror, Groovy, Javascript, AbstractView, EventManager, Base64, Constants, UIUtils, Script, ScriptsDelegate) {

    var EditScriptView = AbstractView.extend({
        initialize: function (options) {
            this.model = null;
        },

        template: "templates/editor/views/EditScriptTemplate.html",
        data: {},
        events: {
            'click #upload': 'uploadScript',
            'keyup #upload': 'uploadScript',
            'change [name=upload]': 'readUploadedFile',
            'click #validateScript': 'validateScript',
            'keyup #validateScript': 'validateScript',
            'click #changeContext': 'openDialog',
            'keyup #changeContext': 'openDialog',
            'click input[name=save]': 'submitForm',
            'keyup input[name=save]': 'submitForm',
            'change input[name=language]': 'changeLanguage',
            'submit form': 'submitForm'
        },

        onModelError: function (model, response) {
            console.error('Unrecoverable load failure Script. ' + response.status + ' ' + response.statusText);
        },

        onModelSync: function (model, response) {
            this.renderAfterSyncModel();
        },

        render: function (args, callback) {
            var uuid = null;

            // As we interrupt render to update the model, we need to remember the callback
            if (callback) { this.renderCallback = callback; }

            if (args && args[0]) { uuid = args[0]; }

            this.contextsPromise = ScriptsDelegate.getAllContexts();
            this.defaultContextPromise = ScriptsDelegate.getDefaultGlobalContext();

            /**
             * Guard clause to check if model requires sync'ing/updating
             * Reason: We do not know the id of the data we need until the render function is called with args,
             * thus we can only check at this point if we have the correct model to render this view (the model
             * might already contain the correct data).
             * Behaviour: If the model does require sync'ing then we abort this render via the return and render
             * will it invoked again when the model is updated
             */
            if (this.syncModel(uuid)) { return; }

            this.renderAfterSyncModel();
        },

        /**
         * So the uuid can be omitted to the render function for two reasons:
         * 1. need to create a new script
         * 2. the render function is called from the function onModelSync
         * Then there is a conflict in the function syncModel.
         * In the first case we should to create a new model, in second case is not create.
         * So I divided the render function into two parts, so as not to cause a re-check and avoid the second case.
         */
        renderAfterSyncModel: function () {
            var self = this;

            this.data.entity = _.pick(this.model.attributes, 'uuid', 'name', 'description', 'language', 'context', 'script');

            if (!this.data.contexts) {
                this.contextsPromise.done(function (contexts) {
                    self.data.contexts = contexts.result;
                    self.renderScript();
                });
            } else {
                self.renderScript();
            }

        },

        renderScript: function () {
            var self = this;

            if (this.model.id) {
                this.data.languages = _.findWhere(this.data.contexts,function (context) {
                    return context._id === self.data.entity.context;
                }).languages;
            } else {
                this.data.languages = [];
                this.data.newScript = true;
            }

            this.parentRender(function () {
                this.showUploadButton();

                if (this.data.newScript) {
                    this.openDialog();
                } else {
                    this.initScriptEditor();
                }

                if (this.renderCallback) {
                    this.renderCallback();
                }
            });
        },

        updateFields: function () {
            var app = this.data.entity,
                dataFields = this.$el.find('[data-field]'),
                dataField;

            _.each(dataFields, function (field, key, list) {
                dataField = field.getAttribute('data-field');

                if (field.type === 'radio') {
                    if (field.checked) {
                        app[dataField] = field.value;
                    }
                } else {
                    app[dataField] = field.value;
                }
            });

            app.script = this.scriptEditor.getValue();
        },

        submitForm: function (e) {
            e.preventDefault();

            if (e.type === 'keyup' && e.keyCode !== 13) {
                return;
            }

            var savePromise,
                nonModifiedAttributes = _.clone(this.model.attributes);

            this.updateFields();

            _.extend(this.model.attributes, this.data.entity);
            savePromise = this.model.save();

            if (savePromise) {
                if (this.model.id) {
                    savePromise.done(function (e) {
                        EventManager.sendEvent(Constants.EVENT_HANDLE_DEFAULT_ROUTE);
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "scriptUpdated");
                    });
                } else {
                    savePromise.done(function (e) {
                        EventManager.sendEvent(Constants.EVENT_HANDLE_DEFAULT_ROUTE);
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "scriptCreated");
                    });
                }
            } else {
                _.extend(this.model.attributes, nonModifiedAttributes);
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.validationError);
            }
        },

        syncModel: function (uuid) {
            var syncRequired = !this.model || (uuid && this.model.id !== uuid);

            if (syncRequired && uuid) {
                // edit existing script
                this.stopListening(this.model);
                this.model = new Script({_id: uuid});
                this.listenTo(this.model, 'sync', this.onModelSync);
                this.listenTo(this.model, 'error', this.onModelError);
                this.model.fetch();
            } else if (!uuid) {
                // create new script, sync is not needed
                syncRequired = false;
                this.stopListening(this.model);
                this.model = new Script();
            }

            return syncRequired;
        },

        validateScript: function (e) {
            if (e.type === 'keyup' && e.keyCode !== 13) {
                return;
            }

            var scriptText = this.scriptEditor.getValue(),
                language = this.$el.find('input[name=language]:checked'),
                script,
                self = this;

            if (scriptText.trim() === '') {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "validationNoScript");
                return;
            }

            script = {
                script: Base64.encodeUTF8(scriptText),
                language: language.val()
            };

            ScriptsDelegate.validateScript(script).done(function (result) {
                self.$el.find('#validation').html(UIUtils.fillTemplateWithData("templates/editor/views/ScriptValidationTemplate.html", result));
            });
        },

        uploadScript: function (e) {
            this.$el.find("[name=upload]").trigger("click");
        },

        readUploadedFile: function (e) {
            var self = this,
                file = e.target.files[0],
                reader = new FileReader();

            reader.onload = (function (file) {
                return function (e) {
                    self.scriptEditor.setValue(e.target.result);
                };
            }(file));

            reader.readAsText(file);
        },

        openDialog: function (e) {
            var self = this;

            if (!this.data.defaultContext) {
                this.defaultContextPromise.done(function (context) {
                    self.data.defaultContext = context.defaultContext;
                    self.renderDialog();
                });
            } else {
                self.renderDialog();
            }
        },

        renderDialog: function () {
            var self = this,
                footerButtons = [],
                options = {
                    type: self.data.newScript ? BootstrapDialog.TYPE_PRIMARY : BootstrapDialog.TYPE_DANGER,
                    title: self.data.newScript ? $.t('scripts.edit.dialog.title.select') : $.t('scripts.edit.dialog.title.change'),
                    cssClass: "change-context",
                    closable: !self.data.newScript,
                    message: $('<div></div>'),
                    onshow: function (dialog) {
                        this.message.append(UIUtils.fillTemplateWithData('templates/editor/views/ChangeContextTemplate.html', self.data));
                        dialog.$modalContent.find('[name=changeContext]:checked');
                    }
                };

            if (!self.data.newScript) {
                footerButtons.push({
                    label: $.t('common.form.cancel'),
                    cssClass: 'btn-default',
                    action: function (dialog) {
                        dialog.close();
                    }
                });
            }

            footerButtons.push({
                label: self.data.newScript ? $.t('common.form.save') : $.t('common.form.change'),
                cssClass: self.data.newScript ? 'btn-primary' : 'btn-danger',
                action: function (dialog) {
                    var newContext = dialog.$modalContent.find('[name=changeContext]:checked').val();
                    if (self.data.entity.context !== newContext) {
                        self.data.entity.context = newContext;
                        self.changeContext();
                        self.parentRender(function () {
                            self.showUploadButton();
                            self.initScriptEditor();
                        });
                    }
                    dialog.close();
                }
            });

            options.buttons = footerButtons;
            BootstrapDialog.show(options);

            this.data.newScript = false;
        },

        changeContext: function () {
            var self = this,
                selectedContext = _.findWhere(this.data.contexts, function (context) {
                    return context._id === self.data.entity.context;
                });

            this.data.languages = selectedContext.languages;

            this.data.entity.script = Base64.decodeUTF8(selectedContext.defaultScript);
            this.data.entity.language = selectedContext.defaultLanguage;
        },

        initScriptEditor: function () {
            this.scriptEditor = CodeMirror.fromTextArea(this.$el.find("#script")[0], {
                lineNumbers: true,
                autofocus: true,
                viewportMargin: Infinity,
                mode: this.data.entity.language.toLowerCase(),
                theme: 'forgerock'
            });
        },

        changeLanguage: function (e) {
            this.data.entity.language = e.target.value;
            this.scriptEditor.setOption('mode', this.data.entity.language.toLowerCase());
        },

        showUploadButton: function () {
            // Show the Upload button for modern browsers only. Documented feature.
            // File: Chrome 13; Firefox (Gecko) 3.0 (1.9) (non standard), 7 (7) (standard); Internet Explorer 10.0; Opera 11.5; Safari (WebKit) 6.0
            // FileReader: Firefox (Gecko) 3.6 (1.9.2);	Chrome 7; Internet Explorer 10; Opera 12.02; Safari 6.0.2
            if (window.File && window.FileReader && window.FileList) {
                this.$el.find('#upload').show();
            }
        }
    });

    return new EditScriptView();
});