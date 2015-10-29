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
 * Copyright 2015 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "sinon",
    "org/forgerock/openam/ui/common/util/Promise"
], function ($, _, sinon, Promise) {
    describe("org/forgerock/openam/ui/common/util/Promise", function () {
        describe("#all", function () {
            function whenPassed (value) {
                return function () {
                    it("rejects the promise", function (done) {
                        Promise.all(value).then(function () {
                            done(new Error("Excepted the promise to be rejected"));
                        }, function (value) {
                            expect(value).to.be.an.instanceOf(TypeError);
                            done();
                        });
                    });
                };
            }

            context("when passed null", whenPassed(null));
            context("when passed undefined", whenPassed(undefined));
            context("when passed a number", whenPassed(3));
            context("when passed an object", whenPassed({ a: 1, b: 2 }));

            context("when passed an array", function () {
                context("which is empty", function () {
                    it("resolves with empty array", function () {
                        return Promise.all([]).then(function (value) {
                            expect(value).to.be.an.instanceOf(Array).and.to.be.empty;
                        });
                    });
                });
                context("of 1 promise", function () {
                    var d, p, resolvedSpy, rejectedSpy;
                    beforeEach(function () {
                        d = $.Deferred();
                        p = Promise.all([d.promise()]);
                        resolvedSpy = sinon.spy();
                        rejectedSpy = sinon.spy();
                        p.then(resolvedSpy, rejectedSpy);
                    });
                    it("returns a pending promise while the passed in promise is pending", function () {
                        expect(resolvedSpy).to.not.be.called;
                        expect(rejectedSpy).to.not.be.called;
                    });
                    it("resolves the returned promise when the passed in promise is resolved", function () {
                        d.resolve();
                        expect(resolvedSpy).to.be.called;
                        expect(rejectedSpy).to.not.be.called;
                    });
                    it("rejects the returned promise when the passed in promise is rejected", function () {
                        d.reject();
                        expect(resolvedSpy).to.not.be.called;
                        expect(rejectedSpy).to.be.called;
                    });
                    it("resolves the promise with an array containing the value of the resolved promise", function () {
                        d.resolve(1);
                        expect(resolvedSpy).to.be.calledWith([1]);
                    });
                    it("groups multiple resolved values into an array", function () {
                        d.resolve(1, 2, 3);
                        expect(resolvedSpy).to.be.calledWith([[1, 2, 3]]);
                    });
                    it("rejects the returned promise if the passed in promise is rejected", function () {
                        d.reject();
                        expect(resolvedSpy).to.not.be.called;
                        expect(rejectedSpy).to.be.called;
                    });
                });
                context("of 2 promises", function () {
                    var d1, d2, p, resolvedSpy, rejectedSpy;
                    beforeEach(function () {
                        d1 = $.Deferred();
                        d2 = $.Deferred();
                        p = Promise.all([d1.promise(), d2.promise()]);
                        resolvedSpy = sinon.spy();
                        rejectedSpy = sinon.spy();
                        p.then(resolvedSpy, rejectedSpy);
                    });
                    it("doesn't resolve the returned promise if neither of the promises are resolved", function () {
                        expect(resolvedSpy).to.not.be.called;
                        expect(rejectedSpy).to.not.be.called;
                    });
                    it("doesn't resolve the returned promise if only the first promise is resolved", function () {
                        d1.resolve();
                        expect(resolvedSpy).to.not.be.called;
                        expect(rejectedSpy).to.not.be.called;
                    });
                    it("doesn't resolve the returned promise if only the second promise is resolved", function () {
                        d2.resolve();
                        expect(resolvedSpy).to.not.be.called;
                        expect(rejectedSpy).to.not.be.called;
                    });
                    it("resolves the returned promise when both of the promises are resolved", function () {
                        d1.resolve();
                        d2.resolve();
                        expect(resolvedSpy).to.be.called;
                        expect(rejectedSpy).to.not.be.called;
                    });
                    it("resolves the promise with an array containing the value of the resolved promise", function () {
                        d1.resolve(1);
                        d2.resolve(2);
                        expect(resolvedSpy).to.be.calledWith([1, 2]);
                    });
                    it("groups multiple resolved values into an array", function () {
                        d1.resolve(1, 2, 3);
                        d2.resolve(4);
                        expect(resolvedSpy).to.be.calledWith([[1, 2, 3], 4]);
                    });
                    it("rejects the returned promise if the first promise is rejected", function () {
                        d1.reject();
                        expect(resolvedSpy).to.not.be.called;
                        expect(rejectedSpy).to.be.called;
                    });
                    it("rejects the returned promise if the second promise is rejected", function () {
                        d2.reject();
                        expect(resolvedSpy).to.not.be.called;
                        expect(rejectedSpy).to.be.called;
                    });
                });
            });
        });
    });
});
