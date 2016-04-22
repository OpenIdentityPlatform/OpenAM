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
    let RouteTo;
    let Strategy;
    let validatePromise;
    let Validator;
    describe("org/forgerock/openam/ui/common/sessions/SessionValidator", () => {
        beforeEach((done) => {
            const injector = new Squire();

            validatePromise = $.Deferred();

            Strategy = sinon.stub().returns(validatePromise);

            RouteTo = {
                sessionExpired: sinon.stub()
            };

            injector
                .mock("org/forgerock/openam/ui/common/RouteTo", RouteTo)
                .require(["org/forgerock/openam/ui/common/sessions/SessionValidator"], (subject) => {
                    Validator = subject;
                    done();
                });
        });

        describe("#start", () => {
            let clock;

            beforeEach(() => {
                clock = sinon.useFakeTimers();
            });

            afterEach(() => {
                clock.restore();
            });

            it("invokes strategy immediately", () => {
                Validator.start("token", Strategy);

                clock.tick(1000);

                expect(Strategy).be.calledOnce.calledWith("token");
            });

            context("when strategy rejects", () => {
                it("invokes RouteTo#sessionExpired", () => {
                    validatePromise.reject();

                    Validator.start("token", Strategy);

                    clock.tick(1000);

                    expect(RouteTo.sessionExpired).to.be.calledOnce;
                });
            });

            context("when invoked for the 2nd time", () => {
                beforeEach(() => {
                    Validator.start("token", Strategy);
                });

                it("throws error", () => {
                    expect(() => {
                        Validator.start("token", Strategy);
                    }).to.throw(Error, "Validator has already been started");
                });

                context("when #stop has been invoked beforehand", () => {
                    it("doesn not throw error", () => {
                        Validator.stop();

                        expect(() => {
                            Validator.start("token", Strategy);
                        }).to.not.throw(Error);
                    });
                });
            });
        });

        describe("#stop", () => {
            it("invokes #clearTimeout", () => {
                sinon.spy(window, "clearTimeout");

                Validator.stop();

                expect(clearTimeout).to.be.calledOnce;
            });

        });
    });
});
