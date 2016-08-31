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
    "squire",
    "sinon"
], (Squire, sinon) => {
    let query;
    let URIUtils;

    beforeEach((done) => {
        const injector = new Squire();

        URIUtils = {
            getCurrentQueryString: sinon.stub()
        };

        injector.mock("org/forgerock/commons/ui/common/util/URIUtils", URIUtils)
        .require(["org/forgerock/openam/ui/common/util/uri/query"], (obj) => {
            query = obj;
            done();
        });
    });

    describe("org/forgerock/openam/ui/common/uri/query", () => {
        describe("#urlParamsFromObject", () => {
            describe("when the argument is an object of key value pairs", () => {
                it("returns a query string", () => {
                    const params = { foo:"bar", alice:"bob" };
                    expect(query.urlParamsFromObject(params)).eql("foo=bar&alice=bob");
                });
            });
            describe("when the argument is an empty object", () => {
                it("returns an empty string", () => {
                    const params = {};
                    expect(query.urlParamsFromObject(params)).eql("");
                });
            });
            describe("when no argument is provided", () => {
                it("returns an empty string", () => {
                    expect(query.urlParamsFromObject()).eql("");
                });
            });
        });

        describe("#parseParameters", () => {
            describe("when param string is provided", () => {
                it("returns an empty object.", () => {
                    const string = "";
                    expect(query.parseParameters(string)).eql({});
                });
            });

            describe("when a param string is provided", () => {
                it("returns an object of key pair values", () => {
                    const string = "foo=bar&alice=bob";
                    expect(query.parseParameters(string)).eql({ foo:"bar", alice:"bob" });
                });
            });
        });

        describe("#getCurrentQueryParameters", () => {
            describe("when the current url contains a query", () => {
                it("returns an object of key pair values", () => {
                    URIUtils.getCurrentQueryString.returns("foo=bar&alice=bob");
                    expect(query.getCurrentQueryParameters()).eql({ foo:"bar", alice:"bob" });
                });
            });

            describe("when the current url has no query", () => {
                it("returns an empty object.", () => {
                    URIUtils.getCurrentQueryString.returns("");
                    expect(query.getCurrentQueryParameters()).eql({});
                });
            });
        });
    });
});
