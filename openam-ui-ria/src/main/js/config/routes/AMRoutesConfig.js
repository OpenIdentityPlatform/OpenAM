/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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
            view: "org/forgerock/openam/ui/user/login/RESTLoginView",
            url: /login([^\&]+)?(&.+)?/,
            pattern: "login??",
            defaults: ["/"],
            forceUpdate: true
        },
        "loginDialog" : {
            dialog: "org/forgerock/openam/ui/user/login/RESTLoginDialog",
            url: "loginDialog/"
        },
        "": {
            view: "org/forgerock/openam/ui/user/profile/UserProfileView",
            role: "authenticated",
            url: "",
            forceUpdate: true
        },
        "profile": {
            view: "org/forgerock/openam/ui/user/profile/UserProfileView",
            role: "authenticated",
            url: "profile/" ,
            forceUpdate: true
        },
        "changeSecurityData": {
            base: "profile",
            dialog: "org/forgerock/openam/ui/user/profile/ChangeSecurityDataDialog",
            role: "authenticated",
            url: "profile/change_security_data/"
        },
        "dashboard": {
            view: "org/forgerock/openam/ui/dashboard/DashboardView",
            role: "authenticated",
            url: "dashboard/",
            forceUpdate: true
        },
        "oauth2Tokens": {
            view: "org/forgerock/openam/ui/user/oauth2/TokensView",
            role: "authenticated",
            url: "oauth2/tokens",
            forceUpdate: true
        }

    };
    
    return obj;
});