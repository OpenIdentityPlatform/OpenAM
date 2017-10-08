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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "squire",
    "sinon"
], ($, Squire, sinon) => {
    let getTimeLeftPromise;
    let MaxIdleTimeLeftStrategy;
    let SessionService;
    describe("org/forgerock/openam/ui/common/sessions/strategies/MaxIdleTimeLeftStrategy", () => {
        beforeEach((done) => {
            const injector = new Squire();

            getTimeLeftPromise = $.Deferred();

            SessionService = {
                getTimeLeft: sinon.stub().returns(getTimeLeftPromise)
            };

            injector
                .mock("org/forgerock/openam/ui/user/services/SessionService", SessionService)
                .require(
                    ["org/forgerock/openam/ui/common/sessions/strategies/MaxIdleTimeLeftStrategy"]
                , (subject) => {
                    MaxIdleTimeLeftStrategy = subject;
                    done();
                });
        });

        it("returns a promise", () => {
            getTimeLeftPromise.resolve();
            const func = MaxIdleTimeLeftStrategy();

            expect(func.then).to.not.be.undefined;

            return func;
        });

        context("when invoked", () => {
            it("returns the idle expiration time from session service", () => {
                getTimeLeftPromise.resolve(300);
                return MaxIdleTimeLeftStrategy().then((seconds) => {
                    expect(seconds).to.be.eq(300);
                });
            });
        });
    });
});
