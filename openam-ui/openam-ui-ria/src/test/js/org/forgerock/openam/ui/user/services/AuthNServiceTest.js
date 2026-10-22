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
    describe("org/forgerock/openam/ui/user/services/AuthNService#submitRequirements", () => {
        // Built rather than written out, so that no linter takes the test data for a script URL. Were it followed,
        // it would set the flag rather than replace the test page.
        const SCRIPT_URL = `${"javascript"}:void(window.authNServiceFollowedTheFailureUrl = true)`;

        let AuthNService;

        beforeEach((done) => {
            const injector = new Squire();

            delete window.authNServiceFollowedTheFailureUrl;

            injector
                .mock("org/forgerock/commons/ui/common/components/Messages", {
                    addMessage: sinon.stub(),
                    TYPE_DANGER: "error"
                })
                .mock("org/forgerock/commons/ui/common/main/Configuration", { globalData: { auth: {} } })
                .mock("org/forgerock/commons/ui/common/main/EventManager", { sendEvent: sinon.stub() })
                .mock("org/forgerock/commons/ui/common/util/URIUtils", {
                    getCurrentFragmentQueryString: () => ""
                })
                .mock("org/forgerock/openam/ui/common/services/fetchUrl", { "default": (path) => path })
                .mock("org/forgerock/openam/ui/user/login/tokens/AuthenticationToken", { get: sinon.stub() })
                .mock("org/forgerock/openam/ui/user/login/tokens/SessionToken", { get: sinon.stub() })
                .mock("store/actions/creators", {})
                .mock("store/index", { "default": { getState: () => ({ server: { realm: "/" } }) } })
                .require(["org/forgerock/openam/ui/user/services/AuthNService"], (subject) => {
                    AuthNService = subject;
                    done();
                });
        });

        function failAuthenticationWith (errorBody) {
            AuthNService.serviceCall = sinon.stub().returns($.Deferred().reject({
                status: 401,
                responseText: JSON.stringify(errorBody)
            }).promise());

            const failed = $.Deferred();
            AuthNService.submitRequirements({}).then(() => failed.reject("the authentication succeeded"),
                (failedStage, reason, failureUrl) => {
                    // A failure handling that threw rejects with the error alone, which no failure URL tells apart.
                    if (failedStage instanceof Error) {
                        failed.reject(failedStage);
                        return;
                    }
                    // Gives a script URL assigned to window.location the time to run, had it been assigned.
                    window.setTimeout(() => failed.resolve(failureUrl), 50);
                });
            return failed.promise();
        }

        // An http(s) failure URL is not driven here: following it would send the test page away.
        it("hands on no failure URL when the server answered with none", () => {
            return failAuthenticationWith({ code: 401, message: "Authentication Failed" }).then((handedOn) => {
                expect(handedOn).to.be.undefined;
            });
        });

        it("hands on the failure URL the server answered with, but does not follow one that is not http(s)", () => {
            return failAuthenticationWith({ code: 401, message: "Authentication Failed",
                detail: { failureUrl: SCRIPT_URL } }).then((handedOn) => {
                    expect(handedOn).to.eq(SCRIPT_URL);
                    expect(window.authNServiceFollowedTheFailureUrl).to.be.undefined;
                });
        });
    });
});
