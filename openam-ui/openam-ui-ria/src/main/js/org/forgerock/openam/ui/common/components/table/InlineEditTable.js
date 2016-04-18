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

define("org/forgerock/openam/ui/common/components/table/InlineEditTable", [
    "jquery",
    "lodash",
    "backbone",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/components/table/InlineEditRow"
], ($, _, Backbone, UIUtils, InlineEditRow) => Backbone.View.extend({
    template: "templates/common/components/table/InlineEditTable.html",

    /**
     * Initializes the table with editables rows. Only single row is allowed to be in edit mode at a time.
     *
     * @param {object[]} [values=[]] Data array to be passed to the rows. Format of individual object is dictated by the
     *                              row view.
     */
    initialize ({ values = [] }) {
        this.values = values;
        this.rows = [];
    },

    render () {
        UIUtils.compileTemplate(this.template).then((template) => {
            this.$el.html(template);

            this.tBody = this.$el.find("tbody");

            _.each(this.values, (value) => {
                const row = this.initRow(value);
                this.tBody.append(row.renderInReadOnlyMode().$el);
                this.rows.push(row);
            });

            this.appendEmptyNewRowToTheBottom();
        });

        return this;
    },

    initRow (rowData = {}) {
        const row = new InlineEditRow(rowData);

        const enterEditMode = (row) => {
            if (row === this.currentlyEditedRow) {
                return;
            }

            if (this.currentlyEditedRow) {
                if (this.currentlyEditedRow === this.newEmptyRow) {
                    this.currentlyEditedRow.delete();
                } else {
                    this.currentlyEditedRow.renderInReadOnlyMode();
                }
            }

            row.renderInEditMode().focus();
            this.currentlyEditedRow = row;
        };

        const exitEditMode = (row) => {
            this.currentlyEditedRow.renderInReadOnlyMode();

            const newRowAddedToTheTable = (row === this.newEmptyRow);
            this.appendEmptyNewRowToTheBottom();

            if (newRowAddedToTheTable) {
                this.rows.push(row);
                this.newEmptyRow.focus();
            }
        };

        const deleteRow = (row) => {
            this.rows = _.without(this.rows, row);
            row.remove();
        };

        row.on("edit", enterEditMode);
        row.on("exitEditMode", exitEditMode);
        row.on("delete", deleteRow);

        return row;
    },

    appendEmptyNewRowToTheBottom () {
        const row = this.initRow();
        this.tBody.append(row.renderInEditMode().$el);

        this.currentlyEditedRow = row;
        this.newEmptyRow = row;
    },

    getData () {
        return _.map(this.rows, (row) => row.getData());
    }
}));
