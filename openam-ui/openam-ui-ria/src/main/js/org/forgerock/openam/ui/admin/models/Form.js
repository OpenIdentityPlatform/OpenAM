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

 /**
 * @deprecated
 */
define([
    "jquery",
    "lodash",
    "jsonEditor",
    "org/forgerock/openam/ui/admin/utils/JSONEditorTheme",
    "popoverclickaway", // depends on jquery and bootstrap
    "selectize" // jquery dependencies
], function ($, _, JSONEditor, JSONEditorTheme) {
    var obj = function Form (element, schema, values) {
        console.warn("[Form] \"Form\" is deprecated. Use a FlatJSONSchemaView or GroupedJSONSchemaView instead.");

        this.element = element;
        this.schema = schema;
        this.values = values;

        // Attributes that are identifiable as passwords
        const passwordProperties = _.where(schema.properties, { format: "password" });
        this.passwordAttributes = _.map(passwordProperties, (property) => _.findKey(schema.properties, property));

        JSONEditor.plugins.selectize.enable = true;
        JSONEditor.defaults.themes.openam = JSONEditorTheme.getTheme(6, 4);

        this.editor = new JSONEditor(element, {
            "disable_collapse": true,
            "disable_edit_json": true,
            "iconlib": "fontawesome4",
            schema,
            "theme": "openam"
        });

        /**
         * Passwords are not delivered to the UI from the server. Thus we set a placeholder informing the user that
         * the password will remain unchanged if they do nothing
         */
        $(element).find("input:password").attr("placeholder", $.t("common.form.passwordPlaceholder"));

        $(element).find(".help-block").addClass("hidden-lg hidden-md hidden-sm").each(function () {
            var group = $(this).parent(),
                element = $('<a class="btn info-button visible-lg-inline-block' +
                    ' visible-md-inline-block visible-sm-inline-block" ' +
                    'tabindex="0" data-toggle="popoverclickaway" ><i class="fa fa-info-circle"></i></a>');

            $(group).append(element);

            element.popoverclickaway({
                container: "#content",
                html: true,
                placement: "auto top",
                content: this.innerHTML
            });
            element.click(function (event) {
                event.preventDefault();
            });
        });

        this.reset();
    };

    /**
     * Filters out empty, specified attributes from an object
     * @param  {Object} object    Object to filter
     * @param  {Array} attributes Attribute names to filter
     * @returns {Object}          Filtered object
     */
    function filterEmptyAttributes (object, attributes) {
        return _.omit(object, function (value, key) {
            if (_.contains(attributes, key)) {
                return _.isEmpty(value);
            } else {
                return false;
            }
        });
    }

    obj.prototype.data = function () {
        return filterEmptyAttributes(this.editor.getValue(), this.passwordAttributes);
    };

    obj.prototype.reset = function () {
        this.editor.setValue(_.pick(this.values, _.keys(this.schema.properties)));
    };

    obj.prototype.destroy = function () {
        this.editor.destroy();
    };

    return obj;
});
