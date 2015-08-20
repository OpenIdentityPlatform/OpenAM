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

module.exports = function (grunt) {
    grunt.initConfig({
        // To fix cache issue, do not forget to update OPENAM_VERSION environment variable after release
        // (refer to the pom.xml for the correct version, e.g. 13.0.0-SNAPSHOT)
        buildNumber: process.env.OPENAM_VERSION,

        // Path to deployed OpenAM (e.g. /Users/username/tomcat/webapps/openam)
        destination: process.env.OPENAM_HOME,

        // Path to FR-UI codebase (e.g. /Users/username/forgerock-ui)
        forgerockui: process.env.FORGEROCK_UI_SRC,

        replace: {
            ria_html: {
                src: ["openam-ui-ria/src/main/resources/index.html"],
                dest: "<%= destination %>/XUI/index.html",
                replacements: [
                    {
                        from: "${version}",
                        to: "<%= buildNumber %>"
                    }
                ]
            },
            ria_style: {
                src: ["openam-ui-ria/src/main/resources/css/styles.less"],
                dest: "<%= destination %>/XUI/css/styles.less",
                replacements: [
                    {
                        from: "${version}",
                        to: "<%= buildNumber %>"
                    }
                ]
            }
        },

        sync: {
            source_to_tomcat: {
                files: [
                    {
                        cwd: "<%= forgerockui %>/forgerock-ui-commons/src/main/js",
                        src: ["**/*"],
                        dest: "<%= destination %>/XUI"
                    },
                    {
                        cwd: "<%= forgerockui %>/forgerock-ui-commons/src/main/resources",
                        src: ["**/*"],
                        dest: "<%= destination %>/XUI"
                    },
                    {
                        cwd: "<%= forgerockui %>/forgerock-ui-user/src/main/js",
                        src: ["**/*"],
                        dest: "<%= destination %>/XUI"
                    },
                    {
                        cwd: "<%= forgerockui %>/forgerock-ui-user/src/main/resources",
                        src: ["**/*"],
                        dest: "<%= destination %>/XUI"
                    },
                    {
                        cwd: "openam-ui-common/src/main/resources",
                        src: ["**/*"],
                        dest: "<%= destination %>/XUI"
                    },
                    {
                        cwd: "openam-ui-common/src/main/js",
                        src: ["**/*"],
                        dest: "<%= destination %>/XUI"
                    },

                    {
                        cwd: "openam-ui-ria/src/main/resources",
                        src: ["**/*"],
                        dest: "<%= destination %>/XUI"
                    },
                    {
                        cwd: "openam-ui-ria/src/main/js",
                        src: ["**/*"],
                        dest: "<%= destination %>/XUI"
                    }
                ],
                verbose: true
            },

            source_to_test: {
                files: [
                    {
                        cwd: "<%= forgerockui %>/forgerock-ui-commons/src/main/js",
                        src: ["**/*"],
                        dest: "<%= destination %>/../openam-test"
                    },
                    {
                        cwd: "<%= forgerockui %>/forgerock-ui-commons/src/main/resources",
                        src: ["**/*"],
                        dest: "<%= destination %>/../openam-test"
                    },
                    {
                        cwd: "<%= forgerockui %>/forgerock-ui-user/src/main/js",
                        src: ["**/*"],
                        dest: "<%= destination %>/../openam-test"
                    },
                    {
                        cwd: "<%= forgerockui %>/forgerock-ui-user/src/main/resources",
                        src: ["**/*"],
                        dest: "<%= destination %>/../openam-test"
                    },
                    {
                        cwd: "openam-ui-common/src/main/resources",
                        src: ["**/*"],
                        dest: "<%= destination %>/../openam-test"
                    },
                    {
                        cwd: "openam-ui-common/src/main/js",
                        src: ["**/*"],
                        dest: "<%= destination %>/../openam-test"
                    },
                    {
                        cwd: "openam-ui-common/src/test/resources",
                        src: ["**/*"],
                        dest: "<%= destination %>/../openam-test"
                    },
                    {
                        cwd: "openam-ui-common/src/test/js",
                        src: ["**/*"],
                        dest: "<%= destination %>/../openam-test"
                    },
                    {
                        cwd: "openam-ui-ria/src/main/resources",
                        src: ["**/*"],
                        dest: "<%= destination %>/../openam-test"
                    },
                    {
                        cwd: "openam-ui-ria/src/main/js",
                        src: ["**/*"],
                        dest: "<%= destination %>/../openam-test"
                    },
                    {
                        cwd: "openam-ui-ria/src/test/resources",
                        src: ["**/*"],
                        dest: "<%= destination %>/../openam-test"
                    },
                    {
                        cwd: "openam-ui-ria/src/test/js",
                        src: ["**/*"],
                        dest: "<%= destination %>/../openam-test"
                    },
                    {
                        cwd: "<%= destination %>/XUI/libs",
                        src: ["**"],
                        dest: "<%= destination %>/../openam-test/libs"
                    },
                    {
                        cwd: "openam-ui-ria/target/test-classes/libs",
                        src: ["**/*"],
                        dest: "<%= destination %>/../openam-test/libs"
                    }

                ],
                verbose: true
            },

            css_to_test: {
                files: [
                    {
                        cwd: "<%= destination %>/XUI/css",
                        src: ["**"],
                        dest: "<%= destination %>/../openam-test/css"
                    }
                ],
                verbose: true
            }
        },

        less: {
            xui: {
                files: {
                    "<%= destination %>/XUI/css/styles.css": "<%= destination %>/XUI/css/styles.less"
                }
            }
        },

        watch: {
            fr_ui_commons: {
                files: [
                    "<%= forgerockui %>/forgerock-ui-commons/src/main/js/**",
                    "<%= forgerockui %>/forgerock-ui-commons/src/main/resources/**",
                    "!<%= forgerockui %>/forgerock-ui-commons/src/main/resources/css/**/*.less"
                ],
                tasks: ["sync", "qunit"]
            },
            fr_ui_user: {
                files: [
                    "<%= forgerockui %>/forgerock-ui-user/src/main/js/**",
                    "<%= forgerockui %>/forgerock-ui-user/src/main/resources/**",
                    "!<%= forgerockui %>/forgerock-ui-user/src/main/resources/css/**/*.less"
                ],
                tasks: ["sync", "qunit"]
            },
            openam_ui_common: {
                files: [
                    "openam-ui-common/src/main/js/**",
                    "openam-ui-common/src/test/js/**",

                    "openam-ui-common/src/main/resources/**",
                    "openam-ui-common/src/test/resources/**",

                    "!openam-ui-common/src/main/resources/css/**/*.less",
                    "!openam-ui-common/src/test/resources/css/**/*.less"
                ],
                tasks: ["sync", "qunit"]
            },
            openam_ui_ria: {
                files: [
                    "openam-ui-ria/src/main/js/**",
                    "openam-ui-ria/src/test/js/**",

                    "openam-ui-ria/src/main/resources/**",
                    "openam-ui-ria/src/test/resources/**",

                    "!openam-ui-ria/src/main/resources/css/**/*.less",
                    "!openam-ui-ria/src/test/resources/css/**/*.less"
                ],
                tasks: ["sync", "replace", "qunit"]
            },
            less_files: {
                files: [
                    "<%= forgerockui %>/forgerock-ui-commons/src/main/resources/css/**/*.less",
                    "<%= forgerockui %>/forgerock-ui-user/src/main/resources/css/**/*.less",

                    "openam-ui-common/src/main/resources/css/**/*.less",
                    "openam-ui-common/src/test/resources/css/**/*.less",

                    "openam-ui-ria/src/main/resources/css/**/*.less",
                    "openam-ui-ria/src/test/resources/css/**/*.less"
                ],
                tasks: ["sync", "less", "sync:css_to_test"]
            }
        },

        qunit: {
            all: ["<%= destination %>/../openam-test/qunit.html"]
        },

        notify_hooks: {
            options: {
                enabled: true,
                title: "OpenAM XUI Tests"
            }
        }
    });

    grunt.loadNpmTasks("grunt-contrib-qunit");
    grunt.loadNpmTasks("grunt-contrib-watch");
    grunt.loadNpmTasks("grunt-notify");
    grunt.loadNpmTasks("grunt-sync");
    grunt.loadNpmTasks("grunt-text-replace");
    grunt.loadNpmTasks("grunt-contrib-less");

    grunt.registerTask("default", [
        "sync:source_to_tomcat",
        "sync:source_to_test",
        "replace",
        "less",
        "sync:css_to_test",
        "qunit",
        "watch"
    ]);
};
