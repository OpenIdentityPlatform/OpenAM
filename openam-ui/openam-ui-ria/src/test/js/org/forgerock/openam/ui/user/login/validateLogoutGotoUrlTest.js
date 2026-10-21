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
 * Copyright 2026 3A Systems, LLC.
 */

define([
    "jquery",
    "squire",
    "sinon"
], ($, Squire, sinon) => {
    const OFF_SITE = "https://app.example.com/";
    const OWN_ORIGIN = `${window.location.origin}/openam/XUI/`;
    // Built rather than written out, so that no linter takes the test data for a script URL.
    const SCRIPT_URL = `${"javascript"}:alert(document.domain)`;

    describe("org/forgerock/openam/ui/user/login/validateLogoutGotoUrl", () => {
        let validate;
        let SessionToken;
        let SessionService;
        let AuthNService;

        beforeEach((done) => {
            const injector = new Squire();

            SessionToken = { get: sinon.stub() };
            SessionService = { isSessionValid: sinon.stub() };
            AuthNService = { validateGotoUrl: sinon.stub() };

            injector
                .mock("org/forgerock/openam/ui/user/login/tokens/SessionToken", SessionToken)
                .mock("org/forgerock/openam/ui/user/services/SessionService", SessionService)
                .mock("org/forgerock/openam/ui/user/services/AuthNService", AuthNService)
                .require(["org/forgerock/openam/ui/user/login/validateLogoutGotoUrl"], (subject) => {
                    validate = subject.default;
                    done();
                });
        });

        function withSession () {
            SessionToken.get.returns("token");
            SessionService.isSessionValid.returns($.Deferred().resolve(true).promise());
        }

        it("answers nothing when there is no goto", () => {
            return validate(undefined).then((url) => {
                expect(url).to.be.undefined;
                expect(AuthNService.validateGotoUrl).not.to.have.been.called;
            });
        });

        describe("without a session", () => {
            it("follows a goto on this server", () => {
                return validate(encodeURIComponent(OWN_ORIGIN)).then((url) => {
                    expect(url).to.eq(OWN_ORIGIN);
                });
            });

            it("follows a relative goto", () => {
                return validate(encodeURIComponent("/openam/XUI/#login/")).then((url) => {
                    expect(url).to.eq("/openam/XUI/#login/");
                });
            });

            it("does not follow a goto leaving this server, and asks no server to judge it", () => {
                return validate(encodeURIComponent(OFF_SITE)).then((url) => {
                    expect(url).to.be.undefined;
                    expect(AuthNService.validateGotoUrl).not.to.have.been.called;
                });
            });

            it("does not follow a protocol relative goto", () => {
                return validate(encodeURIComponent("//evil.example/")).then((url) => {
                    expect(url).to.be.undefined;
                });
            });

            it("does not follow a script URL", () => {
                return validate(encodeURIComponent(SCRIPT_URL)).then((url) => {
                    expect(url).to.be.undefined;
                });
            });

            it("does not ask the server when the session it holds has ended", () => {
                SessionToken.get.returns("token");
                SessionService.isSessionValid.returns($.Deferred().resolve(false).promise());

                return validate(encodeURIComponent(OFF_SITE)).then((url) => {
                    expect(url).to.be.undefined;
                    expect(AuthNService.validateGotoUrl).not.to.have.been.called;
                });
            });
        });

        describe("with a session", () => {
            beforeEach(withSession);

            it("follows a goto leaving this server once the server accepts it", () => {
                AuthNService.validateGotoUrl.returns($.Deferred().resolve({ successURL: OFF_SITE }).promise());

                return validate(encodeURIComponent(OFF_SITE)).then((url) => {
                    expect(AuthNService.validateGotoUrl).to.have.been.calledWith(encodeURIComponent(OFF_SITE));
                    expect(url).to.eq(OFF_SITE);
                });
            });

            it("does not follow a goto the server replaced with another URL", () => {
                AuthNService.validateGotoUrl.returns(
                    $.Deferred().resolve({ successURL: "/openam/console" }).promise());

                return validate(encodeURIComponent(OFF_SITE)).then((url) => {
                    expect(url).to.be.undefined;
                });
            });

            it("holds the goto to this server when the server cannot validate it", () => {
                AuthNService.validateGotoUrl.returns($.Deferred().reject().promise());

                return validate(encodeURIComponent(OFF_SITE)).then((url) => {
                    expect(url).to.be.undefined;
                });
            });

            it("still follows a goto on this server when the server cannot validate it", () => {
                AuthNService.validateGotoUrl.returns($.Deferred().reject().promise());

                return validate(encodeURIComponent(OWN_ORIGIN)).then((url) => {
                    expect(url).to.eq(OWN_ORIGIN);
                });
            });

            it("does not follow a script URL even when the server echoes it", () => {
                AuthNService.validateGotoUrl.returns($.Deferred().resolve({ successURL: SCRIPT_URL }).promise());

                return validate(encodeURIComponent(SCRIPT_URL)).then((url) => {
                    expect(url).to.be.undefined;
                });
            });
        });
    });
});
