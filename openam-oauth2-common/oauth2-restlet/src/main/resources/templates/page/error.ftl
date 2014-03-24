<!doctype html>
<!--
  ~ DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  ~
  ~ Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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
  ~ "Portions Copyrighted [2012] [ForgeRock Inc]"
  -->
<html lang="en">
<head>
    <title>OAuth2 Error Page</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" charset="utf-8"/>
    <meta name="description" content="OAuth2 Error">
    <link rel="stylesheet" href="resources/styles.css?v=1.0" type="text/css" media="screen" charset="utf-8">
    <!--[if lt IE 9]>
    <![endif]-->
</head>
<body>
<p><b>Error: </b>
<#if error??>
    <#if error_uri??>
        <a href="${error_uri?html}">${error?html}</a>
    <#else>
    ${error}
    </#if>
</#if>
</p>

<p><b>Description: </b>
<#if error_description??>${error_description?html}</#if>
</p>
</body>
</html>