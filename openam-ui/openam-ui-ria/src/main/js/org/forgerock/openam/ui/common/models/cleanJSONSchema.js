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
    "lodash"
], (_) => {
    /**
     * Determines whether the specified object is of type <code>object</code>
     * @param   {Object}  object Object to determine the type of
     * @returns {Boolean}        Whether the object is of type <code>object</code>
     */
    function isObjectType (object) {
        return object.type === "object";
    }

    /**
     * Recursively invokes the specified functions over each object's properties
     * @param {Object} object   Object with properties
     * @param {Array} callbacks Array of functions
     */
    function eachProperty (object, callbacks) {
        if (isObjectType(object)) {
            _.forEach(object.properties, (property, key) => {
                _.forEach(callbacks, (callback) => {
                    callback(property, key);
                });

                if (isObjectType(property)) {
                    eachProperty(property, callbacks);
                }
            });
        }
    }

    /**
    * Transforms boolean types to checkbox format
    * @param {Object} property Property to transform
    */
    function transformBooleanTypeToCheckboxFormat (property) {
        if (property.hasOwnProperty("type") && property.type === "boolean") {
            property.format = "checkbox";
        }
    }

    /**
    * Recursively add string type to enum
    * FIXME: To fix server side
    * @param {Object} property Property to transform
    */
    function transformEnumTypeToString (property) {
        if (property.hasOwnProperty("enum")) {
            property.type = "string";
        }
    }

    /**
     * Transforms propertyOrder attribute to integer
     * @param {Object} property Property to transform
     */
    function transformPropertyOrderAttributeToInt (property) {
        if (property.hasOwnProperty("propertyOrder") && !_.isNumber(property.propertyOrder)) {
            const orderWithoutPrefixedCharacter = property.propertyOrder.slice(1);
            property.propertyOrder = parseInt(orderWithoutPrefixedCharacter, 10);
        }
    }

    /**
     * Warns if a property is inferred to be a password and does not have a format of password
     * @param {Object} property Property to transform
     * @param {String} name Raw property name
     */
    function warnOnInferredPasswordWithoutFormat (property, name) {
        const possiblePassword = name.toLowerCase().indexOf("password", name.length - 8) !== -1;
        const hasFormat = property.format === "password";
        if (property.type === "string" && possiblePassword && !hasFormat) {
            console.error(`[cleanJSONSchema] Detected (inferred) a password property \"${name}\" ` +
                "without format attribute of \"password\"");
        }
    }

    const exports = function (schema) {
        /**
         * Property transforms & warnings
         */
        eachProperty(schema, [transformPropertyOrderAttributeToInt,
                              transformBooleanTypeToCheckboxFormat,
                              transformEnumTypeToString,
                              warnOnInferredPasswordWithoutFormat]);

        return schema;
    };

    return exports;
});
