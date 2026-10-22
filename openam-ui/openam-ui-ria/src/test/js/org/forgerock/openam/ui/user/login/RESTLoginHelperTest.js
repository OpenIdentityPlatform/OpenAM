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
 * Portions Copyright 2026 3A Systems, LLC.
 */

define([
    "jquery",
    "squire",
    "sinon"
], ($, Squire, sinon) => {
    describe("org/forgerock/openam/ui/user/login/RESTLoginHelper", () => {
        let RESTLoginHelper;

        before((done) => {
            const injector = new Squire();

            injector
                .mock("org/forgerock/openam/ui/user/services/AuthNService", {})
                .mock("org/forgerock/openam/ui/user/UserModel", {})
                .mock("org/forgerock/commons/ui/common/main/ViewManager", {})
                .require(["org/forgerock/openam/ui/user/login/RESTLoginHelper"], (subject) => {
                    RESTLoginHelper = subject;
                    done();
                });
        });

        describe("#filterUrlParams", () => {
            it("returns a string", () => {
                expect(RESTLoginHelper.filterUrlParams()).to.be.a("string");
            });

            it("coverts an object to parameter string", () => {
                const params = {
                    arg: "argValue",
                    locale: "localeValue"
                };

                expect(RESTLoginHelper.filterUrlParams(params)).to.eq("&arg=argValue&locale=localeValue");
            });

            it("filters out non-allowed parameters", () => {
                const params = {
                    arg: "argValue",
                    authIndexType: "authIndexTypeValue",
                    authIndexValue: "authIndexValueValue",
                    "goto": "gotoValue",
                    gotoOnFail: "gotoOnFailValue",
                    ForceAuth: "ForceAuthValue",
                    locale: "localeValue",
                    unknown: "unknown"
                };
                const expected = "&arg=argValue&authIndexType=authIndexTypeValue&authIndexValue=authIndexValueValue" +
                               "&goto=gotoValue&gotoOnFail=gotoOnFailValue&ForceAuth=ForceAuthValue&locale=localeValue";

                expect(RESTLoginHelper.filterUrlParams(params)).to.eq(expected);
            });

            context("when all parameters are filtered out", () => {
                it("returns an empty string", () => {
                    const params = {
                        unknown: "unknown"
                    };

                    expect(RESTLoginHelper.filterUrlParams(params)).to.eq("");
                });
            });

            context("when params is \"undefined\"", () => {
                it("returns an empty string", () => {
                    expect(RESTLoginHelper.filterUrlParams(undefined)).to.eq("");
                });
            });
        });
    });

    describe("org/forgerock/openam/ui/user/login/RESTLoginHelper#setSuccessURL", () => {
        const OFF_SITE_GOTO = "https%3A%2F%2Fevil.example%2F";
        const SUCCESS_URL = "https://openam.example.com/openam/console";

        let RESTLoginHelper;
        let AuthNService;
        let Configuration;

        beforeEach((done) => {
            const injector = new Squire();

            AuthNService = { validateGotoUrl: sinon.stub() };
            // handleParams in RESTLoginView has already stored the goto of the address bar, unvalidated.
            Configuration = { globalData: { auth: { urlParams: { "goto": OFF_SITE_GOTO } } } };

            injector
                .mock("org/forgerock/openam/ui/user/services/AuthNService", AuthNService)
                .mock("org/forgerock/openam/ui/user/UserModel", {})
                .mock("org/forgerock/commons/ui/common/main/ViewManager", {})
                .mock("org/forgerock/commons/ui/common/main/Configuration", Configuration)
                .mock("org/forgerock/commons/ui/common/util/Constants", {
                    context: "openam",
                    CONSOLE_PATH: "/openam/console"
                })
                .mock("org/forgerock/commons/ui/common/util/URIUtils", {
                    getCurrentCompositeQueryString: () => `realm=%2F&goto=${OFF_SITE_GOTO}`
                })
                .require(["org/forgerock/openam/ui/user/login/RESTLoginHelper"], (subject) => {
                    RESTLoginHelper = subject;
                    done();
                });
        });

        function gotoAfterSetSuccessURL () {
            return RESTLoginHelper.setSuccessURL("token", SUCCESS_URL).then(
                () => Configuration.globalData.auth.urlParams.goto);
        }

        it("asks the server to validate the goto of the address bar", () => {
            AuthNService.validateGotoUrl.returns($.Deferred().resolve({ successURL: SUCCESS_URL }).promise());

            return gotoAfterSetSuccessURL().then(() => {
                expect(AuthNService.validateGotoUrl).to.have.been.calledWith(OFF_SITE_GOTO);
            });
        });

        it("replaces the goto of the address bar with the URL the server validated", () => {
            AuthNService.validateGotoUrl.returns(
                $.Deferred().resolve({ successURL: "https://app.example.com/" }).promise());

            return gotoAfterSetSuccessURL().then((goto) => {
                expect(goto).to.eq(encodeURIComponent("https://app.example.com/"));
            });
        });

        it("falls back to the success URL when the server cannot validate the goto", () => {
            AuthNService.validateGotoUrl.returns($.Deferred().reject().promise());

            return gotoAfterSetSuccessURL().then((goto) => {
                expect(goto).to.eq(SUCCESS_URL);
            });
        });

        it("falls back to the success URL when the server names no URL", () => {
            AuthNService.validateGotoUrl.returns($.Deferred().resolve({ successURL: null }).promise());

            return gotoAfterSetSuccessURL().then((goto) => {
                expect(goto).to.eq(SUCCESS_URL);
            });
        });

        it("falls back to the success URL when the validated URL is not an http(s) one", () => {
            AuthNService.validateGotoUrl.returns(
                $.Deferred().resolve({ successURL: "javascript:alert(document.domain)" }).promise());

            return gotoAfterSetSuccessURL().then((goto) => {
                expect(goto).to.eq(SUCCESS_URL);
            });
        });
    });

    describe("org/forgerock/openam/ui/user/login/RESTLoginHelper#login", () => {
        const OFF_SITE_GOTO_ON_FAIL = "https://evil.example/";
        const FAILURE_URL = "https://openam.example.com/openam/XUI/#login/failed";
        // Built rather than written out, so that no linter takes the test data for a script URL.
        const SCRIPT_URL = `${"javascript"}:alert(document.domain)`;

        let RESTLoginHelper;
        let AuthNService;
        let Configuration;
        let SessionToken;

        beforeEach((done) => {
            const injector = new Squire();

            AuthNService = {
                getRequirements: sinon.stub().returns($.Deferred().resolve({ callbacks: [] }).promise()),
                submitRequirements: sinon.stub(),
                resetProcess: sinon.stub()
            };
            SessionToken = { isAuthenticated: sinon.stub().returns(false) };
            // handleParams in RESTLoginView has already stored the gotoOnFail of the address bar, unvalidated.
            Configuration = {
                globalData: { auth: { urlParams: { gotoOnFail: OFF_SITE_GOTO_ON_FAIL } } },
                setProperty: sinon.stub()
            };

            injector
                .mock("org/forgerock/openam/ui/user/services/AuthNService", AuthNService)
                .mock("org/forgerock/openam/ui/user/UserModel", {})
                .mock("org/forgerock/commons/ui/common/main/ViewManager", { refresh: sinon.stub() })
                .mock("org/forgerock/commons/ui/common/main/Configuration", Configuration)
                .mock("org/forgerock/openam/ui/user/login/tokens/SessionToken", SessionToken)
                .require(["org/forgerock/openam/ui/user/login/RESTLoginHelper"], (subject) => {
                    RESTLoginHelper = subject;
                    done();
                });
        });

        // Resolves with what CommonConfig would read from urlParams when the failure callback runs.
        function gotoOnFailWhenTheLoginFails () {
            const failed = $.Deferred();
            RESTLoginHelper.login({}, () => failed.reject("the login succeeded"), (reason) => {
                failed.resolve({
                    present: Configuration.globalData.auth.urlParams.hasOwnProperty("gotoOnFail"),
                    gotoOnFail: Configuration.globalData.auth.urlParams.gotoOnFail,
                    reason
                });
            });
            return failed.promise();
        }

        function failAuthenticationWith (failureUrl) {
            AuthNService.submitRequirements.returns($.Deferred().reject(1, "authenticationFailed", failureUrl)
                .promise());
        }

        it("replaces the gotoOnFail of the address bar with the failure URL the server answered with", () => {
            failAuthenticationWith(FAILURE_URL);

            return gotoOnFailWhenTheLoginFails().then((state) => {
                expect(state.gotoOnFail).to.eq(FAILURE_URL);
                expect(state.reason).to.eq("authenticationFailed");
            });
        });

        it("drops the gotoOnFail of the address bar when the server answers with no failure URL", () => {
            failAuthenticationWith(undefined);

            return gotoOnFailWhenTheLoginFails().then((state) => {
                expect(state.present).to.be.false;
            });
        });

        it("drops the gotoOnFail of the address bar when the failure URL is not an http(s) one", () => {
            failAuthenticationWith(SCRIPT_URL);

            return gotoOnFailWhenTheLoginFails().then((state) => {
                expect(state.present).to.be.false;
            });
        });

        it("adds no gotoOnFail when the address bar had none", () => {
            delete Configuration.globalData.auth.urlParams.gotoOnFail;
            failAuthenticationWith(FAILURE_URL);

            return gotoOnFailWhenTheLoginFails().then((state) => {
                expect(state.present).to.be.false;
            });
        });

        it("drops the gotoOnFail of the address bar when the session cannot be read after authentication", () => {
            SessionToken.isAuthenticated.returns(true);
            AuthNService.submitRequirements.returns($.Deferred().resolve({ tokenId: "token" }).promise());
            sinon.stub(RESTLoginHelper, "getLoggedUser", (successCallback, errorCallback) => errorCallback());

            return gotoOnFailWhenTheLoginFails().then((state) => {
                expect(state.present).to.be.false;
            });
        });
    });
});
