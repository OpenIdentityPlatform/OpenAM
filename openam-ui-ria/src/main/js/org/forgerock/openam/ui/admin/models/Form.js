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
    'jsonEditor',
    'org/forgerock/openam/ui/admin/utils/JsonEditorTheme'
], function (JSONEditor, JsonEditorTheme) {
    var obj = function Form(element, schema, values) {
        this.element = element;
        this.schema = schema;
        this.values = values;

        JSONEditor.plugins.selectize.enable = true;
        JSONEditor.defaults.themes.openam = JsonEditorTheme.getTheme(6, 4);

        this.editor = new JSONEditor(element, {
            disable_collapse: true,
            disable_edit_json: true,
            disable_properties: true,
            iconlib: "fontawesome4",
            schema: schema,
            theme: "openam"
        });

        $(element).find('.help-block').hide().each(function () {
            var group = $(this).parent(),
                button = $('<button class="btn btn-default info-button" type="button"><i class="fa fa-info-circle"></i></button>');

            $(group).append(button);

            button.popover({
                container: '#content',
                html: true,
                placement: 'auto top',
                trigger: 'focus',
                content: this.innerHTML,
                title: group.find("label:first-of-type").text()
            });

        });

        this.reset();
    };

    obj.prototype.data = function () {
        return this.editor.getValue();
    };

    obj.prototype.reset = function () {
        this.editor.setValue(_.pick(this.values, _.keys(this.schema.properties)));
    };

    return obj;
});
