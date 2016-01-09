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
 * Portions copyright 2011-2016 ForgeRock AS.
 */


define("org/forgerock/openam/ui/user/login/RESTLogoutView", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openam/ui/user/login/RESTLoginHelper"
], function ($, AbstractView, Configuration, Constants, EventManager, RESTLoginHelper) {

    var LogoutView = AbstractView.extend({
        template: "templates/openam/ReturnToLoginTemplate.html",
        baseTemplate: "templates/common/LoginBaseTemplate.html",
        data: {},
        render: function () {
            /*
            The RESTLoginHelper.filterUrlParams returns a filtered list of the parameters from the value set within the
            Configuration.globalData.auth.fullLoginURL which is populated by the server upon successful login.
            Once the session has ended we need to manually remove the fullLoginURL as it is no longer valid and can
            cause problems to subsequent failed login requests - i.e ones which do not override the current value.
            FIXME: Remove all session specific properties from the globalData object.
            */
            this.data.params = RESTLoginHelper.filterUrlParams(RESTLoginHelper.getSuccessfulLoginUrlParams());
            delete Configuration.globalData.auth.fullLoginURL;

            Configuration.setProperty("loggedUser", null);
            delete Configuration.gotoURL;
            EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true });

            this.data.title = $.t("templates.user.RestLogoutTemplate.loggedOut");
            this.parentRender();
        }
    });

    return new LogoutView();
});
