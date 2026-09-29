/*
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
 * Portions copyright 2026 3A Systems LLC.
 */

module.exports = function (config) {
    config.set({
        client: {
            mocha: {
                // A "before" hook that loads the module under test through Squire can take several seconds
                // on a loaded CI runner; keep this below waitSeconds in src/test/js/test-main.js.
                timeout : 30000
            }
        },
        basePath: ".",
        frameworks: ["mocha", "requirejs"],
        files: [
            { pattern: "target/test-classes/test-main.js" },
            { pattern: "target/test-classes/**/*.js", included: false },
            { pattern: "target/compiled/**/*.js", included: false },
            { pattern: "target/dependencies/libs/**/*.js", included: false },
            { pattern: "node_modules/chai/chai.js", included: false },
            { pattern: "node_modules/sinon-chai/lib/sinon-chai.js", included: false }
        ],
        exclude: [],
        preprocessors: {
            "target/test-classes/org/**/*.js": ["babel"],
            "target/test-classes/store/**/*.js": ["babel"]
        },
        babelPreprocessor: {
            options: {
                ignore: ["libs/"],
                presets: [["@babel/preset-env", { "targets": "> 0.2%, not dead, last 2 versions" }],]
            }
        },
        reporters: ["progress"],
        port: 9876,
        colors: true,
        logLevel: config.LOG_INFO,
        autoWatch: true,
        browsers: ["chromeNoSandbox"],
        customLaunchers: {
            chromeNoSandbox: {
                base: "Chrome",
                flags: ["--headless=new",
                    "--allow-file-access-from-files",
                    "--disable-dev-shm-usage",
                    "--no-sandbox",
                    "--disable-setuid-sandbox"]
            }
        },
        singleRun: false,
        browserNoActivityTimeout: 60000
    });
};
