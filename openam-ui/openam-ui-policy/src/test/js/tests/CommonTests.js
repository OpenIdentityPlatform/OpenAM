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

/*global require, define, QUnit, $ */

define([
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants"
], function (Configuration, EventManager, Router, Constants) {

    return {
        executeAll: function () {
            module('Common');

            QUnit.asyncTest('Unauthorized GET Request', function () {
                var viewManager = require('org/forgerock/commons/ui/common/main/ViewManager');
                Configuration.loggedUser = {"roles": ["ui-admin"]};
                EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                    route: Router.configuration.routes.manageApps,
                    callback: function () {
                        sinon.stub(viewManager, 'showDialog', function () {
                            QUnit.ok(true, "Login dialog is shown");
                            QUnit.start();
                            delete Configuration.globalData.authorizationFailurePending;
                            viewManager.showDialog.restore();
                        });

                        EventManager.sendEvent(Constants.EVENT_UNAUTHORIZED, {error: {type: "GET"} });
                    }
                });
            });

            QUnit.asyncTest('Unauthorized POST Request', function () {
                var viewManager = require('org/forgerock/commons/ui/common/main/ViewManager');

                sinon.stub(viewManager, 'showDialog', function () {
                    QUnit.ok(true, "Login dialog is shown");
                    QUnit.start();
                    delete Configuration.globalData.authorizationFailurePending;
                    viewManager.showDialog.restore();
                });

                QUnit.ok(!viewManager.showDialog.called, "Login Dialog render function has not yet been called");
                Configuration.loggedUser = {"roles": ["ui-admin"]};
                EventManager.sendEvent(Constants.EVENT_UNAUTHORIZED, {error: {type: "POST"} });
                QUnit.ok(Configuration.loggedUser !== null, "User info should be retained after UNAUTHORIZED POST error");
            });

            QUnit.test('Add/Edit routes with different input', function () {
                QUnit.equal(Router.getLink(Router.configuration.routes.editApp, [null]), "app/", "Add App - no arguments provided");
                QUnit.equal(Router.getLink(Router.configuration.routes.editApp, ["calendar"]), "app/calendar", "Edit App with one argument provided");
                QUnit.equal(Router.getLink(Router.configuration.routes.editApp, ["test spaces"]), "app/test spaces", "Edit App with space in the name");
                QUnit.equal(Router.getLink(Router.configuration.routes.editPolicy, ["calendar", null]), "app/calendar/policy/", "Add policy with one argument provided");
                QUnit.equal(Router.getLink(Router.configuration.routes.editPolicy, ["calendar", "testPolicy"]), "app/calendar/policy/testPolicy", "Edit policy with two arguments provided");
            });
        }
    }
});