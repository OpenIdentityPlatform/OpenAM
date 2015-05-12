/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define, $, _*/

define("org/forgerock/openam/ui/editor/views/EditScriptView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Base64",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/editor/models/ScriptModel",
    "org/forgerock/openam/ui/editor/delegates/ScriptsDelegate"
], function (AbstractView, Constants, EventManager, Base64, UIUtils, Script, ScriptsDelegate) {

    var EditScriptView = AbstractView.extend({
        initialize: function(options) {
            this.model = null;
        },

        template: "templates/editor/views/EditScriptTemplate.html",
        data: {},
        events: {
            'click #validateScript': 'validateScript',
            'keyup #validateScript': 'validateScript',
            'click input[name=save]': 'submitForm',
            'keyup input[name=save]': 'submitForm'
        },

        onModelError: function(model, response) {
            console.error('Unrecoverable load failure Script. ' +
                response.status + ' ' + response.statusText);
        },

        onModelSync: function(model, response) {
            this.renderAfterSyncModel();
        },

        render: function (args, callback) {
            var uuid = null;

            // As we interrupt render to update the model, we need to remember the callback
            if (callback) { this.renderCallback = callback; }

            // Get the current id
            if(args && args[0]) { uuid = args[0]; }

            /**
             * Guard clause to check if model requires sync'ing/updating
             * Reason: We do not know the id of the data we need until the render function is called with args,
             * thus we can only check at this point if we have the correct model to render this view (the model
             * might already contain the correct data).
             * Behaviour: If the model does require sync'ing then we abort this render via the return and render
             * will it invoked again when the model is updated
             */
            if(this.syncModel(uuid)) { return; }

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
        renderAfterSyncModel: function() {
            var self = this;

            this.data.entity = _.pick(this.model.attributes, 'uuid', 'name', 'language', 'context', 'script');

            this.data.languages = [ {name: 'Groovy', value: 'GROOVY'},
                 {name: 'Javascript', value: 'JAVASCRIPT'}
            ];

            // TODO temporary, until contexts are not implemented on the server side
            this.data.contexts = [ {name: 'General Purpose', value: 'GENERAL_PURPOSE'},
                {name: 'Authorization', value: 'AUTHORIZATION'},
                {name: 'Client-side Authentication', value: 'CLIENT_SIDE_AUTHENTICATION'},
                {name: 'Server-side Authentication', value: 'SERVER_SIDE_AUTHENTICATION'},
                {name: 'Authorization entitlement condition', value: 'AUTHORIZATION_ENTITLEMENT_CONDITION'}
            ];

            this.parentRender(function () {
                if (self.renderCallback) {
                    self.renderCallback();
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
        },

        submitForm: function (e) {
            if (e.type === 'keyup' && e.keyCode !== 13) {
                return;
            }

            var savePromise;

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
                        console.log(e);
                        EventManager.sendEvent(Constants.EVENT_HANDLE_DEFAULT_ROUTE);
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "scriptCreated");
                    });
                }
            } else {
                console.log(this.model.validationError);
                // TODO: implement highlighting for inputs
            }
        },

        syncModel: function(uuid) {
            var syncRequired = !this.model || (uuid && this.model.id !== uuid);

            if (syncRequired && uuid) {
                // edit existing script
                this.stopListening(this.model);
                this.model = new Script( { _id: uuid} );
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

            var scriptText = this.$el.find('#script').val(),
                language = this.$el.find('input[name=language]:checked'),
                script,
                self = this;

            if (scriptText === '') {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "validationNoScript");
                return;
            }

            if (language.length === 0) {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "validationNoLanguage");
                return;
            }

            script = {
                script: Base64.encodeUTF8(scriptText),
                language: language.val()
            };

            ScriptsDelegate.validateScript(script).done(function (result) {
                self.$el.find('#validation').html(UIUtils.fillTemplateWithData("templates/editor/views/ScriptValidationTemplate.html", result));
            });
        }
    });

    return new EditScriptView();
});