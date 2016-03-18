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

define("org/forgerock/openam/ui/common/models/JSONSchema", [
    "lodash",
    "org/forgerock/openam/ui/common/models/cleanJSONSchema"
], (_, cleanJSONSchema) => {
    function throwOnNoSchemaRootType (schema) {
        if (!schema.type) {
            throw new Error("[JSONSchema] No \"type\" attribute found on schema root object.");
        }
    }

    function throwOnPropertiesDefaultsFound (schema) {
        if (schema.properties && schema.properties.defaults) {
            throw new Error("[JSONSchema] \"defaults\" attribute found in schema properties." +
                                " This is probably a mistake and should be removed.");
        }
    }

    function JSONSchema (schema) {
        throwOnNoSchemaRootType(schema);
        throwOnPropertiesDefaultsFound(schema);

        schema = cleanJSONSchema(schema);

        this.raw = Object.freeze(schema);
    }

    JSONSchema.prototype.allPropertiesAreSchemas = function () {
        return _.every(this.raw.properties, (property) => property.type === "object");
    };

    JSONSchema.prototype.enableKey = function () {
        const key = `${_.camelCase(this.raw.title)}Enabled`;
        if (this.raw.properties[key]) {
            return key;
        }
    };

    JSONSchema.prototype.enableProperty = function () {
        return !_.isUndefined(this.raw.properties[`${_.camelCase(this.raw.title)}Enabled`]);
    };

    JSONSchema.prototype.isEmpty = function () {
        return _.isEmpty(this.raw.properties);
    };

    JSONSchema.prototype.keys = function (sort) {
        sort = typeof sort !== "undefined" ? sort : false;

        if (sort) {
            const sortedSchemas = _.sortBy(_.map(this.raw.properties), "propertyOrder");
            return _.map(sortedSchemas, (schema) => _.findKey(this.raw.properties, schema));
        } else {
            return _.keys(this.raw.properties);
        }
    };

    JSONSchema.prototype.passwordKeys = function () {
        const passwordProperties = _.pick(this.raw.properties, _.matches({ format: "password" }));

        return _.keys(passwordProperties);
    };

    JSONSchema.prototype.pick = function (predicate) {
        const schema = _.cloneDeep(this.raw);
        schema.properties = _.pick(this.raw.properties, predicate);

        return new JSONSchema(schema);
    };

    JSONSchema.prototype.omit = function (predicate) {
        const schema = _.cloneDeep(this.raw);
        schema.properties = _.omit(this.raw.properties, predicate);

        return new JSONSchema(schema);
    };

    JSONSchema.prototype.propertiesToSchemaArray = function () {
        return _.mapValues(this.raw.properties, (property) => new JSONSchema(property));
    };

    JSONSchema.prototype.toSchemaWithRequiredProperties = function () {
        return this.pick(_.matches({ required: true }));
    };

    JSONSchema.prototype.toSchemaWithDefaultProperties = function (properties) {
        const schema = _.cloneDeep(this.raw);
        schema.defaultProperties = properties;

        return new JSONSchema(schema);
    };

    return JSONSchema;
});
