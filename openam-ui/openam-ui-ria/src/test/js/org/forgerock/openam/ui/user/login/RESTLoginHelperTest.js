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
    "squire"
], function (Squire) {
    describe("org/forgerock/openam/ui/user/login/RESTLoginHelper", function () {
        var RESTLoginHelper;

        before(function (done) {
            var injector = new Squire();

            injector
                .mock("org/forgerock/openam/ui/user/delegates/AuthNDelegate", {})
                .mock("org/forgerock/openam/ui/user/UserModel", {})
                .mock("org/forgerock/commons/ui/common/main/ViewManager", {})
                .require(["org/forgerock/openam/ui/user/login/RESTLoginHelper"], function (subject) {
                    RESTLoginHelper = subject;
                    done();
                });
        });

        describe("#filterUrlParams", function () {
            it("returns a string", function () {
                expect(RESTLoginHelper.filterUrlParams()).to.be.a("string");
            });

            it("coverts an object to parameter string", function () {
                var params = {
                    arg: "argValue",
                    locale: "localeValue"
                };

                expect(RESTLoginHelper.filterUrlParams(params)).to.eq("&arg=argValue&locale=localeValue");
            });

            it("filters out non-allowed parameters", function () {
                var params = {
                        arg: "argValue",
                        authIndexType: "authIndexTypeValue",
                        authIndexValue: "authIndexValueValue",
                        "goto": "gotoValue",
                        gotoOnFail: "gotoOnFailValue",
                        ForceAuth: "ForceAuthValue",
                        locale: "localeValue",
                        unknown: "unknown"
                    },
                    expected = "&arg=argValue&authIndexType=authIndexTypeValue&authIndexValue=authIndexValueValue" +
                               "&goto=gotoValue&gotoOnFail=gotoOnFailValue&ForceAuth=ForceAuthValue&locale=localeValue";

                expect(RESTLoginHelper.filterUrlParams(params)).to.eq(expected);
            });

            context("when all parameters are filtered out", function () {
                it("returns an empty string", function () {
                    var params = {
                        unknown: "unknown"
                    };

                    expect(RESTLoginHelper.filterUrlParams(params)).to.eq("");
                });
            });

            context("when params is \"undefined\"", function () {
                it("returns an empty string", function () {
                    expect(RESTLoginHelper.filterUrlParams(undefined)).to.eq("");
                });
            });
        });
    });
});
