/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS.
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

/*global require, define, window */
require.config({
    paths: {
        "backbone"           : "libs/backbone-1.1.2-min",
        "backbone.paginator" : "libs/backbone-paginator.min",
        "backbone-relational": "libs/backbone-relational",

        "backgrid"          : "libs/backgrid.min",
        "backgrid.filter"   : "libs/backgrid-filter.min",
        "backgrid.paginator": "libs/backgrid-paginator.min",

        "bootstrap"         : "libs/bootstrap-3.3.4-custom",
        "bootstrap-dialog"  : "libs/bootstrap-dialog-1.34.4-min",
        "bootstrap-tabdrop": "libs/bootstrap-tabdrop-1.0",

        "doTimeout"       : "libs/jquery.ba-dotimeout-1.0-min",
        "form2js"         : "libs/form2js-2.0",
        "handlebars"      : "libs/handlebars-1.3.0-min",
        "i18next"         : "libs/i18next-1.7.3-min",
        "jquery"          : "libs/jquery-2.1.1-min",
        "js2form"         : "libs/js2form-2.0",
        "jsonEditor"      : "libs/jsoneditor-custom.min",
        "moment"          : "libs/moment-2.8.1-min",
        "qrcode"          : "libs/qrcode-1.0.0-min",
        "selectize"       : "libs/selectize-0.12.1-min",
        "sortable"        : "libs/jquery-nestingSortable-0.9.12",
        "spin"            : "libs/spin-2.0.1-min",
        "underscore"      : "libs/lodash-2.4.1-min",
        "xdate"           : "libs/xdate-0.8-min",

        "ThemeManager": "org/forgerock/openam/ui/common/util/ThemeManager",
        "UserDelegate": "org/forgerock/openam/ui/user/delegates/UserDelegate"
    },
    shim: {
        "backbone": {
            deps: ["underscore"],
            exports: "Backbone"
        },
        "backbone.paginator":{
            deps: ["backbone"]
        },
        "backbone-relational": {
            deps: ['backbone']
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

        "bootstrap": {
            deps: ["jquery"]
        },
        "bootstrap-dialog": {
            deps: ["jquery", "underscore", "backbone", "bootstrap"]
        },
        "bootstrap-tabdrop": {
            deps: ["jquery", "bootstrap"]
        },
        "doTimeout": {
            deps: ["jquery"],
            exports: "doTimeout"
        },
        "form2js": {
            exports: "form2js"
        },
        "handlebars": {
            exports: "handlebars"
        },
        "i18next": {
            deps: ["jquery", "handlebars"],
            exports: "i18next"
        },
        "js2form": {
            exports: "js2form"
        },
        "jsonEditor": {
            exports: "JSONEditor"
        },
        "moment": {
            exports: "moment"
        },
        "qrcode": {
            exports: "qrcode"
        },
        "selectize": {
            deps: ["jquery"]
        },
        "spin": {
            exports: "spin"
        },
        "underscore": {
            exports: "_"
        },
        "xdate": {
            exports: "xdate"
        },
        "sortable": {
            deps: ["jquery"]
        }
    }
});

/**
 * Loads all application on start, so each module will be available to
 * required synchronously
 */
require([
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "jquery",
    "underscore",
    "backbone",
    "backgrid",
    "form2js",
    "js2form",
    "jsonEditor",
    "spin",
    "xdate",
    "moment",
    "doTimeout",
    "handlebars",
    "i18next",
    "org/forgerock/openam/ui/common/util/ThemeManager",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "config/main",
    "org/forgerock/openam/ui/common/main",
    "org/forgerock/openam/ui/user/main",
    "org/forgerock/openam/ui/dashboard/main",
    "UserDelegate",
    "ThemeManager",
    "org/forgerock/commons/ui/user/main",
    "org/forgerock/commons/ui/common/main",
    "selectize",
    "backbone.paginator",
    "backgrid.paginator",
    "backgrid.filter",
    "bootstrap",
    "bootstrap-dialog",
    "bootstrap-tabdrop",
    "org/forgerock/openam/ui/uma/main",
    "org/forgerock/openam/ui/admin/main",
    "sortable",
    "qrcode"
], function(constants, eventManager, $, _, Backbone) {
    // Helpers for the code that hasn't been properly migrated to require these as explicit dependencies:
    window.$ = $;
    window._ = _;
    window.Backbone = Backbone;

    eventManager.sendEvent(constants.EVENT_DEPENDECIES_LOADED);
});
