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

require.config({
    paths: {
        jquery: "libs/jquery-2.1.1-min",
        "bootstrap-dialog": "libs/bootstrap-dialog-1.34.4-min",
        sinon: "sinon-1.15.4",
        squire: "squire-0.2.0"
    },
    shim: {
        sinon: {
            exports: "sinon"
        }
    }
});

require([
    "mock/admin/common",
    "mock/admin/realms/realms",
    "mock/admin/realms/policies",
    "mock/admin/realms/scripts",
    "sinon"
], function (Common, Realms, Policies, Scripts, sinon) {

    sinon.FakeXMLHttpRequest.useFilters = true;
    sinon.FakeXMLHttpRequest.addFilter(function (method, url, async, username, password) {
        return (/((\.html)|(\.css)|(\.less)|(\.json))$/).test(url);
    });

    var server = sinon.fakeServer.create();
    server.autoRespond = true;

    Common(server);
    Realms(server);
    // Policies(server); TODO: tests for policies are not working
    Scripts(server);

    require(["main", "run"], function (appMain, run) {
        run(server);
    });
});
