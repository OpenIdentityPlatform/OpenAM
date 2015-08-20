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

/*global require, QUnit */

define([
    "jquery",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "tests/admin/common",
    "tests/admin/realms/policies",
    "tests/admin/realms/scripts"
], function ($, Constants, EventManager, CommonTests, PoliciesTests, ScriptsTests) {

    return function (server) {
        EventManager.registerListener(Constants.EVENT_APP_INITIALIZED, function () {

            QUnit.testStart(function (testDetails) {
                console.log("Starting " + testDetails.module + ": " + testDetails.name + " (" + testDetails.testNumber + ")");

                // every state needs to be reset at the start of each test
                var vm = require("org/forgerock/commons/ui/common/main/ViewManager");

                vm.currentView = null;
                vm.currentDialog = null;
                vm.currentViewArgs = null;
                vm.currentDialogArgs = null;

                localStorage.clear();
                sessionStorage.clear();

                require("org/forgerock/commons/ui/common/main/Configuration").baseTemplate = null;
            });

            QUnit.testDone(function () {
                // various widgets which get added outside of the fixture and need to be cleaned up
                $(".modal-backdrop").remove();
                $(".ui-autocomplete").remove();
                $(".ui-helper-hidden-accessible").remove();
                $("body").removeClass("modal-open");
            });

            QUnit.done(function () {
                localStorage.clear();
                sessionStorage.clear();
                Backbone.history.stop();
                window.location.hash = "";
            });

            _.delay(function () {
                QUnit.start();

                CommonTests.executeAll();
                PoliciesTests.executeAll();
                ScriptsTests.executeAll();
            }, 500);
        });
    }
});
