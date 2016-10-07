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
    "sinon",
    "squire",
    "org/forgerock/openam/ui/common/models/JSONValues"
], (sinon, Squire, JSONValues) => {
    let i18next;
    let JSONSchema;
    describe("org/forgerock/openam/ui/common/models/JSONSchema", () => {
        beforeEach((done) => {
            const injector = new Squire();

            i18next = {
                t: sinon.stub().withArgs("console.common.global").returns("Global Attributes")
            };

            injector.mock("i18next", i18next)
                .require(["org/forgerock/openam/ui/common/models/JSONSchema"], (subject) => {
                    JSONSchema = subject;
                    done();
                });
        });

        describe("#constructor", () => {
            let schemaWithGlobalProps;
            let schemaWithDefaultsProps;
            let schemaWithDefaultsCollectionProps;

            beforeEach(() => {
                schemaWithGlobalProps = new JSONSchema({
                    "properties": {
                        "globalSimpleProperty": {},
                        "globalCollectionProperty": {
                            "type": "object",
                            "title": "",
                            "properties": {}
                        },
                        "dynamic": {}
                    },
                    "type": "object"
                });

                schemaWithDefaultsProps = new JSONSchema({
                    "properties": {
                        "defaults": {
                            "type": "object",
                            "title": "",
                            "properties": {
                                "defaultsSimpleProperty": {},
                                "defaultsCollectionProperty": {
                                    type: "object",
                                    title: "",
                                    properties: {}
                                }
                            }
                        }
                    },
                    "type": "object"
                });

                schemaWithDefaultsCollectionProps = new JSONSchema({
                    "properties": {
                        "defaults": {
                            "type": "object",
                            "title": "",
                            "properties": {
                                "defaultsCollectionProperty": {
                                    type: "object",
                                    title: "",
                                    properties: {}
                                }
                            }
                        }
                    },
                    "type": "object"
                });
            });

            // Global properties
            it("groups the top-level simple properties under a \"global\" property", () => {
                expect(schemaWithGlobalProps.raw.properties).to.contain.keys("global");
                expect(schemaWithGlobalProps.raw.properties.global.properties).to.contain.keys("globalSimpleProperty");
            });

            it("groups the top-level simple properties with title", () => {
                expect(i18next.t).to.be.calledWith("console.common.globalAttributes");
                expect(schemaWithGlobalProps.raw.properties.global.title).eq("Global Attributes");
            });

            it("groups the top-level simple properties with property order", () => {
                expect(schemaWithGlobalProps.raw.properties.global.propertyOrder).eq(-10);
            });

            it("does not group the top-level collection properties under a \"global\" property", () => {
                expect(schemaWithGlobalProps.raw.properties).to.contain.keys("global");
                expect(schemaWithGlobalProps.raw.properties.global.properties).to.not.have
                    .keys("globalCollectionProperty");
            });

            //Defaults properties
            it("ungroups \"defaults\" collection properties, moving them one level up", () => {
                expect(schemaWithDefaultsProps.raw.properties).to.contain.keys("defaultsCollectionProperty");
            });

            it("does not ungroup \"defaults\" simple properties", () => {
                expect(schemaWithDefaultsProps.raw.properties.defaults.properties).to.contain
                    .keys("defaultsSimpleProperty");
            });

            it("ungroups \"defaults\" collection properties, moving them one level up (collection props only)", () => {
                expect(schemaWithDefaultsCollectionProps.raw.properties).to.contain.keys("defaultsCollectionProperty");
            });

            it("removes \"defaults\" property when there are no simple props", () => {
                expect(schemaWithDefaultsCollectionProps.raw.properties).to.not.have.keys("defaults");
            });
        });

        describe("#hasInheritance", () => {
            context("schema has inheritance", () => {
                it("returns true", () => {
                    const jsonSchema = new JSONSchema({
                        type: "object",
                        properties: {
                            propertyCollection: {
                                type: "object",
                                title: "",
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
                        propertyCollection: {
                            type: "object",
                            title: "",
                            properties: {
                                property: {}
                            }
                        }
                    }
                });

                expect(jsonSchema.hasInheritance()).to.be.false;
            });
        });

        describe("#removeNonRequiredProperties", () => {
            let schema;

            beforeEach(() => {
                const jsonSchema = new JSONSchema({
                    "type": "object",
                    "properties": {
                        propertyCollection: {
                            title: "",
                            type: "object",
                            properties: {
                                "propertyKeyRequired": {
                                    required: true
                                },
                                "propertyKeyNonRequired": {
                                    required: false
                                }
                            }
                        }
                    }
                });
                schema = jsonSchema.removeUnrequiredProperties();
            });

            it("removes properties where \"required\" is \"false\"", () => {
                expect(schema.raw.properties.propertyCollection).to.not.have.keys("propertyKeyNonRequired");
            });
        });

        describe("#toFlatWithInheritanceMeta", () => {
            const jsonValues = new JSONValues({
                "com.iplanet.am.smtphost":{
                    "value":"localhost",
                    "inherited":true
                },
                "com.iplanet.am.smtpport":{
                    "value":25,
                    "inherited":true
                }
            });
            let schema;

            beforeEach(() => {
                const jsonSchema = new JSONSchema({
                    "title":"Mail Server",
                    "type":"object",
                    "propertyOrder":3,
                    "properties":{
                        "com.iplanet.am.smtphost":{
                            "title":"Mail Server Host Name",
                            "type":"object",
                            "propertyOrder":0,
                            "description":"(property name: com.iplanet.am.smtphost)",
                            "properties":{
                                "value":{
                                    "type":"string",
                                    "required":false
                                },
                                "inherited":{
                                    "type":"boolean",
                                    "required":true
                                }
                            }
                        }
                    }
                });
                schema = jsonSchema.toFlatWithInheritanceMeta(jsonValues);
            });

            it("flattens inherited property values onto the top-level properties", () => {
                expect(schema.raw.properties).to.contain.keys("com.iplanet.am.smtphost");
                expect(schema.raw.properties["com.iplanet.am.smtphost"]).to.contain
                    .keys("type", "required");
            });

            it("sets the title on the flattened properties", () => {
                expect(schema.raw.properties["com.iplanet.am.smtphost"].title).eq("Mail Server Host Name");
            });

            it("adds 'isInherited' key to each property of the schema", () => {
                expect(schema.raw.properties["com.iplanet.am.smtphost"]).to.contain.keys("isInherited");
                expect(schema.raw.properties["com.iplanet.am.smtphost"].isInherited).eq(true);
            });
        });
    });
});
