module.exports = function (config) {
    config.set({
        client: {
            mocha: {
                timeout : 6000
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
                presets: ["env"]
            }
        },
        reporters: ["notify", "nyan"],
        mochaReporter: {
            output: "autowatch"
        },
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
