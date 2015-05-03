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

/*global _ $ define*/
define('org/forgerock/openam/ui/admin/models/Form', [
    'jsonEditor'
], function(JSONEditor) {
    var obj = function Form(element, schema, values) {
        this.element = element;
        this.schema = schema;
        this.values = values;

        this.editor = new JSONEditor(element, {
            disable_collapse: true,
            disable_edit_json: true,
            disable_properties: true,
            iconlib: "fontawesome4",
            schema: schema,
            theme: 'bootstrap3'
        });

        element = $(element);

        element.find('div[data-schematype="array"] p').addClass('help-block');

        element.find('.help-block').addClass('help-block-collapsed')
        .click(function(event) {
            if(event.target === this) {
                $(event.target).toggleClass('help-block-collapsed help-block-expanded');
            }
        });

        this.reset();
    };

    obj.prototype.data = function() {
        return this.editor.getValue();
    };

    obj.prototype.reset = function() {
        this.editor.setValue(_.pick(this.values, _.keys(this.schema.properties)));
    };

    return obj;
});
