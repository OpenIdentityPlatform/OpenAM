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
    let getMaxIdlePromise;
    let getTimeLeftPromise;
    let MaxIdleTimeLeftStrategy;
    let SessionService;
    describe("org/forgerock/openam/ui/common/sessions/strategies/MaxIdleTimeLeftStrategy", () => {
        beforeEach((done) => {
            const injector = new Squire();

            getMaxIdlePromise = $.Deferred();
            getTimeLeftPromise = $.Deferred();

            SessionService = {
                getMaxIdle: sinon.stub().returns(getMaxIdlePromise),
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

        const getMaxIdlePromisePayload = { maxidletime: 10 };

        it("returns a promise", () => {
            getMaxIdlePromise.resolve(getMaxIdlePromisePayload);
            const func = MaxIdleTimeLeftStrategy();

            expect(func.then).to.not.be.undefined;

            return func;
        });

        context("on first invocation", () => {
            it("resolves with the maximum idle time in seconds", () => {
                getMaxIdlePromise.resolve(getMaxIdlePromisePayload);

                return MaxIdleTimeLeftStrategy().then((seconds) => {
                    expect(seconds).to.be.eq(600);
                });
            });
        });

        context("on second invocation", () => {
            beforeEach(() => {
                getMaxIdlePromise.resolve(getMaxIdlePromisePayload);

                MaxIdleTimeLeftStrategy();
            });

            context("when session time left is greater than maximum idle time", () => {
                it("resolves with the maximum idle time in seconds", () => {
                    getTimeLeftPromise.resolve({ maxtime: 900 });

                    return MaxIdleTimeLeftStrategy().then((seconds) => {
                        expect(seconds).to.be.eq(600);
                    });
                });
            });

            context("when session time left is less than maximum idle time", () => {
                it("resolves with the session time left in seconds", () => {
                    getTimeLeftPromise.resolve({ maxtime: 300 });

                    return MaxIdleTimeLeftStrategy().then((seconds) => {
                        expect(seconds).to.be.eq(300);
                    });
                });
            });
        });
    });
});
