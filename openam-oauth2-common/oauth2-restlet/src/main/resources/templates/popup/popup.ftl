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
<html>
<head>
    <title>Authorize popup</title>
</head>
<script src="../js/jquery.js" type="text/javascript"></script>
<script type="text/javascript">
    function poponload()
    {
        var testwindow = window.open("", "window", "location=1,status=1,scrollbars=1,width=450,height=500");
        var html = $("#print").html();
        testwindow.document.writeln(html);
    }
</script>
<body onload="javascript: poponload()">
<div id="print" style="visibility: hidden">
   ${htmlCode?if_exists};
</div>
</body>
</html>