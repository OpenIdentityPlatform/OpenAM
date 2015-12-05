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

/* global module, require, process */

var _ = require("lodash"),
    mavenSrcPath = "/src/main/js",
    mavenTestPath = "/src/test/js";

function mavenProjectSource (projectDir) {
    return [
        projectDir + mavenSrcPath,
        projectDir + "/src/main/resources"
    ];
}

function mavenProjectTestSource (projectDir) {
    return [
        projectDir + mavenTestPath,
        projectDir + "/src/test/resources"
    ];
}

module.exports = function (grunt) {
    var compositionDirectory = "target/composed",
        compiledDirectory = "target/compiled",
        testClassesDirectory = "target/test-classes",
        forgeRockCommonsDirectory = process.env.FORGEROCK_UI_SRC + "/forgerock-ui-commons",
        forgeRockUiDirectory = process.env.FORGEROCK_UI_SRC + "/forgerock-ui-user",
        targetVersion = grunt.option("target-version") || "dev",
        buildCompositionDirs = _.flatten([
            "target/dependencies",
            // When building, dependencies are downloaded and expanded by Maven
            "target/dependencies-expanded/forgerock-ui-user",
            // This must come last so that it overwrites any conflicting files!
            mavenProjectSource(".")
        ]),
        watchCompositionDirs = _.flatten([
            // When watching, we want to get the dependencies directly from the source
            mavenProjectSource(forgeRockCommonsDirectory),
            mavenProjectSource(forgeRockUiDirectory),
            // This must come last so that it overwrites any conflicting files!
            mavenProjectSource(".")
        ]),
        testWatchDirs = _.flatten([
            mavenProjectTestSource(".")
        ]),
        testInputDirs = _.flatten([
            mavenProjectTestSource(".")
        ]),
        nonCompiledFiles = [
            "**/*.html",
            "**/*.ico",
            "**/*.json",
            "**/*.png",
            "**/*.js",
            "**/*.eot",
            "**/*.svg",
            "**/*.woff",
            "**/*.woff2",
            "**/*.otf",
            "css/bootstrap-3.3.5-custom.css",
            "themes/**/*.*"
        ],
        serverDeployDirectory = process.env.OPENAM_HOME + "/XUI";

    grunt.initConfig({
        copy: {
            /**
             * Copy all the sources and resources from this project and all dependencies into the composition directory.
             *
             * TODO: This copying shouldn't really be necessary, but is required because the dependencies are all over
             * the place. If we move to using npm for our dependencies, this can be greatly simplified.
             */
            compose: {
                files: buildCompositionDirs.map(function (dir) {
                    return {
                        expand: true,
                        cwd: dir,
                        src: ["**"],
                        dest: compositionDirectory
                    };
                })
            },
            /**
             * Copy files that do not need to be compiled into the compiled directory.
             */
            compiled: {
                files: [{
                    expand: true,
                    cwd: compositionDirectory,
                    src: nonCompiledFiles.concat([
                        "!main.js", // Output by r.js
                        "!index.html" // Output by grunt-text-replace
                    ]),
                    dest: compiledDirectory
                }]
            }
        },
        eslint: {
            /**
             * Check the JavaScript source code for common mistakes and style issues.
             */
            lint: {
                src: [
                    "." + mavenSrcPath + "/**/*.js",
                    "!." + mavenSrcPath + "/libs/**/*.js",
                    "." + mavenTestPath + "/**/*.js"
                ],
                options: {
                    format: require.resolve("eslint-formatter-warning-summary")
                }
            }
        },
        karma: {
            options: {
                configFile: "karma.conf.js"
            },
            build: {
                singleRun: true,
                reporters: "progress"
            },
            dev: {
            }
        },
        less: {
            /**
             * Compile LESS source code into minified CSS files.
             */
            compile: {
                files: [{
                    src: compositionDirectory + "/css/structure.less",
                    dest: compiledDirectory + "/css/structure.css"
                }, {
                    src: compositionDirectory + "/css/theme.less",
                    dest: compiledDirectory + "/css/theme.css"
                }, {
                    src: compositionDirectory + "/css/styles-admin.less",
                    dest: compiledDirectory + "/css/styles-admin.css"
                }],
                options: {
                    compress: true,
                    plugins: [
                        new (require("less-plugin-clean-css"))({})
                    ],
                    relativeUrls: true
                }
            }
        },
        replace: {
            /**
             * Include the version of AM in the index file.
             *
             * This is needed to force the browser to refetch JavaScript files when a new version of AM is deployed.
             */
            buildNumber: {
                src: compositionDirectory + "/index.html",
                dest: compiledDirectory + "/index.html",
                replacements: [{
                    from: "${version}",
                    to: targetVersion
                }]
            }
        },
        requirejs: {
            /**
             * Concatenate and uglify the JavaScript.
             */
            compile: {
                options: {
                    baseUrl: compositionDirectory,
                    mainConfigFile: compositionDirectory + "/main.js",
                    out: compiledDirectory + "/main.js",
                    include: ["main"],
                    preserveLicenseComments: false,
                    generateSourceMaps: true,
                    optimize: "uglify2",
                    // These files are excluded from optimization so that the UI can be customized without having to
                    // repackage it.
                    excludeShallow: [
                        "config/AppConfiguration",
                        "config/ThemeConfiguration"
                    ]
                }
            }
        },
        /**
         * Sync is used when watching to speed up the build.
         */
        sync: {
            /**
             * Copy all the sources and resources from this project and all dependencies into the composition directory.
             */
            compose: {
                files: watchCompositionDirs.map(function (dir) {
                    return {
                        cwd: dir,
                        src: ["**"],
                        dest: compositionDirectory
                    };
                }),
                compareUsing: "md5"
            },
            /**
             * Copy files that do not need to be compiled into the compiled directory.
             *
             * Note that this also copies main.js because the requirejs step is not being performed when watching (it
             * is too slow).
             */
            compiled: {
                files: [{
                    cwd: compositionDirectory,
                    src: nonCompiledFiles.concat([
                        "!index.html" // Output by grunt-text-replace
                    ]),
                    dest: compiledDirectory
                }],
                compareUsing: "md5"
            },
            /**
             * Copy the test source files into the test-classes target directory.
             */
            test: {
                files: testInputDirs.map(function (inputDirectory) {
                    return {
                        cwd: inputDirectory,
                        src: ["**"],
                        dest: testClassesDirectory
                    };
                }),
                verbose: true,
                compareUsing: "md5" // Avoids spurious syncs of touched, but otherwise unchanged, files (e.g. CSS)
            },
            /**
             * Copy the compiled files to the server deploy directory.
             */
            server: {
                files: [{
                    cwd: compiledDirectory,
                    src: ["**"],
                    dest: serverDeployDirectory
                }],
                verbose: true,
                compareUsing: "md5" // Avoids spurious syncs of touched, but otherwise unchanged, files (e.g. CSS)
            }
        },
        watch: {
            /**
             * Redeploy whenever any source files change.
             */
            source: {
                files: watchCompositionDirs.concat(testWatchDirs).map(function (dir) {
                    return dir + "/**";
                }),
                tasks: ["deploy"]
            }
        }
    });

    grunt.loadNpmTasks("grunt-contrib-copy");
    grunt.loadNpmTasks("grunt-contrib-less");
    grunt.loadNpmTasks("grunt-contrib-requirejs");
    grunt.loadNpmTasks("grunt-contrib-watch");
    grunt.loadNpmTasks("grunt-eslint");
    grunt.loadNpmTasks("grunt-karma");
    grunt.loadNpmTasks("grunt-sync");
    grunt.loadNpmTasks("grunt-text-replace");

    /**
     * Resync the compiled directory and deploy to the web server.
     */
    grunt.registerTask("deploy", [
        "sync:compose",
        "less",
        "replace",
        "sync:compiled",
        "sync:test",
        "sync:server"
    ]);

    /**
     * Rebuild the compiled directory. Maven then packs this directory into the final archive artefact.
     */
    grunt.registerTask("build", [
        "copy:compose",
        "eslint",
        "requirejs",
        "less",
        "replace",
        "copy:compiled",
        "karma:build"
    ]);

    grunt.registerTask("dev", ["copy:compose", "deploy", "watch"]);
    grunt.registerTask("prod", ["build"]);

    grunt.registerTask("default", ["dev"]);
};
