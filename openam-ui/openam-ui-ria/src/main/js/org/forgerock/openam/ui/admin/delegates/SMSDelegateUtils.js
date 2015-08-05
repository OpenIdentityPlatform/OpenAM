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
 * Copyright 2015 ForgeRock AS.
 */

 /*global define*/
define("org/forgerock/openam/ui/admin/delegates/SMSDelegateUtils", [
    "jquery",
    "underscore"
], function ($, _) {
    /**
     * @exports org/forgerock/openam/ui/admin/delegates/SMSDelegateUtils
     */
    var obj = {};

    /**
     * Sanitizes JSON Schemas.
     * @param  {Object} schema Schema to sanitize
     * @return {Object}        Sanitized schema
     */
    obj.sanitizeSchema = function (schema) {
        /**
         * Missing and superfluous attribute checks
         */
        schema = obj.rootTypePresent(schema);
        schema = obj.defaultPropertyPresent(schema);

        /**
         * Transforms
         */
        // Recursively transforms propertyOrder attribute to int
        _.forEach(schema.properties, obj.propertyOrderTransform);
        // Recursively add checkbox format to boolean FIXME: To fix server side? Visual only?
        _.forEach(schema.properties, obj.addCheckboxFormatToBoolean);
        // Recursively add string type to enum FIXME: To fix server side
        _.forEach(schema.properties, obj.addStringTypeToEnum);

        /**
         * Additional attributes
         */
        // Adds attribute indicating if all the schema properties are of the type "object" (hence grouped)
        schema.grouped = _.every(schema.properties, obj.isObjectType);
        // Create ordered array
        schema.orderedProperties = _.sortBy(_.map(schema.properties, function (value, key) {
            value._id = key;
            return value;
        }), "propertyOrder");

        return schema;
    };

    // Not intended for use outside of this module
    obj.addCheckboxFormatToBoolean = function (property) {
        if (property.hasOwnProperty("type") && property.type === "boolean") {
            property.format = "checkbox";
        }

        if (property.type === "object") {
            _.forEach(property.properties, obj.addCheckboxFormatToBoolean);
        }
    };

    obj.addStringTypeToEnum = function (property) {
        if (property.hasOwnProperty("enum")) {
            property.type = "string";
        }

        if (property.type === "object") {
            _.forEach(property.properties, obj.addStringTypeToEnum);
        }
    };

    /**
     * Checks for the existence of a "defaults" property
     * @param  {[type]} schema [description]
     * @return {[type]}        [description]
     */
    obj.defaultPropertyPresent = function (schema) {
        if(schema.properties.defaults) {
            console.warn("JSON schema detected with a \"defaults\" section present in it's properties. Removing.");
            delete schema.properties.defaults;
        }

        return schema;
    };

    obj.isObjectType = function(schema) {
        return schema.type === "object";
    };

    obj.propertyOrderTransform = function (property) {
        if (property.hasOwnProperty("propertyOrder")) {
            property.propertyOrder = parseInt(property.propertyOrder.slice(1), 10);
        }

        if (property.type === "object") {
            _.forEach(property.properties, obj.propertyOrderTransform);
        }
    };

    /**
     * Checks for the existence of an object type at the root of the schema. Defaults to "object" if attribute is not
     * found.
     * @param  {Object} schema Schema to check
     * @return {Object}        Checked schema
     */
    obj.rootTypePresent = function(schema) {
        if(!schema.type) {
            console.warn("JSON schema detected without root type attribute! Defaulting to \"object\" type.");
            schema.type = "object";
        }

        return schema;
    };

    obj.sortResultBy = function (attribute) {
        return function(data) {
            data.result = _.sortBy(data.result, attribute);
        };
    };

    return obj;
});
