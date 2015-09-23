/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
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
        "backbone"          : "libs/backbone-1.1.2-min",
        "backbone.paginator": "libs/backbone-paginator.min",

        "backgrid"          : "libs/backgrid.min",
        "backgrid.filter"   : "libs/backgrid-filter.min",
        "backgrid.paginator": "libs/backgrid-paginator.min",
        "backgrid.selectall": "libs/backgrid-select-all.min",

        "bootstrap"         : "libs/bootstrap-3.3.4-custom",
        "bootstrap-dialog"  : "libs/bootstrap-dialog-1.34.4-min",

        "handlebars"        : "libs/handlebars-1.3.0-min",
        "i18nGrid"          : "libs/i18n/grid.locale-en",
        "i18next"           : "libs/i18next-1.7.3-min",
        "jsonEditor"        : "libs/jsoneditor-0.7.9-min",
        "jquery"            : "libs/jquery-2.1.1-min",
        "moment"            : "libs/moment-2.8.1-min",
        "spin"              : "libs/spin-2.0.1-min",
        "underscore"        : "libs/lodash-2.4.1-min",
        "xdate"             : "libs/xdate-0.8-min",

        "ThemeManager"      : "org/forgerock/openam/ui/common/util/ThemeManager"
    },

    shim: {
        "backbone": {
            deps: ["underscore"],
            exports: "Backbone"
        },
        "backbone.paginator": {
            deps: ["backbone"]
        },
        "backgrid": {
            deps: ["jquery", "underscore", "backbone"],
            exports: "Backgrid"
        },
        "backgrid.filter": {
            deps: ["backgrid"]
        },
        "backgrid.paginator": {
            deps: ["backgrid", "backbone.paginator"]
        },
        "backgrid.selectall": {
            deps: ["backgrid"]
        },
        "bootstrap": {
            deps: ["jquery"]
        },
        "bootstrap-dialog": {
            deps: ["jquery", "underscore", "backbone", "bootstrap"]
        },
        jsonEditor: {
            exports: "jsonEditor"
        },
        handlebars: {
            exports: "Handlebars"
        },
        i18next: {
            deps: ["jquery", "handlebars"],
            exports: "i18next"
        },
        moment: {
            exports: "moment"
        },
        underscore: {
            exports: "_"
        },
        spin: {
            exports: "spin"
        },
        xdate: {
            exports: "xdate"
        }
    }
});

require([
    "jquery",
    "underscore",
    "backbone",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "backbone.paginator",
    "backgrid",
    "backgrid.paginator",
    "backgrid.filter",
    "backgrid.selectall",
    "bootstrap",
    "bootstrap-dialog",
    "handlebars",
    "i18next",
    "jsonEditor",
    "moment",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "org/forgerock/commons/ui/common/main",
    "org/forgerock/openam/ui/common/main",
    "org/forgerock/openam/ui/editor/main",
    "spin",
    "ThemeManager",
    "xdate",
    "config/main"
], function ($, _, Backbone, Constants, EventManager) {

    // Helpers for the code that hasn't been properly migrated to require these as explicit dependencies:
    window.$ = $;
    window._ = _;
    window.Backbone = Backbone;

    EventManager.sendEvent(Constants.EVENT_DEPENDECIES_LOADED);

    JSONEditor.defaults.options.theme = 'bootstrap3';
    JSONEditor.defaults.options.iconlib = "fontawesome4";
});