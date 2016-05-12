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
    "lodash",
    "org/forgerock/openam/ui/common/models/cleanJSONSchema"
], (_, cleanJSONSchema) => {
    function throwOnNoSchemaRootType (schema) {
        if (!schema.type) {
            throw new Error("[JSONSchema] No \"type\" attribute found on schema root object.");
        }
    }

    return class JSONSchema {
        constructor (schema) {
            throwOnNoSchemaRootType(schema);

            schema = cleanJSONSchema(schema);

            this.raw = Object.freeze(schema);
        }
        enableKey () {
            const key = `${_.camelCase(this.raw.title)}Enabled`;
            if (this.raw.properties[key]) {
                return key;
            }
        }
        getEnableProperty () {
            return this.pick(this.enableKey());
        }
        getPropertiesAsSchemas () {
            return _.mapValues(this.raw.properties, (property) => new JSONSchema(property));
        }
        getRequiredPropertyKeys () {
            return _.keys(_.pick(this.raw.properties, _.matches({ required: true })));
        }
        /**
         * Creates a new JSONSchema object converting from a Global and Organisation properties structure to a flatten
         * properties structure that can be rendered.
         *
         *  The following transformations applied:
         * * Top-level properties are wrapped into a group (using the title, key and property order specified)
         * * The "defaults" property is flatten and it's properties applied to the top-level
         * @param   {string} title                Title for wrapped top-level properties group
         * @param   {string} key                  Key to use for wrapped top-level properties group
         * @param   {number|string} propertyOrder Property order for wrapped top-level properties group
         * @returns {JSONSchema}                  JSONSchema object with transforms applied
         */
        fromGlobalAndOrganisation (title, key, propertyOrder) {
            const schema = _.cloneDeep(this.raw);
            const group = {
                properties: _.omit(schema.properties, "defaults"),
                propertyOrder,
                title,
                type: "object"
            };

            schema.properties = _.merge({
                [key]: group
            }, schema.properties.defaults);

            return new JSONSchema(schema);
        }
        hasEnableProperty () {
            return !_.isUndefined(this.raw.properties[`${_.camelCase(this.raw.title)}Enabled`]);
        }
        /**
         * Whether this schema objects' properties are all schemas in their own right.
         * If true, this object is a simply a container for other schemas.
         * @returns {Boolean} Whether this object is a collection
         */
        isCollection () {
            return _.every(this.raw.properties, (property) => property.type === "object");
        }
        isEmpty () {
            return _.isEmpty(this.raw.properties);
        }
        keys (sort) {
            sort = typeof sort !== "undefined" ? sort : false;

            if (sort) {
                const sortedSchemas = _.sortBy(_.map(this.raw.properties), "propertyOrder");
                return _.map(sortedSchemas, (schema) => _.findKey(this.raw.properties, schema));
            } else {
                return _.keys(this.raw.properties);
            }
        }
        passwordKeys () {
            const passwordProperties = _.pick(this.raw.properties, _.matches({ format: "password" }));

            return _.keys(passwordProperties);
        }
        pick (predicate) {
            const schema = _.cloneDeep(this.raw);
            schema.properties = _.pick(this.raw.properties, predicate);

            return new JSONSchema(schema);
        }
        omit (predicate) {
            const schema = _.cloneDeep(this.raw);
            schema.properties = _.omit(this.raw.properties, predicate);

            return new JSONSchema(schema);
        }
        setDefaultProperties (keys) {
            const schema = _.cloneDeep(this.raw);
            schema.defaultProperties = keys;
            return new JSONSchema(schema);
        }
        isWrappedByInheritance () {
            return _.every(this.raw.properties, (property) =>
                property.type === "object" &&
                _.has(property, "properties.inherited")
            );
        }
        getUnwrappedSchema () {
            const properties = _.mapValues(this.raw.properties, "properties.value");
            return {
                properties,
                title: this.raw.title,
                type: this.raw.type
            };
        }
    };
});
