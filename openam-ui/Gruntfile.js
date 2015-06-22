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

module.exports = function(grunt) {
    grunt.initConfig({
        destination: process.env.OPENAM_HOME,
        forgerockui: process.env.FORGEROCK_UI_SRC,
        sync: {
            ria: {
                files: [
                    {
                        cwd: '<%= forgerockui %>/forgerock-ui-commons/src/main/js',
                        src: ['**/*'],
                        dest: '<%= destination %>/XUI'
                    },
                    {
                        cwd: '<%= forgerockui %>/forgerock-ui-commons/src/main/resources',
                        src: ['**/*'],
                        dest: '<%= destination %>/XUI'
                    },
                    {
                        cwd: '<%= forgerockui %>/forgerock-ui-user/src/main/js',
                        src: ['**/*'],
                        dest: '<%= destination %>/XUI'
                    },
                    {
                        cwd: '<%= forgerockui %>/forgerock-ui-user/src/main/resources',
                        src: ['**/*'],
                        dest: '<%= destination %>/XUI'
                    },
                    {
                        cwd: 'openam-ui-common/src/main/resources',
                        src: ['**/*'],
                        dest: '<%= destination %>/XUI'
                    },
                    {
                        cwd: 'openam-ui-common/src/main/js',
                        src: ['**/*'],
                        dest: '<%= destination %>/XUI'
                    },
                    {
                        cwd: 'openam-ui-ria/src/main/resources',
                        src: ['**/*'],
                        dest: '<%= destination %>/XUI'
                    },
                    {
                        cwd: 'openam-ui-ria/src/main/js',
                        src: ['**/*'],
                        dest: '<%= destination %>/XUI'
                    }
                ],
                verbose: true
            },

            scripts_to_test: {
                files: [
                    {
                        cwd: 'openam-ui-scripts/target/dependency',
                        src: ['**'],
                        dest: 'openam-ui-scripts/target/www'
                    },
                    {
                        cwd: 'openam-ui-scripts/target/codemirror-4.10',
                        src: ['lib/codemirror.js', 'mode/javascript/javascript.js', 'mode/groovy/groovy.js'],
                        dest: 'openam-ui-scripts/target/www/libs/codemirror'
                    },
                    {
                        cwd: 'openam-ui-scripts/test/libs',
                        src: ['**'],
                        dest: 'openam-ui-scripts/target/www/libs'
                    },
                    {
                        cwd: 'openam-ui-scripts/src/main/js',
                        src: ['**'],
                        dest: 'openam-ui-scripts/target/www'
                    },
                    {
                        cwd: 'openam-ui-scripts/src/main/resources',
                        src: ['**'],
                        dest: 'openam-ui-scripts/target/www'
                    },
                    {
                        cwd: 'openam-ui-scripts/src/test/resources',
                        src: ['**'],
                        dest: 'openam-ui-scripts/target/test'
                    },
                    {
                        cwd: 'openam-ui-scripts/src/test/js',
                        src: ['**'],
                        dest: 'openam-ui-scripts/target/test'
                    }
                ],
                verbose: true
            },

            scripts_css_to_test: {
                files: [
                    {
                        cwd: 'openam-ui-scripts/target/www',
                        src: ['css/**/*.css'],
                        dest: 'openam-ui-scripts/target/test'
                    }
                ],
                verbose: true
            },

            scripts_to_tomcat: {
                files: [
                    {
                        cwd: '<%= forgerockui %>/forgerock-ui-commons/src/main/js',
                        src: ['**/*'],
                        dest: '<%= destination %>/scripts'
                    },
                    {
                        cwd: '<%= forgerockui %>/forgerock-ui-commons/src/main/resources',
                        src: ['**/*'],
                        dest: '<%= destination %>/scripts'
                    },
                    {
                        cwd: 'openam-ui-common/src/main/resources',
                        src: ['**/*'],
                        dest: '<%= destination %>/scripts'
                    },
                    {
                        cwd: 'openam-ui-common/src/main/js',
                        src: ['**/*'],
                        dest: '<%= destination %>/scripts'
                    },
                    {
                        cwd: 'openam-ui-scripts/src/main/resources',
                        src: ['**/*'],
                        dest: '<%= destination %>/scripts'
                    },
                    {
                        cwd: 'openam-ui-scripts/src/main/js',
                        src: ['**/*'],
                        dest: '<%= destination %>/scripts'
                    }
                    /*
                    ,
                    {
                        cwd: 'target/test',
                        src: ['**'],
                        dest: '<%= destination %>/../test'
                    },
                    {
                        cwd: 'target/www',
                        src: ['**'],
                        dest: '<%= destination %>/../www'
                    }*/
                ],
                verbose: true
            },

            policy_to_test: {
                files: [
                    {
                        cwd: 'openam-ui-policy/target/dependency',
                        src: ['**'],
                        dest: 'openam-ui-policy/target/www'
                    },
                    {
                        cwd: 'openam-ui-policy/test/libs',
                        src: ['**'],
                        dest: 'openam-ui-policy/target/www/libs'
                    },
                    {
                        cwd: 'openam-ui-policy/src/main/js',
                        src: ['**'],
                        dest: 'openam-ui-policy/target/www'
                    },
                    {
                        cwd: 'openam-ui-policy/src/main/resources',
                        src: ['**'],
                        dest: 'openam-ui-policy/target/www'
                    },
                    {
                        cwd: 'openam-ui-policy/src/test/resources',
                        src: ['**'],
                        dest: 'openam-ui-policy/target/test'
                    },
                    {
                        cwd: 'openam-ui-policy/src/test/js',
                        src: ['**'],
                        dest: 'openam-ui-policy/target/test'
                    }
                ],
                verbose: true
            },

            source_css_to_test: {
                files: [
                    {
                        cwd: 'openam-ui-policy/target/www',
                        src: ['css/**/*.css'],
                        dest: 'openam-ui-policy/target/test'
                    }
                ],
                verbose: true
            },

            source_to_tomcat: {
                files: [
                    {
                        cwd: '<%= forgerockui %>/forgerock-ui-commons/src/main/js',
                        src: ['**/*'],
                        dest: '<%= destination %>/policyEditor'
                    },
                    {
                        cwd: '<%= forgerockui %>/forgerock-ui-commons/src/main/resources',
                        src: ['**/*'],
                        dest: '<%= destination %>/policyEditor'
                    },
                    {
                        cwd: 'openam-ui-common/src/main/resources',
                        src: ['**/*'],
                        dest: '<%= destination %>/policyEditor'
                    },
                    {
                        cwd: 'openam-ui-common/src/main/js',
                        src: ['**/*'],
                        dest: '<%= destination %>/policyEditor'
                    },
                    {
                        cwd: 'openam-ui-policy/src/main/resources',
                        src: ['**/*'],
                        dest: '<%= destination %>/policyEditor'
                    },
                    {
                        cwd: 'openam-ui-policy/src/main/js',
                        src: ['**/*'],
                        dest: '<%= destination %>/policyEditor'
                    }

                    /*,
                    {
                        cwd: 'target/test',
                        src: ['**'],
                        dest: '<%= destination %>/../test'
                    },
                    {
                        cwd: 'target/www',
                        src: ['**'],
                        dest: '<%= destination %>/../www'
                    }*/
                ],
                verbose: true
            }

        },
        watch: {
            frCommons: {
                files: [
                    '<%= forgerockui %>/forgerock-ui-commons/src/main/js/**',
                    '<%= forgerockui %>/forgerock-ui-commons/src/main/resources/**'
                ],
                tasks: ['sync']
            },
            frUser : {
                files: [
                    '<%= forgerockui %>/forgerock-ui-user/src/main/js/**',
                    '<%= forgerockui %>/forgerock-ui-user/src/main/resources/**'
                ],
                tasks: ['sync']
            },
            common : {
                files: [
                    'openam-ui-common/src/main/js/**',
                    'openam-ui-common/src/main/resources/**'
                ],
                tasks: ['sync']
            },
            ria: {
                files: [
                    'openam-ui-ria/src/main/js/**',
                    'openam-ui-ria/src/main/resources/**'
                ],
                tasks: ['sync']
            },
            scripts: {
                files: [
                    'openam-ui-scripts/src/main/js/**',
                    'openam-ui-scripts/src/main/resources/**',
                    'openam-ui-scripts/src/test/js/**',
                    'openam-ui-scripts/src/test/resources/**'
                ],
                tasks: ['sync']
            },
            policy: {
                files: [
                    'openam-ui-policy/src/main/js/**',
                    'openam-ui-policy/src/main/resources/**',
                    'openam-ui-policy/src/test/js/**',
                    'openam-ui-policy/src/test/resources/**'
                ],
                tasks: ['sync']
            }
        }
    });

    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-sync');

    grunt.registerTask('default', ['sync', 'watch']);
};
