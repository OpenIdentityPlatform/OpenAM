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
 * Portions copyright 2025 3A Systems LLC.
 */

(function () {
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
            "backbone": "/base/target/dependencies/libs/backbone-1.1.2-min",
            "chai": "/base/node_modules/chai/chai",
            "handlebars": "/base/target/dependencies/libs/handlebars-4.7.7",
            "i18next": "/base/target/dependencies/libs/i18next-1.7.3-min",
            "jquery": "/base/target/dependencies/libs/jquery-3.7.1-min",
            "lodash": "/base/target/dependencies/libs/lodash-3.10.1-min",
            "moment": "/base/target/dependencies/libs/moment-2.28.0-min",
            "redux": "/base/target/dependencies/libs/redux-3.5.2-min",
            "sinon-chai": "/base/node_modules/sinon-chai/lib/sinon-chai",
            "sinon": "/base/target/test-classes/libs/sinon-1.15.4",
            "squire": "/base/target/test-classes/libs/squire-0.2.0"
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
}());
