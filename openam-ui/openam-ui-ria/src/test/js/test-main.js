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

var TEST_REGEXP = /(spec|test)\.js$/i,
    allTestFiles = Object.keys(window.__karma__.files).filter(function (file) {
        return TEST_REGEXP.test(file);
    });

require.config({
    baseUrl: "/base/target/compiled",

    map: {
        "*": {
            // TODO: Remove this when there are no longer any references to the "underscore" dependency
            "underscore": "lodash"
        }
    },
    paths: {
        jquery: "/base/target/dependencies/libs/jquery-2.1.1-min",
        lodash: "/base/target/dependencies/libs/lodash-3.10.1-min",
        sinon: "/base/target/test-classes/libs/sinon-1.15.4",
        squire: "/base/target/test-classes/libs/squire-0.2.0",
        chai: "/base/node_modules/chai/chai",
        "sinon-chai": "/base/node_modules/sinon-chai/lib/sinon-chai"
    },
    shim: {
        "lodash": {
            exports: "_"
        }
    }
});

require(["chai", "sinon-chai"].concat(allTestFiles), function (chai, chaiSinon) {
    chai.use(chaiSinon);
    window.expect = chai.expect;
    window.__karma__.start();
});
