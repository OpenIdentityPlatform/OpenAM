<!--
  ~ DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  ~
  ~  2012 ForgeRock AS. All rights reserved.
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
  ~ $Id$
  -->
<!DOCTYPE html>

<html>
<head>
    <title>The OpenAM OAuth2 Demo Page</title>

    <meta charset="UTF-8">

    <meta http-equiv="X-UA-Compatible" content="chrome=1">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black">
    <meta name="viewport"
          content="width=device-width; initial-scale=1.0; minimum-scale=1.0; maximum-scale=1.0; user-scalable=no;">

    <link href='http://fonts.googleapis.com/css?family=Droid+Sans:400,700' rel='stylesheet'
          type='text/css'>
    <link href='http://fonts.googleapis.com/css?family=Alfa+Slab+One' rel='stylesheet'
          type='text/css'>

    <link rel="stylesheet" href="../css/main.css" type="text/css">

    <link rel="icon" href="icon.png">
    <link rel="apple-touch-icon" href="button.png">
</head>
<body>
<div id="bg">

    <div id="dialog">
        <div id="dialogFrame">
            <div id="dialogContainer" class="radious">
                <div id="dialogContent"></div>
                <div id="dialogActions"></div>
                <div class="clear"></div>
            </div>
        </div>
    </div>
    <!-- #dialog -->

    <div id="container">
        <div id="header">
            <div id="logo">
                <a href="../index.html" title="idm"><img src="../images/logo.png"
                                                         width="226"
                                                         height="60" alt="idm logo"/> </a>
            </div>
            <!-- #logo-->

            <div class="clear"></div>
        </div>
        <!-- #header-->

        <div id="menu">
            <ul>
                <li class="active-first"><a href="../index.html" title="home">Home</a></li>
                <li><a href="http://forgerock.com/openam.html" title="OpenAM"
                       target="_blank">OpenAM</a></li>
                <li><a href="http://forgerock.com/opendj.html" title="OpenDJ"
                       target="_blank">OpenDJ</a></li>
                <li><a href="http://forgerock.com/openidm.html" title="OpenIDM" target="_blank"
                       class="last">OpenIDM</a></li>
            </ul>
        </div>
        <!-- #menu-->

        <div id="navi">
            <div id="nav-content">
                <a href="#" id="home_link" class="gray nodecorate"><span class="orange">Forge</span>Rock</a>
                <img src="../images/navi-next.png" alt="" class="navi-next" align="absmiddle"
                     height="5" width="3">
                <span style="display: inline;">${page_name}</span>
            </div>
            <!-- #navi-->

            <div id="messages">
            </div>
            <!-- #messages -->

            <div class="clear"></div>
        </div>
        <!-- #navi -->

        <div style="display: block;" id="content">
            <div id="registration">
                <h2 class="floatLeft">Test OAuth2 authentication flows</h2>

                <div class="clear"></div>

                <div class="shadowFrame">

                <#if error??>
                    <h4>Error Message:</h4>
                    <#if error_uri??>
                        <a href="${error_uri}">${error}</a>
                    <#else>
                    ${error}
                    </#if>
                </#if>

                <#if protected??>
                    <p>The protected resource has been presented for
                        <b>${protected.name?if_exists}</b> after presenting
                        this access_token<br/>
                        <#if protected.uri??>
                            <a href="${protected.uri}"><b>${protected.access_token?if_exists}</b></a>
                        <#else>
                            <b>${protected.access_token?if_exists}</b>
                        </#if>
                    </p>

                    <#if protected.scope??>
                        <#if protected.scope?has_content>
                            <hr/>
                            <h4>The token scope:</h4>

                            <#list protected.scope as g>
                                <div class="field"><b>${g}</b></div>
                            </#list>
                        </#if>
                    </#if>
                <#else>
                    <p>The protected content is not available. </p>
                </#if>

                    <hr/>
                    <h4>Test parameters:</h4>

                    <p>Custom action: ${page_action?if_exists} </p><br/>
                <#if page_scope??>
                    <#if page_scope?has_content>
                        <h4>Requested Scopes:</h4>
                        <#list page_scope as g>
                            <div class="field"><b>${g}</b></div>
                        </#list>
                    </#if>
                </#if>

                </div>
            </div>
        </div>
        <!-- #content-->

    </div>
    <!-- #container-->

    <div id="footer">
        <div id="footer-content">
            <a href="http://forgerock.com/" target="_blank"><img
                    src="../images/footer_logo.png" alt="aRockGroupCompany" width="228"
                    height="41"/></a><br/>
            <a href="mailto: info@forgerock.com" class="orange">info@forgerock.com</a>, or phone +47
            21081746. <br/>
            Copyright © 2012 ForgeRock, all rights reserved.
        </div>
    </div>
    <!-- #footer-->

</div>
<!-- #bg -->
</body>
</html>