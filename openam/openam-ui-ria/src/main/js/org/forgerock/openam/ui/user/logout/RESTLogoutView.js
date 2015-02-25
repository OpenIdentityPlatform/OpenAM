/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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

/*global define, $, form2js, _, js2form, Handlebars, window */

define("org/forgerock/openam/ui/user/logout/RESTLogoutView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/user/login/RESTLoginHelper"
], function(AbstractView, router, conf, constants, eventManager, uiUtils, loginHelper) {

    var LogoutView = AbstractView.extend({
        template: "templates/openam/RESTLogoutTemplate.html",
        baseTemplate: "templates/common/MediumBaseTemplate.html",

        data: {},
        events: {
            "click #gotoLogin": "gotoLoginViewClick"
        },
        render: function(args, callback) {
            this.parentRender(callback);
        },
        gotoLoginViewClick: function(event){
            event.preventDefault();
            var urlParams = "";
            if (conf.globalData.auth.fullLoginURL) {
                urlParams = loginHelper.filterUrlParams(loginHelper.getLoginUrlParams());
            }

            conf.setProperty('loggedUser', null);
            delete conf.gotoURL;
            eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true});
            router.navigate(router.getLink(router.configuration.routes.login) + urlParams, {trigger: true});
        }
    });

    return new LogoutView();
});


