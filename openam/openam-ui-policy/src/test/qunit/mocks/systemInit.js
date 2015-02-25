/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global require, define*/
define([
    "text!locales/en/translation.json",
    "text!themeConfig.json",
    "text!libs/less-1.5.1-min.js",
    "text!css/styles.less",
    "text!css/common/config.less",
    "text!css/common/helpers.less",
    "text!css/common/layout.less",
    "text!css/common/forms.less",
    "text!css/policy/font-icomoon.less",
    "text!css/policy/jquery-clockpicker.0.0.7.min.css",
    "text!css/policy/policyEditor.less",
    "text!css/selectize/selectize.less",
    "text!css/selectize/plugins/drag_drop.less",
    "text!css/selectize/plugins/dropdown_header.less",
    "text!css/selectize/plugins/optgroup_columns.less",
    "text!css/selectize/plugins/remove_button.less",
    "text!templates/common/NavigationTemplate.html",
    "text!templates/common/FooterTemplate.html"
], function () {

    /* an unfortunate need to duplicate the file names here, but I haven't
     yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [
            "locales/en/translation.json",
            "themeConfig.json",
            "libs/less-1.5.1-min.js",
            "css/styles.less",
            "css/common/config.less",
            "css/common/helpers.less",
            "css/common/layout.less",
            "css/common/forms.less",
            "css/policy/font-icomoon.less",
            "css/policy/jquery-clockpicker.0.0.7.min.css",
            "css/policy/policyEditor.less",
            "css/selectize/selectize.less",
            "css/selectize/plugins/drag_drop.less",
            "css/selectize/plugins/dropdown_header.less",
            "css/selectize/plugins/optgroup_columns.less",
            "css/selectize/plugins/remove_button.less",
            "templates/common/NavigationTemplate.html",
            "templates/common/FooterTemplate.html"
        ],
        deps = arguments;

    return function (server) {

        _.each(staticFiles, function (file, i) {
            server.respondWith(
                "GET",
                new RegExp(file.replace(/([\/\.\-])/g, "\\$1") + "$"),
                [
                    200,
                    { },
                    deps[i]
                ]
            );
        });

        server.respondWith(
            "POST",
            new RegExp("/openam/json/users\\?_action=idFromSession$"),
            [
                200,
                { },
                "{\"id\":\"amadmin\",\"realm\":\"/\",\"dn\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"successURL\":\"/openam/console\",\"fullLoginURL\":\"/openam/UI/Login?realm=%2F&goto=http%3A%2F%2Famserver.restful.com%2Fopenam-policy-debug%2F\"}"
            ]
        );
    };
});