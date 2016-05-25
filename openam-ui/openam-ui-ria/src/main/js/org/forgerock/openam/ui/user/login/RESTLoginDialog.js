/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2016 ForgeRock AS. All rights reserved.
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

define([
    "backbone",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/user/login/RESTLoginHelper",
    "org/forgerock/openam/ui/user/login/RESTLoginView"
], function (Backbone, AbstractView, Configuration, RESTLoginHelper, RESTLoginView) {
    var LoginDialog = AbstractView.extend({
        template: "templates/common/DefaultBaseTemplate.html",
        data : {},
        actions: [],
        render () {
            Configuration.backgroundLogin = true;
            // The session cookie does not expire until the browser is closed. So if the server session expires and
            // the browser remains, the XUI will attempt to login sending the old cookie and the server will assume
            // this is a session upgrade. Removing the old session cookie first resolves this problem.
            RESTLoginHelper.removeSessionCookie();
            RESTLoginView.render();
        }
    });
    return new LoginDialog();
});
