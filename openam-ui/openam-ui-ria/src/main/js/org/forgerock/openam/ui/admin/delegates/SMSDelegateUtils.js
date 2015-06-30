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

    obj.sanitizeSchema = function (schema) {
        // Recursively transforms propertyOrder attribute to int
        _.forEach(schema.properties, obj.propertyOrderTransform);

        // Recursively add checkbox format to boolean FIXME: To fix server side? Visual only?
        _.forEach(schema.properties, obj.addCheckboxFormatToBoolean);

        // Recursively add string type to enum FIXME: To fix server side
        _.forEach(schema.properties, obj.addStringTypeToEnum);

        // Create ordered array
        schema.orderedProperties = _.sortBy(_.map(schema.properties, function (value, key) {
            value._id = key;
            return value;
        }), "propertyOrder");

        return schema;
    };

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

    obj.propertyOrderTransform = function (property) {
        if (property.hasOwnProperty("propertyOrder")) {
            property.propertyOrder = parseInt(property.propertyOrder.slice(1), 10);
        }

        if (property.type === "object") {
            _.forEach(property.properties, obj.propertyOrderTransform);
        }
    };

    return obj;
});
