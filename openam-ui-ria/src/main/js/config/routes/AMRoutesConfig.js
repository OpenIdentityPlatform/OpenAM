/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
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

/**
 * "Portions Copyrighted 2011-2013 ForgeRock Inc"
 */
/*global define*/

/**
 * @author jfeasel
 */
define("config/routes/AMRoutesConfig", [
], function() {
    
    var obj = {
        "login" : {
            view: "LoginView",
            url: /login([^\&]+)?(&.+)?/,
            pattern: "login??",
            defaults: ["/"],
            forceUpdate: true
        },
        "forgotPassword": {
            view: "org/forgerock/openam/ui/user/profile/ForgotPasswordView",
            url: "forgotPassword/",
            forceUpdate: true
        },
        "forgotPasswordChange": {
            view: "org/forgerock/openam/ui/user/profile/ForgotPasswordView",
            url: /forgotPasswordChange(.*)?/,
            pattern: "forgotPasswordChange?",
            forceUpdate: true
        },
        "continueSelfRegister": {
            view: "org/forgerock/openam/ui/user/profile/RegisterView",
            url: /continueRegister(.*)?/,
            pattern: "continueRegister?",
            forceUpdate: true
        },
        "confirmLogin": {
            view: "org/forgerock/openam/ui/user/login/RESTConfirmLoginView",
            role: "ui-user",
            url: "confirmLogin/" ,
            forceUpdate: true
        },
        "dashboard": {
            view: "org/forgerock/openam/ui/dashboard/DashboardView",
            role: "ui-user",
            url: "dashboard/",
            forceUpdate: true
        },
        "oauth2Tokens": {
            view: "org/forgerock/openam/ui/user/oauth2/TokensView",
            role: "ui-user",
            url: "oauth2/tokens",
            forceUpdate: true
        }

    };
    
    return obj;
});