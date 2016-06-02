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
    "org/forgerock/openam/ui/common/models/JSONValues"
], (JSONValues) => {
    describe("org/forgerock/openam/ui/common/models/JSONValues", () => {
        describe("#constructor", () => {
            let values;

            beforeEach(() => {
                values = new JSONValues({
                    "globalValue": {},
                    "defaults": {
                        "defaultsValue": {}
                    },
                    "dynamic": {
                        "dynamicValue": {}
                    }
                });
            });

            it("groups the top-level values under a \"global\" value", () => {
                expect(values.raw).to.contain.keys("global");
                expect(values.raw.global).to.have.keys("globalValue");
            });

            it("flatten values in \"defaults\" onto the top-level values", () => {
                expect(values.raw).to.contain.keys("defaultsValue");
            });

            it("flatten values in \"dynamic\" onto the top-level values", () => {
                expect(values.raw).to.contain.keys("dynamicValue");
            });

            context("when there is no \"defaults\" or \"dynamic\" properties", () => {
                beforeEach(() => {
                    values = new JSONValues({
                        "globalValue": {}
                    });
                });

                it("does not group the top-level properties under a \"global\" property", () => {
                    expect(values.raw).to.have.keys("globalValue");
                });
            });
        });
        describe("#addInheritance", () => {
            const jsonValues = new JSONValues({
                propertyKey: "value"
            });

            let values;

            beforeEach(() => {
                values = jsonValues.addInheritance({
                    propertyKey: {
                        inherited: true
                    }
                });
            });

            it("creates an object for each property key", () => {
                expect(values.raw.propertyKey).to.be.an("object");
            });

            it("creates a \"value\" attribute on the property object", () => {
                expect(values.raw.propertyKey.value).to.exist;
                expect(values.raw.propertyKey.value).eq("value");
            });

            it("creates a \"inherited\" attribute on the property object", () => {
                expect(values.raw.propertyKey.inherited).to.exist;
                expect(values.raw.propertyKey.inherited).to.be.true;
            });
        });
        describe("#removeInheritance", () => {
            it("flattens each inherited property into a single value", () => {
                const jsonValues = new JSONValues({
                    propertyKey: {
                        value: "value",
                        inherited: true
                    }
                });

                const values = jsonValues.removeInheritance();

                expect(values.raw.propertyKey).eq("value");
            });
        });
        describe("#toJSON", () => {
            let values;

            beforeEach(() => {
                values = new JSONValues({
                    "_id": {},
                    "_type": {},
                    "globalValue": {},
                    "defaults": {
                        "defaultsValue": {}
                    },
                    "dynamic": {
                        "dynamicValue": {}
                    }
                }).toJSON();
            });

            it("returns an JSON string", () => {
                expect(values).to.be.a("string");
                expect(JSON.parse(values)).to.not.throw;
            });

            it("returns global values at the top-level", () => {
                expect(JSON.parse(values)).to.contain.keys("globalValue");
            });

            it("returns defaults values under a \"defaults\" property", () => {
                expect(JSON.parse(values)).to.contain.keys("defaults");
                expect(JSON.parse(values).defaults).to.have.keys("defaultsValue");
            });

            it("returns dynamic values under a \"dynamic\" property", () => {
                expect(JSON.parse(values)).to.contain.keys("dynamic");
                expect(JSON.parse(values).dynamic).to.have.keys("dynamicValue");
            });

            it("returns \"_id\" at the top-level", () => {
                expect(JSON.parse(values)).to.contain.keys("_id");
            });

            it("returns \"_type\" at the top-level", () => {
                expect(JSON.parse(values)).to.contain.keys("_type");
            });
        });
    });
});
