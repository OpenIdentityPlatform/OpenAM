/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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

/*global require, window, JSONEditor */

require.config({
    paths: {
        i18next: "libs/i18next-1.7.3-min",
        i18nGrid: "libs/i18n/grid.locale-en",
        backbone: "libs/backbone-1.1.2-min",
        underscore: "libs/lodash-2.4.1-min",
        jquery: "libs/jquery-2.1.1-min",
        handlebars: "libs/handlebars-1.3.0-min",
        spin: "libs/spin-2.0.1-min",
        xdate: "libs/xdate-0.8-min",
        moment: "libs/moment-2.8.1-min",
        jsonEditor: "libs/jsoneditor-0.7.9-min",
        bootstrap: "libs/bootstrap.min",
        "bootstrap-dialog": "libs/bootstrap-dialog.min",
        ThemeManager: "org/forgerock/openam/ui/common/util/ThemeManager"
    },

    shim: {
        underscore: {
            exports: "_"
        },
        backbone: {
            deps: ["underscore"],
            exports: "Backbone"
        },
        handlebars: {
            exports: "Handlebars"
        },
        i18next: {
            deps: ["jquery", "handlebars"],
            exports: "i18next"
        },
        spin: {
            exports: "spin"
        },
        xdate: {
            exports: "xdate"
        },
        moment: {
            exports: "moment"
        },
        jsonEditor: {
            exports: "jsonEditor"
        },
        bootstrap: {
            deps: ["jquery"]
        },
        'bootstrap-dialog': {
            deps: ["jquery", "underscore", "backbone", "bootstrap"]
        }
    }
});

require([
    "jquery",
    "underscore",
    "backbone",
    "handlebars",
    "i18next",
    "spin",
    "xdate",
    "moment",
    "jsonEditor",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main",
    "org/forgerock/openam/ui/common/main",
    "org/forgerock/openam/ui/editor/main",
    "ThemeManager",
    "config/main"
], function ($, _, Backbone, Handlebars, i18n, spin, xdate, moment, jsonEditor, i18nManager, constants, eventManager) {

    // Helpers for the code that hasn't been properly migrated to require these as explicit dependencies:
    window.$ = $;
    window._ = _;
    window.Backbone = Backbone;

    eventManager.sendEvent(constants.EVENT_DEPENDECIES_LOADED);

    JSONEditor.defaults.options.theme = 'bootstrap3';
    JSONEditor.defaults.options.iconlib = "fontawesome4";
});