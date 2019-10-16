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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2019 Open Identity Community
 */
module.exports = function (grunt) {
    var serverDeployDirectory = process.env.OPENAM_HOME + '/api',
        compiledDirectory = 'target/www/'
    grunt.initConfig({
        copy: {
            swagger: {
                files: [{
                    expand: true,
                    cwd: 'node_modules/swagger-ui-dist/',
                    src: ['swagger-ui-bundle.js', 'swagger-ui-standalone-preset.js', 'swagger-ui.css'],
                    dest: compiledDirectory
                }],
                options: {
                    noProcess: ['**/*.{png,gif,jpg,ico,svg,ttf,eot,woff}']
                }
            },
            resources: {
                files: [{
                    expand: true,
                    cwd: 'src/main/resources/',
                    src: ['**'],
                    dest: compiledDirectory
                }]
            },
            server: {
                files: [{
                    expand: true,
                    cwd: compiledDirectory,
                    src: ["**"],
                    dest: serverDeployDirectory
                }]
            }
        }
    });

    grunt.loadNpmTasks('grunt-contrib-copy');

    grunt.registerTask('build:dev', ['build:prod', 'copy:server']);
    grunt.registerTask('build:prod', ['copy:swagger', 'copy:resources']);

    grunt.registerTask("default", ["build:dev"]);
};