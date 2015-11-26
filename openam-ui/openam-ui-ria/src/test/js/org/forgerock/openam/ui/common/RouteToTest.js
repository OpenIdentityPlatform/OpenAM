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
    "squire",
    "sinon",
    "org/forgerock/openam/ui/common/util/Constants"
], function ($, Squire, sinon, Constants) {
    var Configuration, EventManager, Router, RouteTo, SessionManager;
    describe("org/forgerock/openam/ui/common/RouteTo", function () {
        beforeEach(function (done) {
            var injector = new Squire();

            Configuration = {
                globalData: {
                    authorizationFailurePending: true
                },
                setProperty: sinon.stub()
            };

            EventManager = {
                sendEvent: sinon.stub()
            };

            Router = {
                configuration: {
                    routes: {
                        login: {
                            url: "loginUrl"
                        }
                    }
                },
                getCurrentHash: sinon.stub().returns("page")
            };

            SessionManager = {
                logout: sinon.stub()
            };

            injector.mock("org/forgerock/commons/ui/common/main/Configuration", Configuration)
                    .mock("org/forgerock/commons/ui/common/main/EventManager", EventManager)
                    .mock("org/forgerock/commons/ui/common/main/Router", Router)
                    .mock("org/forgerock/commons/ui/common/main/SessionManager", SessionManager)
                    .require(["org/forgerock/openam/ui/common/RouteTo"], function (subject) {
                        RouteTo = subject;
                        done();
                    });
        });

        describe("#setGoToUrlProperty", function () {
            context("when a gotoURL is not set and the current hash does not match the login route's URL", function () {
                it("sets the gotoURL to be the current hash", function () {
                    RouteTo.setGoToUrlProperty();

                    expect(Configuration.setProperty).to.be.calledOnce.calledWith("gotoURL", "#page");
                });
            });
        });

        describe("#forbiddenPage", function () {
            it("deletes \"authorizationFailurePending\" attribute Configuration.globalData", function () {
                RouteTo.forbiddenPage();

                expect(Configuration.globalData).to.not.have.ownProperty("authorizationFailurePending");
            });
            it("sends EVENT_CHANGE_VIEW event", function () {
                RouteTo.forbiddenPage();

                expect(EventManager.sendEvent).to.be.calledOnce.calledWith(Constants.EVENT_CHANGE_VIEW, {
                    route: {
                        view: "org/forgerock/openam/ui/common/views/error/ForbiddenView",
                        url: /.*/
                    },
                    fromRouter: true
                });
            });
        });

        describe("#forbiddenError", function () {
            it("sends EVENT_DISPLAY_MESSAGE_REQUEST event", function () {
                RouteTo.forbiddenError();

                expect(EventManager.sendEvent).to.be.calledOnce.calledWith(Constants.EVENT_DISPLAY_MESSAGE_REQUEST,
                    "unauthorized");
            });
        });

        describe("#logout", function () {
            var promise;

            beforeEach(function () {
                promise = $.Deferred();
                SessionManager.logout = sinon.stub().returns(promise);
                sinon.spy(RouteTo, "setGoToUrlProperty");
            });

            afterEach(function () {
                RouteTo.setGoToUrlProperty.restore();
            });

            it("invokes #setGoToUrlProperty", function () {
                RouteTo.logout();

                expect(RouteTo.setGoToUrlProperty).to.be.calledOnce;
            });

            context("when logout is successful", function () {
                it("sends EVENT_AUTHENTICATION_DATA_CHANGED event", function () {
                    promise.resolve();

                    RouteTo.logout();

                    expect(EventManager.sendEvent).to.be.calledWith(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, {
                        anonymousMode: true
                    });
                });

                it("sends EVENT_CHANGE_VIEW event", function () {
                    promise.resolve();

                    RouteTo.logout();

                    expect(EventManager.sendEvent).to.be.calledWith(Constants.EVENT_CHANGE_VIEW, {
                        route: Router.configuration.routes.login
                    });
                });
            });

            context("when logout is unsuccessful", function () {
                it("sends no events", function () {
                    promise.fail();

                    RouteTo.logout();

                    expect(EventManager.sendEvent).to.not.be.called;
                });
            });
        });

        describe("#loginDialog", function () {
            it("sends EVENT_SHOW_LOGIN_DIALOG event", function () {
                RouteTo.loginDialog();

                expect(EventManager.sendEvent).to.be.calledOnce.calledWith(Constants.EVENT_SHOW_LOGIN_DIALOG);
            });
        });

        describe("#sessionExpired", function () {
            it("sends EVENT_SHOW_LOGIN_DIALOG event", function () {
                RouteTo.sessionExpired();

                expect(EventManager.sendEvent).to.be.calledOnce.calledWith(Constants.EVENT_CHANGE_VIEW, {
                    route: Router.configuration.routes.sessionExpired
                });
            });
        });
    });
});
