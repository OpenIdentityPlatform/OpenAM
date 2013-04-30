<!doctype html>
<!--
  ~ DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  ~
  ~ Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
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
  -->
<html lang="en">
<head>
    <title>OAuth2 Authorization Server</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" charset="utf-8"/>
    <meta name="description" content="OAuth2 Error">
    <link rel="stylesheet" href="resources/styles.css?v=1.0" type="text/css" media="screen" charset="utf-8">
    <!--[if lt IE 9]>
    <![endif]-->
</head>
<body>
<div id="container">
    <div id="header">
        <h2>OAuth authorization page</h2>
        <section id="intro">
            <h2>Application requesting scope</h2>

            <p>${display_name?if_exists}:</p>
            <p>${display_description?if_exists} </p>
        </section>
    </div>
    <aside>
        <form action="${target!'.'}" method="post">
            <h4>The following private info is requested</h4>
        <#if display_scope??>
            <#list display_scope as r>
                <b>${r}</b><br/>
            </#list>
        </#if>
        <#if grantedScopes??>
            <#if grantedScopes?has_content>
                <hr/>
                <h4>Previously approved scopes</h4>
                <#list grantedScopes as g>
                    <b>${g}</b><br/>
                </#list>
            </#if>
        </#if>
            <br/>
            <!-- Optional parameters -->
        <#if realm??>
            <input type="hidden" name="realm" value="${realm}"/>
        </#if>
        <#if redirect_uri??>
            <input type="hidden" name="redirect_uri" value="${redirect_uri}"/>
        </#if>
        <#if scope??>
            <input type="hidden" name="scope" value="${scope}"/>
        </#if>
        <#if state??>
            <input type="hidden" name="state" value="${state}"/>
        </#if>
            <!-- Required parameters -->
            <input type="hidden" name="response_type" value="${response_type}"/>
            <input type="hidden" name="client_id" value="${client_id}"/>
            <!-- Custom parameters -->
            <!--input type="hidden" name="decision" value="allow"/>
            <input type="submit" value="Allow access" class="button save"/>
            <input type="hidden" name="decision" value="deny"/>
            <input type="submit" value="No Thanks" class="button"/-->
            <input type="submit" name="decision" class="button gray" value="Allow"/>
            <input type="submit" name="decision" class="button gray" value="Deny"/>
        </form>
    </aside>
</div>
</body>
</html>