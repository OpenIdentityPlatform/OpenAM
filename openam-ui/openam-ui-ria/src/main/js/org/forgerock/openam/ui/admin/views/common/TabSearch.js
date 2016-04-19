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

/**
 * @module org/forgerock/openam/ui/admin/views/common/TabSearch
 */
define("org/forgerock/openam/ui/admin/views/common/TabSearch", [
    "lodash",
    "jquery",
    "backbone",
    "handlebars",
    "selectize"
], (_, $, Backbone, Handlebars) => {

    const BEHAVIOUR_GROUPED_JSONSCHEMA = "groupedJsonSchema";

    function throwOnInvalidOptions (options) {
        if (!options || !_.isObject(options)) {
            throw new Error("[TabSearch] No \"options\" object found.");
        } else {
            if (!options.behaviour) {
                throw new Error("[TabSearch] No \"options.behaviour\" string found.");
            }
            if (!options.onChange) {
                throw new Error("[TabSearch] No \"options.onChange\" function found.");
            }
            if (!options.properties) {
                throw new Error("[TabSearch] No \"options.properties\" object found.");
            }
        }
    }

    function createSelectize (element, callback) {

        element.selectize({
            sortField: "text",
            onChange (value) {
                const optgroup = this.options[value].optgroup;
                callback(optgroup, value);
                this.clear(true);
            }
        });

        return element[0].selectize;
    }

    function populateOptionsFromJsonSchemaGroup (properties, selectize) {

        _.each(properties, (group, groupName) => {
            selectize.addOptionGroup(groupName, {
                label: group.title || " "
            });
            _.each(group.properties, (option, key) => {
                selectize.addOption({
                    text: option.title,
                    value: key,
                    optgroup: groupName
                });
            });
        });
        selectize.refreshOptions(false);
    }

    function populateSelectizeOptions (properties, behaviour, selectize) {
        if (behaviour === BEHAVIOUR_GROUPED_JSONSCHEMA) {
            populateOptionsFromJsonSchemaGroup(properties, selectize);
        }
    }

    const TabSearch = Backbone.View.extend({
        /**
         * @param  {object} options Contains the options which are passed in
         * @param  {string} options.behaviour Determines the behavior of the TabSearch
         * @param  {object} options.properties Contains the list of searchable properties
         * @param  {function} options.onChangeCallback. The function that is called when an option is selected,
         */
        initialize (options) {
            throwOnInvalidOptions(options);
            this.options = options;
        },

        render () {
            const template = "{{> form/_Select}}";
            const data = {
                cssClass:"am-selectize-search",
                placeholder: "common.form.search",
                data: "search"
            };
            const html = Handlebars.compile(template)(data);
            this.$el.html(html);
            const selectize = createSelectize(this.$el.find("[data-search]"), this.options.onChange);
            populateSelectizeOptions(this.options.properties, this.options.behaviour, selectize);

            return this;
        }
    });

    TabSearch.BEHAVIOUR_GROUPED_JSONSCHEMA = BEHAVIOUR_GROUPED_JSONSCHEMA;


    return TabSearch;
});
