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

define([
    "jquery",
    "lodash",
    "backbone",
    "jsonEditor",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/admin/utils/JSONEditorTheme",
    "org/forgerock/commons/ui/common/util/UIUtils",

    "popoverclickaway", // depends on jquery and bootstrap
    "selectize" // jquery dependencies
], ($, _, Backbone, JSONEditor, JSONSchema, JSONValues, JSONEditorTheme, UIUtils) => {
    function convertHelpBlocksToPopOvers (element) {
        const template = "templates/common/jsonSchema/editors/_HelpPopover.html";
        UIUtils.compileTemplate(template).then((html) => {
            $(element).find(".help-block").addClass("hidden-lg hidden-md hidden-sm").each((index, value) => {
                const helpPopOver = $(html);

                helpPopOver.popoverclickaway({
                    container: "#content",
                    html: true,
                    placement: "auto top",
                    content: value.innerHTML
                }).click((event) => {
                    event.preventDefault();
                });

                $(value).parent().append(helpPopOver);
            });
        });
    }
    /**
     * Passwords are not delivered to the UI from the server. Thus we set a placeholder informing the user that
     * the password will remain unchanged if they do nothing.
     * @param {DOMElement} element The element to perform the element search from
     */
    function setPlaceholderOnPasswords (element) {
        $(element).find("input:password").attr("placeholder", $.t("common.form.passwordPlaceholder"));
    }

    function applyJSONEditorToElement (element, schema, values) {
        const GRID_COLUMN_WIDTH_1 = 6;
        const GRID_COLUMN_WIDTH_2 = 4;

        JSONEditor.plugins.selectize.enable = true;
        JSONEditor.defaults.themes.openam = JSONEditorTheme.getTheme(GRID_COLUMN_WIDTH_1, GRID_COLUMN_WIDTH_2);

        const hasInheritance = schema.hasInheritance();
        let actualSchema;
        let actualValues;

        if (hasInheritance) {
            actualSchema = schema.toFlatWithInheritanceMeta(values);
            actualValues = values.removeInheritance();
        } else {
            actualSchema = schema;
            actualValues = values;
        }
        actualSchema = actualSchema.raw;
        actualValues = actualValues.raw;

        const editor = new JSONEditor(element[0], {
            "disable_collapse": true,
            "disable_edit_json": true,
            "disable_properties": true,
            "iconlib": "fontawesome4",
            "schema": actualSchema,
            "theme": "openam"
        });

        convertHelpBlocksToPopOvers(element);
        setPlaceholderOnPasswords(element);

        editor.setValue(actualValues);

        return editor;
    }

    const JSONEditorView = Backbone.View.extend({
        className: "jsoneditor-block",
        initialize (options) {
            if (!(options.schema instanceof JSONSchema)) {
                throw new TypeError("[JSONEditorView] \"schema\" argument is not an instance of JSONSchema.");
            }
            if (!(options.values instanceof JSONValues)) {
                throw new TypeError("[JSONEditorView] \"values\" argument is not an instance of JSONValues.");
            }

            this.options = _.defaults(options, {
                displayTitle: true
            });
        },
        toggleInheritance (propertySchemaPath, propValue, isInherited) {
            this.options.values = this.options.values.addValueForKey(propertySchemaPath, "inherited", isInherited);
            this.options.values = this.options.values.addValueForKey(propertySchemaPath, "value", propValue);

            this.render();
        },
        render () {
            this.$el.empty();

            this.jsonEditor = applyJSONEditorToElement(
                this.$el,
                this.options.schema,
                this.options.values
            );

            if (!this.options.displayTitle) {
                this.$el.find("[data-header]").parent().hide();
            }

            return this;
        },
        isValid () {
            return this.jsonEditor.validate().length === 0;
        },
        getData () {
            const passwordKeys = this.options.schema.getPasswordKeys();
            const values = new JSONValues(this.jsonEditor.getValue());
            let valuesWithoutEmptyPasswords = values.omit((value, key) => {
                if (passwordKeys.indexOf(key) !== -1 && _.isEmpty(value)) {
                    return true;
                }
            });

            if (this.options.schema.hasInheritance()) {
                valuesWithoutEmptyPasswords = valuesWithoutEmptyPasswords.addInheritance(this.options.values.raw);
            }

            this.options.values = valuesWithoutEmptyPasswords;
            return valuesWithoutEmptyPasswords.raw;
        },
        setData (data) {
            this.options.values = this.options.values.extend(data);
        }
    });

    return JSONEditorView;
});
