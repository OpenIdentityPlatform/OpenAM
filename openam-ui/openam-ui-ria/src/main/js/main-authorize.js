/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
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
    map: {
        "*" : {
            "ThemeManager"              : "org/forgerock/openam/ui/common/util/ThemeManager"
        }
    },
    paths: {
        underscore: "libs/lodash-2.4.1-min",
        spin: "libs/spin-2.0.1-min",
        jquery: "libs/jquery-2.1.1-min"
    },

    shim: {
        underscore: {
            exports: "_"
        },
        spin: {
            exports: "spin"
        }
    }
});

require([
    "jquery",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/SpinnerManager",
    "ThemeManager"
], function($, conf, constants, spinner, themeManager) {

    spinner.showSpinner();

    conf.globalData = { auth: { subRealm : window.realm } };

    themeManager.getTheme().then(function(){
        spinner.hideSpinner();
        $("#login-base,#footer").removeClass("hidden");
    }).fail(function(){
        spinner.hideSpinner();
        $("#login-base,#footer").removeClass("hidden");
    });
});
