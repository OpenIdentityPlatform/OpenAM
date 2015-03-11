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
    "org/forgerock/openam/ui/editor/models/Script"
], function (AbstractView, Script) {

    var EditScriptView = AbstractView.extend({
        initialize: function(options) {
            this.model = null;
        },
        template: "templates/editor/views/EditScriptTemplate.html",
        data: {},
        events: {
            'click input[name=save]': 'submitForm'
        },
        onModelError: function(model, response) {
            console.error('Unrecoverable load failure Script. ' +
                response.status + ' ' + response.statusText);
        },
        onModelSync: function(model, response) {
            this.render();
        },

        render: function (args, callback) {
            var self = this,
                uuid = null;

            // as we interrupt render to update the model, we need to remember callback
            if (callback) { self.renderCallback = callback; }

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

            this.data.entity = _.pick(this.model.attributes, 'uuid', 'name', 'script');

            this.data.languages = [ {name: 'Javascript', value: 'JAVASCRIPT'},
                {name: 'Groovy', value: 'GROOVY'}
            ];

            // TODO temporary, until contexts are not implemented on the server side
            this.data.contexts = [ {name: 'General Purpose', value: 'GENERAL_PURPOSE'},
                {name: 'Authorization', value: 'AUTHORIZATION'},
                {name: 'Client-side Authentication', value: 'CLIENT_SIDE_AUTHENTICATION'},
                {name: 'Server-side Authentication', value: 'SERVER_SIDE_AUTHENTICATION'}
            ];

            this.parentRender(function () {
                if (self.renderCallback) {
                    self.renderCallback();
                }
            });
        },


        // TODO move to Script model
        validate: function () {
            return true;
        },

        submitForm: function () {
            // TODO implement save logic
//            if (this.validate()) {
//
//            }
        },

        syncModel: function(uuid) {
            var syncRequired = !this.model || (uuid && this.model.id !== uuid);

            if (syncRequired && uuid) {
                // edit existing script
                this.stopListening(this.model);
                this.model = new Script( { uuid: uuid} );
                this.listenTo(this.model, 'sync', this.onModelSync);
                this.listenTo(this.model, 'error', this.onModelError);
                this.model.fetch();
            } else if (syncRequired) {
                // create new script, sync is not needed
                syncRequired = false;
                this.stopListening(this.model);
                this.model = new Script();
            }

            return syncRequired;
        }
    });

    return new EditScriptView();
});