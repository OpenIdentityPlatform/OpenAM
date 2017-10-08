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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "libs/codemirror/lib/codemirror",
    "org/forgerock/commons/ui/common/components/ChangesPending",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Base64",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/admin/models/scripts/ScriptModel",
    "org/forgerock/openam/ui/admin/services/realm/ScriptsService",
    "org/forgerock/openam/ui/admin/services/global/ScriptsService",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/openam/ui/common/util/Promise",
    "libs/codemirror/mode/groovy/groovy",
    "libs/codemirror/mode/javascript/javascript",
    "libs/codemirror/addon/display/fullscreen",
    // jquery dependencies
    "selectize"
], function ($, _, BootstrapDialog, CodeMirror, ChangesPending, Messages, AbstractView, EventManager, Router, Base64,
             Constants, UIUtils, Script, RealmScriptsService, GlobalScriptsService, FormHelper, Promise) {

    return AbstractView.extend({
        initialize () {
            AbstractView.prototype.initialize.call(this);
            this.model = null;
        },
        partials: [
            "partials/alerts/_Alert.html"
        ],
        events: {
            "click [data-upload-script]": "uploadScript",
            "change [name=upload]": "readUploadedFile",
            "click [data-validation-script]": "validateScript",
            "click [data-change-context]": "openDialog",
            "change input[name=language]": "onChangeLanguage",
            "click [data-save]": "submitForm",
            "click [data-delete]": "onDeleteClick",
            "click [data-show-fullscreen]": "editFullScreen",
            "click [data-exit-fullscreen]": "exitFullScreen",
            "change [data-field]": "checkChanges",
            "keyup [data-field]": "checkChanges"
        },

        render (args, callback) {
            var uuid = null;

            this.data.realmPath = args[0];
            this.data.headerActions = [
                { actionPartial: "form/_Button", data:"delete", title:"common.form.delete", icon:"fa-times" }
            ];

            // As we interrupt render to update the model, we need to remember the callback
            if (callback) {
                this.renderCallback = callback;
            }

            // Realm location is the first argument, second one is the script uuid
            if (args.length === 2) {
                uuid = args[1];
            }

            this.contextsPromise = GlobalScriptsService.scripts.getAllContexts();
            this.defaultContextPromise = GlobalScriptsService.scripts.getDefaultGlobalContext();
            this.contextSchemaPromise = GlobalScriptsService.scripts.getSchema();
            this.languageSchemaPromise = GlobalScriptsService.scripts.getContextSchema();

            if (uuid) {
                this.template = "templates/admin/views/realms/scripts/EditScriptTemplate.html";
                this.model = new Script({ _id: uuid });
                this.listenTo(this.model, "sync", this.renderAfterSyncModel);
                this.model.fetch();
            } else {
                this.template = "templates/admin/views/realms/scripts/NewScriptTemplate.html";
                this.newEntity = true;
                this.model = new Script();
                this.renderAfterSyncModel();
            }
        },

        /**
         * So the uuid can be omitted to the render function for two reasons:
         * 1. need to create a new script
         * 2. the render function is called from the function onModelSync
         * Then there is a conflict in the function syncModel.
         * In the first case we should to create a new model, in second case is not create.
         * So the render function is divided into two parts, so as not to cause a re-check and avoid the second case.
         */
        renderAfterSyncModel () {
            var self = this;

            this.data.entity = _.pick(this.model.attributes,
                "uuid", "name", "description", "language", "context", "script");

            if (!this.data.contexts) {
                Promise.all([self.contextsPromise, self.defaultContextPromise, self.contextSchemaPromise,
                        self.languageSchemaPromise]).done(
                    function (results) {
                        self.data.contexts = results[0][0].result;
                        self.data.defaultContext = results[1][0].defaultContext;
                        self.addContextNames(self.data.contexts, results[2][0]);
                        self.langSchema = results[3][0];
                        self.renderScript();
                    });
            } else {
                self.languageSchemaPromise.done(function (langSchema) {
                    self.langSchema = langSchema;
                    self.renderScript();
                });
            }
        },

        renderScript () {
            var self = this,
                context;

            if (this.model.id) {
                context = _.find(this.data.contexts, function (context) {
                    return context._id === self.data.entity.context;
                });
                this.data.contextName = context.name;
                this.data.languages = this.addLanguageNames(context.languages);
            } else {
                this.data.languages = [];
            }

            this.parentRender(function () {
                if (this.newEntity) {
                    this.$el.find("#context").selectize();
                } else {
                    this.changesPendingWidget = ChangesPending.watchChanges({
                        element: this.$el.find(".script-changes-pending"),
                        watchedObj: this.data.entity,
                        undo: !this.newEntity,
                        undoCallback (changes) {
                            _.extend(self.data.entity, changes);
                            var context = _.find(self.data.contexts, {
                                "_id": self.data.entity.context
                            });
                            self.data.contextName = context.name;
                            self.data.languages = self.addLanguageNames(context.languages);
                            self.reRenderView();
                        },
                        alertClass: "alert-warning alert-sm"
                    });

                    this.showUploadButton();
                    this.initScriptEditor();
                }

                if (this.renderCallback) {
                    this.renderCallback();
                }
            });
        },

        reRenderView () {
            this.parentRender(function () {
                this.showUploadButton();
                this.initScriptEditor();

                this.changesPendingWidget.makeChanges(this.data.entity);
                this.changesPendingWidget.reRender(this.$el.find(".script-changes-pending"));
            });
        },

        checkChanges () {
            this.updateFields();
            if (this.newEntity) {
                this.toggleSaveButton(this.checkRequiredFields());
            } else {
                this.changesPendingWidget.makeChanges(this.data.entity);
            }
        },

        updateFields () {
            var self = this,
                app = this.data.entity,
                previousContext = app.context,
                dataFields = this.$el.find("[data-field]"),
                dataField;

            _.each(dataFields, function (field) {
                dataField = field.getAttribute("data-field");

                if (field.type === "radio") {
                    if (field.checked) {
                        app[dataField] = field.value;
                    }
                } else {
                    app[dataField] = field.value.trim();
                }
            });

            if (this.newEntity) {
                if (previousContext !== app.context) {
                    self.toggleSaveButton(false);
                    this.changeContext().then(function () {
                        self.toggleSaveButton(self.checkRequiredFields());
                    });
                }
            } else {
                app.script = this.scriptEditor.getValue();
            }
        },

        checkRequiredFields () {
            return this.data.entity.name && this.data.entity.context && this.data.entity.language;
        },

        submitForm (e) {
            e.preventDefault();

            var self = this,
                savePromise,
                nonModifiedAttributes = _.clone(this.model.attributes);

            this.updateFields();

            _.extend(this.model.attributes, { description: "" }, this.data.entity);
            savePromise = this.model.save();

            if (savePromise) {
                savePromise.done(function () {
                    if (self.newEntity) {
                        Router.routeTo(Router.configuration.routes.realmsScriptEdit, {
                            args: [encodeURIComponent(self.data.realmPath), self.model.id],
                            trigger: true
                        });
                    } else {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                    }
                });
            } else {
                _.extend(this.model.attributes, nonModifiedAttributes);
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.validationError);
            }
        },

        validateScript () {
            var scriptText = this.scriptEditor.getValue(),
                language = this.data.entity.language,
                script,
                self = this;

            script = {
                script: Base64.encodeUTF8(scriptText),
                language
            };

            RealmScriptsService.validateScript(script).done(function (result) {
                UIUtils.fillTemplateWithData("templates/admin/views/realms/scripts/ScriptValidationTemplate.html",
                    result,
                    function (tpl) {
                        self.$el.find("#validation").html(tpl);
                    });
            }).fail(function (response) {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
            });
        },

        uploadScript () {
            this.$el.find("[name=upload]").trigger("click");
        },

        readUploadedFile (e) {
            var self = this,
                file = e.target.files[0],
                reader = new FileReader();

            reader.onload = (function () {
                return function (e) {
                    self.scriptEditor.setValue(e.target.result);
                };
            }(file));

            reader.readAsText(file);
        },

        openDialog () {
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

        renderDialog () {
            BootstrapDialog.show(this.constructDialogOptions());
        },

        constructDialogOptions () {
            var self = this,
                footerButtons = [],
                options = {
                    type: BootstrapDialog.TYPE_DANGER,
                    title: $.t("console.scripts.edit.dialog.title"),
                    cssClass: "script-change-context",
                    message: $("<div></div>"),
                    onshow () {
                        var dialog = this;
                        UIUtils.fillTemplateWithData("templates/admin/views/realms/scripts/ChangeContextTemplate.html",
                            self.data,
                            function (tpl) {
                                dialog.message.append(tpl);
                            });
                    }
                };

            footerButtons.push({
                label: $.t("common.form.cancel"),
                cssClass: "btn-default",
                action (dialog) {
                    dialog.close();
                }
            }, {
                label: $.t("common.form.change"),
                cssClass: "btn-danger",
                action (dialog) {
                    var checkedItem = dialog.$modalContent.find("[name=changeContext]:checked"),
                        newContext = checkedItem.val(),
                        newContextName = checkedItem.parent().text().trim();
                    if (self.data.entity.context !== newContext) {
                        self.data.entity.context = newContext;
                        self.data.contextName = newContextName;
                        self.changeContext().done(_.bind(self.reRenderView, self));
                    }
                    dialog.close();
                }
            });

            options.buttons = footerButtons;

            return options;
        },

        changeContext () {
            var self = this,
                selectedContext = _.find(this.data.contexts, function (context) {
                    return context._id === self.data.entity.context;
                }),
                defaultScript,
                promise = $.Deferred();

            this.data.languages = this.addLanguageNames(selectedContext.languages);

            if (selectedContext.defaultScript === "[Empty]") {
                this.data.entity.script = "";
                if (this.data.languages.length === 1) {
                    this.data.entity.language = this.data.languages[0].id;
                } else {
                    this.data.entity.language = "";
                }
                promise.resolve();
            } else {
                defaultScript = new Script({ _id: selectedContext.defaultScript });
                this.listenTo(defaultScript, "sync", function (model) {
                    if (self.data.languages.length === 1) {
                        self.data.entity.language = self.data.languages[0].id;
                    } else {
                        self.data.entity.language = model.attributes.language;
                    }
                    self.data.entity.script = model.attributes.script;
                    promise.resolve();
                });
                defaultScript.fetch();
            }

            return promise;
        },

        initScriptEditor () {
            this.scriptEditor = CodeMirror.fromTextArea(this.$el.find("#script")[0], {
                lineNumbers: true,
                autofocus: true,
                viewportMargin: Infinity,
                mode: this.data.entity.language.toLowerCase(),
                theme: "forgerock"
            });

            this.scriptEditor.on("update", _.bind(this.checkChanges, this));
        },

        onChangeLanguage (e) {
            this.changeLanguage(e.target.value);
        },

        changeLanguage (lang) {
            this.data.entity.language = lang;
            this.scriptEditor.setOption("mode", lang.toLowerCase());
        },

        showUploadButton () {
            // Show the Upload button for modern browsers only. Documented feature.
            // File: Chrome 13; Firefox (Gecko) 3.0 (1.9) (non standard), 7 (7) (standard); Internet Explorer 10.0;
            //       Opera 11.5; Safari (WebKit) 6.0
            // FileReader: Firefox (Gecko) 3.6 (1.9.2);	Chrome 7; Internet Explorer 10; Opera 12.02; Safari 6.0.2
            if (window.File && window.FileReader && window.FileList) {
                this.$el.find("[data-upload-scripts]").show();
            }
        },

        /**
         * Update context's array using translation from Schema.
         * @param  {Array} contexts Array with script contexts
         * @param  {Object} schema Script schema with translations
         */
        addContextNames (contexts, schema) {
            var i,
                index,
                length = contexts.length;
            if (schema && schema.properties && schema.properties.defaultContext) {
                for (i = 0; i < length; i++) {
                    index = _.indexOf(schema.properties.defaultContext["enum"], contexts[i]._id);
                    contexts[i].name = schema.properties.defaultContext.options.enum_titles[index];
                }
            }
        },

        /**
         * Merge script IDs from Context and translation from Schema to the Language array.
         * @param  {Array} languages Language IDs from Context
         * @returns {Array} result combined array
         */
        addLanguageNames (languages) {
            var result,
                i,
                length = languages.length,
                index;
            if (this.langSchema && this.langSchema.properties && this.langSchema.properties.languages &&
                this.langSchema.properties.languages.items) {
                result = [];
                for (i = 0; i < length; i++) {
                    index = _.indexOf(this.langSchema.properties.languages.items["enum"], languages[i]);
                    result[i] = {
                        id: languages[i],
                        name: this.langSchema.properties.languages.items.options.enum_titles[index]
                    };
                }
            }
            return result;
        },

        onDeleteClick (e) {
            e.preventDefault();

            FormHelper.showConfirmationBeforeDeleting({ type: $.t("console.scripts.edit.script") },
                _.bind(this.deleteScript, this));
        },

        deleteScript () {
            var self = this,
                onSuccess = function () {
                    Router.routeTo(Router.configuration.routes.realmsScripts, {
                        args: [encodeURIComponent(self.data.realmPath)],
                        trigger: true
                    });
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                };

            this.model.destroy({
                success: onSuccess,
                wait: true
            });
        },

        editFullScreen () {
            this.toggleFullScreen(true);
        },

        exitFullScreen () {
            this.toggleFullScreen(false);
        },

        toggleFullScreen (fullScreen) {
            this.scriptEditor.setOption("fullScreen", fullScreen);
            this.$el.find(".full-screen-bar").toggle(fullScreen);
        },

        toggleSaveButton (flag) {
            this.$el.find("[data-save]").prop("disabled", !flag);
        }
    });
});
