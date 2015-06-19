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
    "org/forgerock/openam/ui/user/login/RESTLoginHelper",
    "org/forgerock/commons/ui/common/components/Navigation"
], function(AbstractView, Router, Configuration, Constants, EventManager, UiUtils, LoginHelper, Navigation) {

    var LogoutView = AbstractView.extend({
        template: "templates/openam/RESTLogoutTemplate.html",
        baseTemplate: "templates/common/LoginBaseTemplate.html",
        data: {},
        events: {
            "click #gotoLogin": "gotoLoginViewClick"
        },
        render: function(args, callback) {

            //Navigation.reload(callback);

            this.parentRender(callback);
        },
        gotoLoginViewClick: function(e){
            e.preventDefault();
            var urlParams = "";
            if (Configuration.globalData.auth.fullLoginURL) {
                urlParams = LoginHelper.filterUrlParams(LoginHelper.getLoginUrlParams());
            }

            Configuration.setProperty('loggedUser', null);
            delete Configuration.gotoURL;
            EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true});
            Router.navigate(Router.getLink(Router.configuration.routes.login) + urlParams, {trigger: true});
        }
    });

    return new LogoutView();
});
