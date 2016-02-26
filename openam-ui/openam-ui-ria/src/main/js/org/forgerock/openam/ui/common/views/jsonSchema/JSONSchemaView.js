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
 * Copyright 2016 ForgeRock AS.
 */

define("org/forgerock/openam/ui/common/views/jsonSchema/JSONSchemaView", [
    "jquery",
    "lodash",
    "backbone",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/common/views/jsonSchema/editors/JSONEditorView",
    "org/forgerock/openam/ui/common/views/jsonSchema/editors/TogglableJSONEditorView"
], ($, _, Backbone, JSONSchema, JSONValues, JSONEditorView, TogglableJSONEditorView) => {
    const JSONSchemaView = Backbone.View.extend({
        initialize: function (options) {
            if (!(options.schema instanceof JSONSchema)) {
                throw new TypeError("[JSONSchemaView] \"schema\" argument is not an instance of JSONSchema.");
            }
            if (!(options.values instanceof JSONValues)) {
                throw new TypeError("[JSONSchemaView] \"values\" argument is not an instance of JSONValues.");
            }

            this.options = options;
        },
        render: function () {
            if (this.options.schema.allPropertiesAreSchemas()) {
                const schemas = this.options.schema.propertiesToSchemaArray();

                this.views = _.map(this.options.schema.keys(true), (key) => {
                    const args = {
                        schema: schemas[key],
                        values: new JSONValues(this.options.values.raw[key])
                    };

                    let view;

                    if (args.schema.enableProperty()) {
                        view = new TogglableJSONEditorView(args);
                    } else {
                        view = new JSONEditorView(args);
                    }

                    $(view.render().el).appendTo(this.$el);
                    return view;
                });
            } else {
                this.views = [
                    new JSONEditorView({
                        el: this.$el,
                        schema: this.options.schema,
                        values: this.options.values
                    }).render()
                ];
            }

            return this;
        },
        values: function () {
            const values = _.map(this.views, (view) => view.values());

            return _.spread(_.merge)(values);
        }
    });

    return JSONSchemaView;
});
