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

define([
    "org/forgerock/openam/ui/common/models/JSONSchema"
], (JSONSchema) => {
    describe("org/forgerock/openam/ui/common/models/JSONSchema", () => {
        describe("#fromGlobalAndOrganisation", () => {
            const jsonSchema = new JSONSchema({
                "type": "object",
                "properties": {
                    "topLevelProperty": {},
                    "defaults": {
                        "defaultsProperty1": {},
                        "defaultsProperty2": {}
                    }
                }
            });
            const groupTitle = "Default Group Title";
            const groupKey = "defaultGroupKey";
            const groupPropertyOrder = 1;

            let schema;

            beforeEach(() => {
                schema = jsonSchema.fromGlobalAndOrganisation(groupTitle, groupKey, groupPropertyOrder);
            });

            it("groups the top-level properties under the specified group key", () => {
                expect(schema.raw.properties).to.contain.keys(groupKey);
                expect(schema.raw.properties[groupKey].properties).to.have.keys("topLevelProperty");
            });

            it("flatten properties in \"defaults\" onto the top-level properties", () => {
                expect(schema.raw.properties).to.contain.keys("defaultsProperty1", "defaultsProperty2");
            });

            it("groups the top-level properties with the specified title", () => {
                expect(schema.raw.properties[groupKey].title).eq(groupTitle);
            });

            it("groups the top-level properties with the specified property order", () => {
                expect(schema.raw.properties[groupKey].propertyOrder).eq(groupPropertyOrder);
            });
        });
        describe("#hasInheritance", () => {
            context("schema has inheritance", () => {
                it("returns true", () => {
                    const jsonSchema = new JSONSchema({
                        type: "object",
                        properties: {
                            property: {
                                type: "object",
                                properties: {
                                    inherited: {}
                                }
                            }
                        }
                    });

                    expect(jsonSchema.hasInheritance()).to.be.true;
                });
            });

            it("returns true when the schema has all inherited properties", () => {
                const jsonSchema = new JSONSchema({
                    type: "object",
                    properties: {
                        property: {
                            type: "object",
                            properties: {
                                property: {}
                            }
                        }
                    }
                });

                expect(jsonSchema.hasInheritance()).to.be.false;
            });
        });
        describe("#removeInheritance", () => {
            const jsonSchema = new JSONSchema({
                "type": "object",
                "properties": {
                    "propertyKey": {
                        type: "object",
                        title: "Title",
                        properties: {
                            value: {
                                type: "string",
                                required: true
                            },
                            inherited: {
                                type: "boolean",
                                required: true
                            }
                        }
                    }
                }
            });

            let schema;

            beforeEach(() => {
                schema = jsonSchema.removeInheritance();
            });

            it("flattens inherited property values onto the top-level properties", () => {
                expect(schema.raw.properties).to.have.keys("propertyKey");
                expect(schema.raw.properties.propertyKey).to.contain.keys("type", "required");
            });

            it("sets the title on the flattened properties", () => {
                expect(schema.raw.properties.propertyKey.title).eq("Title");
            });
        });
    });
});
