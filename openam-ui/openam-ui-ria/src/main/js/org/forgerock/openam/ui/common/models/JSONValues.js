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
], (_) => {
    function groupTopLevelValues (raw) {
        const globalPropertiesToIngore = ["_id", "_type", "defaults", "dynamic"];

        if (_.isEmpty(_.omit(raw, ...globalPropertiesToIngore))) {
            return raw;
        }

        const values = {
            _id: raw._id,
            _type: raw._type,
            global: _.omit(raw, ...globalPropertiesToIngore)
        };
        if (raw.defaults) {
            values.defaults = raw.defaults;
        }
        if (raw.dynamic) {
            values.dynamic = raw.dynamic;
        }

        return values;
    }

    function ungroupValue (raw, propertyKey) {
        const values = _.merge(raw, raw[propertyKey]);
        values[`_${propertyKey}Properties`] = _.keys(raw[propertyKey]);
        delete values[propertyKey];

        return values;
    }

    return class JSONValues {
        constructor (values) {
            const hasDefaults = _.has(values, "defaults");
            const hasDynamic = _.has(values, "dynamic");

            if (hasDefaults || hasDynamic) {
                values = groupTopLevelValues(values);

                if (hasDefaults) { values = ungroupValue(values, "defaults"); }
                if (hasDynamic) { values = ungroupValue(values, "dynamic"); }
            }

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
        toJSON () {
            let json = _.clone(this.raw);

            if (json._defaultsProperties) {
                json.defaults = {};

                _.each(this.raw._defaultsProperties, (property) => {
                    json.defaults[property] = json[property];
                    delete json[property];
                });
                delete json._defaultsProperties;
            }

            if (json._dynamicProperties) {
                json.dynamic = {};

                _.each(this.raw._dynamicProperties, (property) => {
                    json.dynamic[property] = json[property];
                    delete json[property];
                });
                delete json._dynamicProperties;
            }

            json = _.merge(json, json.global);
            delete json.global;

            return JSON.stringify(json);
        }
    };
});
