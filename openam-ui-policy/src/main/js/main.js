/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All Rights Reserved
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

/*global require, window */

require.config({
    paths: {
        i18next:        "libs/i18next-1.7.3-min",
        i18nGrid:       "libs/i18n/grid.locale-en",
        backbone:       "libs/backbone-1.1.2-min",
        "backbone.paginator": "libs/backbone-paginator.min",
        underscore:     "libs/lodash-2.4.1-min",
        js2form:        "libs/js2form-2.0",
        form2js:        "libs/form2js-2.0",
        spin:           "libs/spin-2.0.1-min",
        jquery:         "libs/jquery-2.1.1-min",
        xdate:          "libs/xdate-0.8-min",
        sortable:       "libs/jquery-nestingSortable-0.9.12",
        doTimeout:      "libs/jquery.ba-dotimeout-1.0-min",
        handlebars:     "libs/handlebars-1.3.0-min",
        moment:         "libs/moment-2.8.1-min",
        jqueryui:       "libs/jquery-ui-1.11.1-min",
        clockPicker:    "libs/jquery-clockpicker.0.0.7.min",
        autosizeInput:  "libs/jquery.autosize.input.min",
        multiselect:    "libs/ui.multiselect-0.3",
        jqgrid:         "libs/jquery.jqGrid-4.5.4-min",
        selectize:      "libs/selectize-0.11.2-min",
        backgrid:       "libs/backgrid.min",
        "backgrid.paginator":   "libs/backgrid-paginator.min",
        "backgrid.filter":      "libs/backgrid-filter.min",
        LoginDialog:            "org/forgerock/commons/ui/common/LoginDialog",
        LoginView:              "org/forgerock/commons/ui/common/LoginView",
        ThemeManager:           "org/forgerock/openam/ui/common/util/ThemeManager"
    },

    shim: {
        underscore: {
            exports: "_"
        },
        backbone: {
            deps: ["underscore"],
            exports: "Backbone"
        },
        "backbone.paginator":{
            deps: ["backbone"]
        },
        js2form: {
            exports: "js2form"
        },
        form2js: {
            exports: "form2js"
        },
        spin: {
            exports: "spin"
        },
        xdate: {
            exports: "xdate"
        },
        doTimeout: {
            deps: ["jquery"],
            exports: "doTimeout"
        },
        handlebars: {
            exports: "Handlebars"
        },
        sortable: {
            deps: ["jquery"]
        },
        i18next: {
            deps: ["jquery", "handlebars"],
            exports: "i18next"
        },
        moment: {
            exports: "moment"
        },
        jqueryui: {
            deps: ["jquery"],
            exports: "jqueryui"
        },
        i18nGrid: {
            deps: ["jquery"]
        },
        clockPicker: {
            deps: ["jquery"],
            exports: "clockPicker"
        },
        autosizeInput: {
            deps: ["jquery"],
            exports: "autosizeInput"
        },
        multiselect: {
            deps: ["jqueryui"],
            exports: "multiselect"
        },
        jqgrid: {
            deps: ["jquery", "jqueryui", "i18nGrid", "multiselect"]
        },
        selectize: {
            deps: ["jquery"]
        },
        backgrid: {
            deps: ["jquery", "underscore"],
            exports: "Backgrid"
        },
        "backgrid.paginator": {
            deps: ["backgrid", "backbone.paginator"]
        },
        "backgrid.filter": {
            deps: ["backgrid"]
        }

    }
});

/**
 * Loads all application on start, so each module will be available to
 * required synchronously
 */
require([
    "jquery",
    "underscore",
    "backbone",
    "form2js",
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
    "backgrid",
    "backbone.paginator",
    "backgrid.paginator",
    "backgrid.filter",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main",
    "org/forgerock/openam/ui/common/main",
    "org/forgerock/openam/ui/policy/main",
    "org/forgerock/openam/ui/uma/main",
    "ThemeManager",
    "config/main"
], function ($, _, Backbone, form2js, js2form, spin, xdate, moment, doTimeout, Handlebars, i18n, sortable, jqueryui, multiselect, jqgrid, clockPicker, autosizeInput, selectize, backgrid, backbonePaginator, backgridPaginator, backgridFilter, i18nManager, constants, eventManager) {

    // Helpers for the code that hasn't been properly migrated to require these as explicit dependencies:
    window.$ = $;
    window._ = _;
    window.Backbone = Backbone;

    // necessary for requests initiated outside of the frameworks (such as via jqGrid)
    $(document).ajaxError(function (event, jqxhr, settings, thrownError) {
        if (jqxhr && jqxhr.responseJSON && jqxhr.responseJSON.code === 401 && settings.contentType !== "application/json") {
            eventManager.sendEvent(constants.EVENT_UNAUTHORIZED);
        }
    });


    eventManager.sendEvent(constants.EVENT_DEPENDECIES_LOADED);
});
