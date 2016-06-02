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
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues"
], (JSONSchema, JSONValues) => {
    describe("org/forgerock/openam/ui/common/models/JSONSchema", () => {
        describe("#constructor", () => {
            let schema;

            beforeEach(() => {
                schema = new JSONSchema({
                    "properties": {
                        "globalProperty": {},
                        "defaults": {
                            "defaultsProperty": {}
                        },
                        "dynamic": {
                            "dynamicProperty": {}
                        }
                    },
                    "type": "object"
                });
            });

            it("groups the top-level properties under a \"global\" property", () => {
                expect(schema.raw.properties).to.contain.keys("global");
                expect(schema.raw.properties.global.properties).to.have.keys("globalProperty");
            });

            // FIXME: Results of calling i18#t are always empty string
            // it("groups the top-level properties with title", () => {
            //     expect(schema.raw.properties.global.title).eq("Global");
            // });

            it("groups the top-level properties with property order", () => {
                expect(schema.raw.properties.global.propertyOrder).eq(-10);
            });

            it("flatten properties in \"defaults\" onto the top-level properties", () => {
                expect(schema.raw.properties).to.contain.keys("defaultsProperty");
            });

            it("flatten properties in \"dynamic\" onto the top-level properties", () => {
                expect(schema.raw.properties).to.contain.keys("dynamicProperty");
            });

            context("when there is no \"defaults\" or \"dynamic\" properties", () => {
                beforeEach(() => {
                    schema = new JSONSchema({
                        "properties": {
                            "globalProperty": {}
                        },
                        "type": "object"
                    });
                });

                it("does not group the top-level properties under a \"global\" property", () => {
                    expect(schema.raw.properties).to.have.keys("globalProperty");
                });
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
        describe("#toFlatWithInheritanceMeta", () => {
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
                    },
                    "anotherPropertyKey": {
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

            const jsonValues = new JSONValues({
                "propertyKey": {
                    "value": "someValue",
                    "inherited": true
                },
                "anotherPropertyKey": {
                    "value": "anotherValue",
                    "inherited": false
                }
            });

            let schema;

            beforeEach(() => {
                schema = jsonSchema.toFlatWithInheritanceMeta(jsonValues);
            });

            it("flattens inherited property values onto the top-level properties", () => {
                expect(schema.raw.properties).to.contain.keys("propertyKey");
                expect(schema.raw.properties.propertyKey).to.contain.keys("type", "required");
            });

            it("sets the title on the flattened properties", () => {
                expect(schema.raw.properties.propertyKey.title).eq("Title");
            });

            it("adds 'isInherited' key to each property of the schema", () => {
                expect(schema.raw.properties).to.contain.keys("propertyKey");
                expect(schema.raw.properties.propertyKey).to.contain.keys("isInherited");
                expect(schema.raw.properties.propertyKey.isInherited).eq(true);

                expect(schema.raw.properties).to.contain.keys("anotherPropertyKey");
                expect(schema.raw.properties.anotherPropertyKey).to.contain.keys("isInherited");
                expect(schema.raw.properties.anotherPropertyKey.isInherited).eq(false);
            });
        });
    });
});
