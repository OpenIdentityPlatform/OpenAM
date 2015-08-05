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
    grunt.registerTask('selectiveWatch', function () {
        var targets = Array.prototype.slice.call(arguments, 0);
        Object.keys(grunt.config('watch')).filter(function (target) {
            return !(grunt.util._.indexOf(targets, target) !== -1);
        }).forEach(function (target) {
            grunt.log.writeln('Ignoring ' + target + '...');
            grunt.config(['watch', target], {files: []});
        });
        grunt.task.run('watch');
    });

    grunt.initConfig({
        // please update environment variable OPENAM_VERSION after realise, for fix cache issue
        // you can use version value from main pom file, ex. 13.0.0-SNAPSHOT
        buildNumber: process.env.OPENAM_VERSION,
        destination: process.env.OPENAM_HOME,
        forgerockui: process.env.FORGEROCK_UI_SRC,
        replace: {
            ria_html: {
                src: ['openam-ui-ria/src/main/resources/index.html'],
                dest: '<%= destination %>/XUI/index.html',
                replacements: [{
                    from: '${version}',
                    to:  '<%= buildNumber %>'
                }]
            },
            ria_style: {
                src: ['openam-ui-ria/src/main/resources/css/styles.less'],
                dest: '<%= destination %>/XUI/css/styles.less',
                replacements: [{
                    from: '${version}',
                    to:  '<%= buildNumber %>'
                }]
            },
            ria_test: {
                // temporary fix for test
                src: ['openam-ui-ria/src/main/resources/css/styles.less'],
                dest: '<%= destination %>/../www/css/styles.less',
                replacements: [{
                    from: '?v=@{openam-version}',
                    to:  ''
                }]
            },
            script_html: {
                src: ['openam-ui-scripts/src/main/resources/index.html'],
                dest: '<%= destination %>/scripts/index.html',
                replacements: [{
                    from: '${version}',
                    to:  '<%= buildNumber %>'
                }]
            },
            script_style: {
                src: ['openam-ui-scripts/src/main/resources/css/styles.less'],
                dest: '<%= destination %>/scripts/css/styles.less',
                replacements: [{
                    from: '${version}',
                    to:  '<%= buildNumber %>'
                }]
            },
            script_test: {
                // temporary fix for test
                src: ['openam-ui-scripts/src/main/resources/css/styles.less'],
                dest: '<%= destination %>/../www/css/styles.less',
                replacements: [{
                    from: '?v=@{openam-version}',
                    to:  ''
                }]
            },
            policy_html: {
                src: ['openam-ui-policy/src/main/resources/index.html'],
                dest: '<%= destination %>/policyEditor/index.html',
                replacements: [{
                    from: '${version}',
                    to:  '<%= buildNumber %>'
                }]
            },
            policy_style: {
                src: ['openam-ui-policy/src/main/resources/css/styles.less'],
                dest: '<%= destination %>/policyEditor/css/styles.less',
                replacements: [{
                    from: '${version}',
                    to:  '<%= buildNumber %>'
                }]
            },
            policy_test: {
                // temporary fix for test
                src: ['openam-ui-policy/src/main/resources/css/styles.less'],
                dest: '<%= destination %>/../www/css/styles.less',
                replacements: [{
                    from: '?v=@{openam-version}',
                    to:  ''
                }]
            }
        },
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
        less: {
            xui: {
                files: {
                    "<%= destination %>/XUI/css/styles.css": "<%= destination %>/XUI/css/styles.less"
                }
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
                tasks: ['sync', 'replace']
            },
            scripts: {
                files: [
                    'openam-ui-scripts/src/main/js/**',
                    'openam-ui-scripts/src/main/resources/**',
                    'openam-ui-scripts/src/test/js/**',
                    'openam-ui-scripts/src/test/resources/**'
                ],
                tasks: ['sync', 'replace']
            },
            policy: {
                files: [
                    'openam-ui-policy/src/main/js/**',
                    'openam-ui-policy/src/main/resources/**',
                    'openam-ui-policy/src/test/js/**',
                    'openam-ui-policy/src/test/resources/**'
                ],
                tasks: ['sync', 'replace', 'less']
            }
        }
    });

    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-sync');
    grunt.loadNpmTasks('grunt-text-replace');
    grunt.loadNpmTasks('grunt-contrib-less');

    grunt.registerTask('default', [
        'sync',
        'replace',
        'less',
        'selectiveWatch:frCommons:frUser:common:ria'
    ]);
};
