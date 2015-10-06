<!DOCTYPE html>
<!--
  ~ DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  ~
  ~ Copyright 2012-2015 ForgeRock AS.
  ~
  ~ The contents of this file are subject to the terms
  ~ of the Common Development and Distribution License
  ~ (the License). You may not use this file except in
  ~ compliance with the License.
  ~
  ~ You can obtain a copy of the License at
  ~ http://forgerock.org/license/CDDLv1.0.html
  ~ See the License for the specific language governing
  ~ permission and limitations under the License.
  ~
  ~ When distributing Covered Code, include this CDDL
  ~ Header Notice in each file and include the License file
  ~ at http://forgerock.org/license/CDDLv1.0.html
  ~ If applicable, add the following below the CDDL Header,
  ~ with the fields enclosed by brackets [] replaced by
  ~ your own identifying information:
  ~ "Portions Copyrighted [year] [name of copyright owner]"
  ~
  ~ Portions Copyrighted 2014 Nomura Research Institute, Ltd
  -->
<html lang="en">
    <head>
        <meta charset="utf-8">
        <meta http-equiv="X-UA-Compatible" content="IE=edge">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="description" content="OAuth2 Authorization">
        <title>OAuth2 Authorization Server</title>
    </head>

    <body style="display:none">
        <div id="wrapper">Loading...</div>
        <footer id="footer" class="footer"></footer>
        <script type="text/javascript">
            pageData = {
                <#if realm??>realm: "${realm?js_string}",</#if>
                <#if ui_locales??>locale: "${ui_locales?js_string}",</#if>
                baseUrl : "${baseUrl?js_string}/XUI",
                oauth2Data: {
                    <#if redirect_uri??>redirectUri: "${redirect_uri?js_string}",</#if>
                    <#if scope??>scope: "${scope?js_string}",</#if>
                    <#if state??>state: "${state?js_string}",</#if>
                    <#if nonce??>nonce: "${nonce?js_string}",</#if>
                    <#if acr??>acr: "${acr?js_string}",</#if>
                    <#if display_description??>displayDescription: "${display_description?js_string}",</#if>
                    <#if response_type??>responseType: "${response_type?js_string}",</#if>
                    <#if client_id??>clientId: "${client_id?js_string}",</#if>
                    formTarget: "${(target!'.')?js_string}",
                    displayName: "${display_name?js_string}",
                    <#if user_name??>userName: "${user_name?js_string}",</#if>
                    <#if user_code??>userCode: "${user_code?js_string}",</#if>
                    displayScopes: ${display_scopes},
                    displayClaims: ${display_claims}
                }
            };
        </script>
        <script data-main="${baseUrl?html}/XUI/main-authorize" src="${baseUrl?html}/XUI/libs/requirejs-2.1.14-min.js"></script>
    </body>
</html>
