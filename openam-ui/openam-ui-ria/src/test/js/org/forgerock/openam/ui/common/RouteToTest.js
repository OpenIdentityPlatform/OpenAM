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
    "sinon",
    "org/forgerock/openam/ui/common/util/Constants"
], ($, Squire, sinon, Constants) => {
    let Configuration;
    let EventManager;
    let Router;
    let RouteTo;
    let SessionManager;
    describe("org/forgerock/openam/ui/common/RouteTo", () => {
        beforeEach((done) => {
            const injector = new Squire();

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
                    .require(["org/forgerock/openam/ui/common/RouteTo"], (subject) => {
                        RouteTo = subject;
                        done();
                    });
        });

        describe("#setGoToUrlProperty", () => {
            context("when a gotoURL is not set and the current hash does not match the login route's URL", () => {
                it("sets the gotoURL to be the current hash", () => {
                    RouteTo.setGoToUrlProperty();

                    expect(Configuration.setProperty).to.be.calledOnce.calledWith("gotoURL", "#page");
                });
            });
        });

        describe("#forbiddenPage", () => {
            it("deletes \"authorizationFailurePending\" attribute Configuration.globalData", () => {
                RouteTo.forbiddenPage();

                expect(Configuration.globalData).to.not.have.ownProperty("authorizationFailurePending");
            });
            it("sends EVENT_CHANGE_VIEW event", () => {
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

        describe("#forbiddenError", () => {
            it("sends EVENT_DISPLAY_MESSAGE_REQUEST event", () => {
                RouteTo.forbiddenError();

                expect(EventManager.sendEvent).to.be.calledOnce.calledWith(Constants.EVENT_DISPLAY_MESSAGE_REQUEST,
                    "unauthorized");
            });
        });

        describe("#logout", () => {
            let promise;

            beforeEach(() => {
                promise = $.Deferred();
                SessionManager.logout = sinon.stub().returns(promise);
                sinon.spy(RouteTo, "setGoToUrlProperty");
            });

            afterEach(() => {
                RouteTo.setGoToUrlProperty.restore();
            });

            it("invokes #setGoToUrlProperty", () => {
                RouteTo.logout();

                expect(RouteTo.setGoToUrlProperty).to.be.calledOnce;
            });

            context("when logout is successful", () => {
                it("sends EVENT_AUTHENTICATION_DATA_CHANGED event", () => {
                    promise.resolve();

                    RouteTo.logout();

                    expect(EventManager.sendEvent).to.be.calledWith(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, {
                        anonymousMode: true
                    });
                });

                it("sends EVENT_CHANGE_VIEW event", () => {
                    promise.resolve();

                    RouteTo.logout();

                    expect(EventManager.sendEvent).to.be.calledWith(Constants.EVENT_CHANGE_VIEW, {
                        route: Router.configuration.routes.login
                    });
                });
            });

            context("when logout is unsuccessful", () => {
                it("sends no events", () => {
                    promise.fail();

                    RouteTo.logout();

                    expect(EventManager.sendEvent).to.not.be.called;
                });
            });
        });

        describe("#loginDialog", () => {
            it("sends EVENT_SHOW_LOGIN_DIALOG event", () => {
                RouteTo.loginDialog();

                expect(EventManager.sendEvent).to.be.calledOnce.calledWith(Constants.EVENT_SHOW_LOGIN_DIALOG);
            });
        });

        describe("#sessionExpired", () => {
            it("sends EVENT_SHOW_LOGIN_DIALOG event", () => {
                RouteTo.sessionExpired();

                expect(EventManager.sendEvent).to.be.calledOnce.calledWith(Constants.EVENT_CHANGE_VIEW, {
                    route: Router.configuration.routes.sessionExpired
                });
            });
        });
    });
});
