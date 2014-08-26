/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 ForgeRock AS.
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

/*global define, $, form2js, _ */

define("org/forgerock/openam/ui/dashboard/OAuthTokensView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/dashboard/OAuthTokensDelegate"
], function (AbstractView, OAuthTokensDelegate) {

    var OAuthToken = AbstractView.extend({
            template: "templates/openam/oauth2/TokensTemplate.html",
            noBaseTemplate: true,
            element: '#myOAuthTokens',
            events: { 'click  a.deleteToken': 'deleteToken' },
            render: function () {

                var self = this;
                OAuthTokensDelegate.getOAuthTokens()
                    .then(function (data) {
                            self.data.tokens = data.result;
                            self.parentRender();
                    });
            },

            deleteToken: function (e) {
                e.preventDefault();
                var self = this;
                OAuthTokensDelegate.deleteOAuthToken(e.currentTarget.id)
                    .then(function () {
                        console.log('Deleted access token');
                        self.render();
                    }, function () {
                        console.error("Failed to delete access token");
                    });
            }
        })
        ;

    return new OAuthToken();
})
;


