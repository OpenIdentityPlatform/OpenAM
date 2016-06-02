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
 * Refer to the following naming convention, when adding new functions to this class:
 * <p/>
 * <h2>Function naming conventions</h2>
 * Refer to the following naming convention, when adding new functions to this class:
 * <ul>
 *   <li>For <strong>query</strong> functions, which do not return a new instance of <code>JSONSchema</code>, use <code>#get*</code></li>
 *   <li>For <strong>transform</strong> functions, which do not loose data, use <code>#to*</code> and <code>#from*</code></li>
 *   <li>For <strong>modification</strong> functions, which loose the data, use <code>add*</code> and <code>#remove*</code></li>
 *   <li>For functions, which <strong>check for presense</strong>, use <code>#has*</code> and <code>#is*</code></li>
 *   <li>For <strong>utility</strong> functions use simple verbs, e.g. <code>#omit</code>, <code>#pick</code>, etc.</li>
 * </ul>
 * @module
 * @example
 * // The structure of JSON Value documents emitted from OpenAM is expected to be the following:
 * {
 *   {
 *     globalProperty: true, // Global values (OpenAM wide) are listed at the top-level
 *     default: { ... }, // Default values are organisation (Realm) level values and are nested under "default"
 *     dynamic: { ... } // Dynamic values are user level values (OpenAM wide) and are nested under "dynamic"
 *   }
 * }
 */
define([
    "lodash"
], (_) => class JSONValues {
    constructor (values) {
        this.raw = Object.freeze(values);
    }
    addInheritance (inheritance) {
        const valuesWithInheritance = _.mapValues(this.raw, (value, key) => ({
            value,
            inherited: inheritance[key].inherited
        }));

        return new JSONValues(valuesWithInheritance);
    }
    /**
     * Adds value for the property.
     *
     * @param   {string} path Property key
     * @param   {string} key Key of the property value object
     * @param   {string} value Value to be set
     * @returns {JSONValues} JSONValues object with new value set
     */
    addValueForKey (path, key, value) {
        const clone = _.clone(this.raw);
        clone[path][key] = value;
        return new JSONValues(clone);
    }
    extend (object) {
        return new JSONValues(_.extend({}, this.raw, object));
    }
    /**
     * Creates a new JSONValues object converting from a Global and Organisation values structure to a flatten
     * values structure that can be rendered.
     *
     *  The following transformations applied:
     * * Top-level values are wrapped into a group (using the groupKey specified)
     * * The "defaults" property is flatten and it's values applied to the top-level
     * @param   {string} groupKey Key to use for wrapped top-level values group
     * @returns {JSONValues}      JSONValues object with transforms applied
     */
    fromGlobalAndOrganisation (groupKey) {
        const values = _.transform(this.raw, (result, value, key) => {
            if (key === "defaults") {
                _.merge(result, value);
            } else if (_.startsWith(key, "_")) {
                result[key] = value;
            } else {
                result[groupKey][key] = value;
            }
        }, { [groupKey]: {} });

        return new JSONValues(values);
    }
    getEmptyValueKeys () {
        function isEmpty (value) {
            if (_.isNumber(value)) {
                return false;
            } else if (_.isBoolean(value)) {
                return false;
            }

            return _.isEmpty(value);
        }

        const keys = [];

        _.forIn(this.raw, (value, key) => {
            if (isEmpty(value)) {
                keys.push(key);
            }
        });

        return keys;
    }
    omit (predicate) {
        return new JSONValues(_.omit(this.raw, predicate));
    }
    pick (predicate) {
        return new JSONValues(_.pick(this.raw, predicate));
    }
    removeInheritance () {
        return new JSONValues(_.mapValues(this.raw, "value"));
    }
    /**
     * Creates a new JSONValues object converting from a flatten values structure to a Global and Organisation values
     * structure that can be transmitted to the server.
     *
     *  The following transformations applied:
     * * Top-level values are wrapped into a "defaults" group
     * * Values under the specifed group key are flatten and applied to the top-level
     * @param   {string} groupKey Key of the group to flatten onto the top-level
     * @returns {JSONValues}      JSONValues object with transforms applied
     */
    toGlobalAndOrganisation (groupKey) {
        const values = _.transform(this.raw, (result, value, key) => {
            if (key === groupKey) {
                _.merge(result, value);
            } else if (_.startsWith(key, "_")) {
                result[key] = value;
            } else {
                result["defaults"][key] = value;
            }
        }, { defaults: {} });

        return new JSONValues(values);
    }
});
