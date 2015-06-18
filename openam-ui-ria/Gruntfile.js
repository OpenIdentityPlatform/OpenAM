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
            XUI: {
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
                        cwd: 'src/main/resources',
                        src: ['**/*'],
                        dest: '<%= destination %>/XUI'
                    },
                    {
                        cwd: 'src/main/js',
                        src: ['**/*'],
                        dest: '<%= destination %>/XUI'
                    }
                ],
                verbose: true
            }
        },
        watch: {
            XUI: {
                files: [
                    '<%= forgerockui %>/forgerock-ui-commons/src/main/js/**',
                    '<%= forgerockui %>/forgerock-ui-commons/src/main/resources/**',
                    '<%= forgerockui %>/forgerock-ui-user/src/main/js/**',
                    '<%= forgerockui %>/forgerock-ui-user/src/main/resources/**',
                    'src/main/js/**',
                    'src/main/resources/**'
                ],
                tasks: ['sync']
            }
        }
    });

    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-sync');

    grunt.registerTask('default', ['sync', 'watch']);
};