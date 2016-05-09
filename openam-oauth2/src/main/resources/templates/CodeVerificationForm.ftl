<!DOCTYPE html>
<!--
~ The contents of this file are subject to the terms of the Common Development and
~ Distribution License (the License). You may not use this file except in compliance with the
~ License.
~
~ You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
~ specific language governing permission and limitations under the License.
~
~ When distributing Covered Software, include this CDDL Header Notice in each file and include
~ the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
~ Header, with the fields enclosed by brackets [] replaced by your own identifying
~ information: "Portions copyright [year] [name of copyright owner]".
~
~ Copyright 2015 ForgeRock AS.
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
      <#if locale??>locale: "${locale?js_string}",</#if>
      <#if errorCode??>errorCode: "${errorCode?js_string}",</#if>
      realm : "${realm?js_string}",
      baseUrl : "${baseUrl?js_string}/XUI"
  };
</script>
<script data-main="${baseUrl?html}/XUI/main-device" src="${baseUrl?html}/XUI/libs/requirejs-2.1.14-min.js"></script>
</body>
</html>
