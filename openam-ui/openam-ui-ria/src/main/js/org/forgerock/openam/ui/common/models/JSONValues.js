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
 * For <strong>query</strong> methods, which do not return new instance of <code>JSONValues</code> class, use
 * <code>get*</code>
 * For <strong>transformation</strong> methods, which do not loose data, use <code>to*</code>\/<code>from*</code>
 * For <strong>modification</strong> methods, which loose the data, use <code>add*</code>\/<code>remove*</code>
 * For methods, which <strong>check the presense</strong>, use <code>has*</code>\/<code>is*</code>
 * For <strong>utility</strong> methods use simple verbs, e.g. <code>omit</code>, <code>pick</code>, etc.
 * @module org/forgerock/openam/ui/common/models/JSONValues
 */
define([
    "lodash"
], (_) => class JSONValues {
    constructor (values) {
        this.raw = Object.freeze(values);
    }
    extend (object) {
        return new JSONValues(_.extend({}, this.raw, object));
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
    omit (predicate) {
        return new JSONValues(_.omit(this.raw, predicate));
    }
    pick (predicate) {
        return new JSONValues(_.pick(this.raw, predicate));
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
    removeInheritance () {
        return new JSONValues(_.mapValues(this.raw, "value"));
    }
    addInheritance (valuesWithoutInheritance) {
        return new JSONValues(_.transform(this.raw, (result, value, key) => {
            result[key] = this.raw[key];
            result[key].value = valuesWithoutInheritance[key];
        }));
    }
});
