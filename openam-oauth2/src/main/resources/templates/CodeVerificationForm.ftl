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
~ Portions Copyright 2026 3A Systems, LLC.
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
      <#if csrf??>csrf: "${csrf?js_string}",</#if>
      realm : "${realm?js_string}",
      baseUrl : "${baseUrl?js_string}/XUI"
  };
  <#-- Cache-bust XUI: /XUI/* is served for a month, so a browser holding the previous
       DeviceTemplate.html (no hidden csrf field) would POST empty and be refused.
       Not a secret and the same for every user - RequireJS appends urlArgs to every module and
       text! URL it loads, and those URLs are recorded by access logs and shared caches. Bump
       this value the next time the device page's contract changes. -->
  var require = { urlArgs: "v=device-csrf-1" };
</script>
<script data-main="${baseUrl?html}/XUI/main-device" src="${baseUrl?html}/XUI/libs/requirejs-2.3.7-min.js"></script>
<script type="text/javascript">
  <#-- Fallback for any stale template that reaches the DOM anyway (custom main-device,
       intermediate cache ignoring the query, forked theme). -->
  document.addEventListener("submit", function (event) {
      var form = event.target;
      if (!form || !form.method || form.method.toLowerCase() !== "post") { return; }
      if (form.querySelector("input[name=csrf]")) { return; }
      if (!window.pageData || !window.pageData.csrf) { return; }
      var input = document.createElement("input");
      input.type = "hidden";
      input.name = "csrf";
      input.value = window.pageData.csrf;
      form.appendChild(input);
  }, true);
</script>
</body>
</html>
