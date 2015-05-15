/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014-2015 ForgeRock AS.
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
        "autosizeInput": "libs/jquery.autosize.input.min",

        "backbone"           : "libs/backbone-1.1.2-min",
        "backbone.paginator" : "libs/backbone-paginator.min",

        "backgrid"          : "libs/backgrid.min",
        "backgrid.filter"   : "libs/backgrid-filter.min",
        "backgrid.paginator": "libs/backgrid-paginator.min",
        "backgrid.selectall": "libs/backgrid-select-all.min",

        "bootstrap"         : "libs/bootstrap-3.3.4-custom",
        "bootstrap-dialog"  : "libs/bootstrap-dialog-1.34.4-min",

        "clockPicker": "libs/jquery-clockpicker.0.0.7.min",
        "doTimeout": "libs/jquery.ba-dotimeout-1.0-min",
        "form2js": "libs/form2js-2.0",
        "handlebars": "libs/handlebars-1.3.0-min",
        "i18nGrid": "libs/i18n/grid.locale-en",
        "i18next": "libs/i18next-1.7.3-min",
        "jsonEditor": "libs/jsoneditor-0.7.9-min",
        "jqgrid": "libs/jquery.jqGrid-4.5.4-min",
        "jquery": "libs/jquery-2.1.1-min",
        "jqueryui": "libs/jquery-ui-1.11.1-min",
        "js2form": "libs/js2form-2.0",
        "moment": "libs/moment-2.8.1-min",
        "multiselect": "libs/ui.multiselect-0.3",
        "selectize": "libs/selectize-0.12.1-min",
        "sortable": "libs/jquery-nestingSortable-0.9.12",
        "spin": "libs/spin-2.0.1-min",
        "underscore": "libs/lodash-2.4.1-min",
        "xdate": "libs/xdate-0.8-min",

        "ThemeManager": "org/forgerock/openam/ui/common/util/ThemeManager",
        "UserDelegate": "org/forgerock/openam/ui/user/delegates/UserDelegate"
    },

    shim: {
        "autosizeInput": {
            deps: ["jquery"],
            exports: "autosizeInput"
        },
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
        "clockPicker": {
            deps: ["jquery"],
            exports: "clockPicker"
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
        "i18nGrid": {
            deps: ["jquery"]
        },
        "i18next": {
            deps: ["jquery", "handlebars"],
            exports: "i18next"
        },
        "jsonEditor": {
            exports: "jsonEditor"
        },
        "js2form": {
            exports: "js2form"
        },
        "jqgrid": {
            deps: ["jquery", "jqueryui", "i18nGrid", "multiselect"]
        },
        "jqueryui": {
            deps: ["jquery"],
            exports: "jqueryui"
        },
        "moment": {
            exports: "moment"
        },
        "multiselect": {
            deps: ["jqueryui"],
            exports: "multiselect"
        },
        "selectize": {
            deps: ["jquery"]
        },
        "sortable": {
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
        }
    }
});

require([
    "jquery",
    "underscore",
    "backbone",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "backbone.paginator",
    "backgrid",
    "backgrid.paginator",
    "backgrid.filter",
    "backgrid.selectall",
    "bootstrap",
    "bootstrap-dialog",
    "form2js",
    "jsonEditor",
    "js2form",
    "spin",
    "xdate",
    "moment",
    "doTimeout",
    "handlebars",
    "i18next",
    "sortable",
    "jqueryui",
    "multiselect",
    "jqgrid",
    "clockPicker",
    "autosizeInput",
    "selectize",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "org/forgerock/commons/ui/common/main",
    "org/forgerock/openam/ui/common/main",
    "org/forgerock/openam/ui/policy/main",
    "ThemeManager",
    "config/main"
], function ($, _, Backbone, EventManager, Constants) {

    // Helpers for the code that hasn't been properly migrated to require these as explicit dependencies:
    window.$ = $;
    window._ = _;
    window.Backbone = Backbone;

    // necessary for requests initiated outside of the frameworks (such as via jqGrid)
    $(document).ajaxError(function (event, jqxhr, settings, thrownError) {
        if (jqxhr && jqxhr.responseJSON && jqxhr.responseJSON.code === 401 && settings.contentType !== "application/json") {
            EventManager.sendEvent(Constants.EVENT_UNAUTHORIZED);
        }
    });

    EventManager.sendEvent(Constants.EVENT_DEPENDECIES_LOADED);

    JSONEditor.defaults.options.theme = 'bootstrap3';
    JSONEditor.defaults.options.iconlib = "fontawesome4";
});
