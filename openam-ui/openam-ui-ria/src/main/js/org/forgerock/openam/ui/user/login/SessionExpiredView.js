/**
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
 * Copyright 2015 ForgeRock AS.
 */

define("org/forgerock/openam/ui/user/login/SessionExpiredView", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openam/ui/user/login/RESTLoginHelper"
], function ($, AbstractView, Configuration, Constants, EventManager, LoginHelper) {

    var SessionExpiredView = AbstractView.extend({
        template: "templates/openam/ReturnToLoginTemplate.html",
        baseTemplate: "templates/common/LoginBaseTemplate.html",
        data: {},
        render: function (args, callback) {

            var params;

            if (Configuration.globalData.auth.fullLoginURL) {
                params = LoginHelper.filterUrlParams(LoginHelper.getLoginUrlParams());
            }

            Configuration.setProperty("loggedUser", null);
            delete Configuration.gotoURL;
            EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true });

            this.data.fragment = params;
            this.data.title = $.t("templates.user.SessionExpiredTemplate.sessionExpired");
            this.parentRender(callback);
        }
    });

    return new SessionExpiredView();
});
