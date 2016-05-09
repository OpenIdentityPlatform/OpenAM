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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "backbone",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils"
], ($, _, Backbone, EventManager, ValidatorsManager, Constants, UIUtils) => {

    const READ_ONLY_TEMPLATE = "templates/common/components/table/ReadOnlyRow.html";
    const EDIT_ROW_TEMPLATE = "templates/common/components/table/InlineEditRow.html";

    return Backbone.View.extend({
        events: {
            "click [data-save-row]": "save",
            "keyup input.form-control": "save",
            "dblclick td": "edit",
            /* The event handler for the [data-edit-row] is attached to the "keyup" event instead of a "click",
            because inside this handler we are changing the focus to the input element and if the orignal event handler
            is attached to "click", then by the time the focus has changed the key is still pressed and when it is
            finally released, it triggers the "keyup" event handler of the input, which tries to save the row */
            "keyup [data-edit-row]": "edit",
            "mouseup [data-edit-row]": "edit",
            "click [data-delete-row]": "delete",
            "click [data-undo-edit-row]": "undoEdit"
        },
        tagName: "tr",

        /**
         * Initializes the row with the data.
         *
         * @param {object} rowData a key-value pair
         * @param {string} rowData.key key
         * @param {string} rowData.value value
         */
        initialize (rowData) {
            this.data = { rowData };
        },

        renderInReadOnlyMode () {
            this.template = READ_ONLY_TEMPLATE;

            UIUtils.compileTemplate(this.template, this.data).then((template) => {
                this.$el.html(template);
                this.$el.removeClass("am-inline-edit-table-row");
            });

            return this;
        },

        renderInEditMode () {
            this.template = EDIT_ROW_TEMPLATE;

            UIUtils.compileTemplate(this.template, this.data).then((template) => {
                this.$el.html(template);
                this.$el.addClass("am-inline-edit-table-row");
                ValidatorsManager.bindValidators(this.$el);
            });

            return this;
        },

        edit (event) {
            if (event.type === "keyup" && event.keyCode !== 13) { return; }
            this.trigger("edit", this);
        },

        delete () {
            this.trigger("delete", this);
        },

        undoEdit () {
            this.trigger("exitEditMode", this);
        },

        focus () {
            this.$el.find("input:first").focus();
        },

        save (event) {
            event.preventDefault();
            if (event.type === "keyup" && event.keyCode !== 13) { return; }

            const getInputValue = (dataAttr) => this.$el.find(`input[${dataAttr}]`).val().trim();
            const key = getInputValue("data-row-key");
            const value = getInputValue("data-row-value");

            if (!key) {
                return;
            } else {
                this.data.rowData = { key, value };
                this.trigger("exitEditMode", this);
            }
        },

        getData () {
            return this.data.rowData;
        }
    });
});
