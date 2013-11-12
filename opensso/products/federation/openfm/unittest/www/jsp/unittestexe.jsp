<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
  
   The contents of this file are subject to the terms
   of the Common Development and Distribution License
   (the License). You may not use this file except in
   compliance with the License.
  
   You can obtain a copy of the License at
   https://opensso.dev.java.net/public/CDDLv1.0.html or
   opensso/legal/CDDLv1.0.txt
   See the License for the specific language governing
   permission and limitations under the License.
  
   When distributing Covered Code, include this CDDL
   Header Notice in each file and include the License file
   at opensso/legal/CDDLv1.0.txt.
   If applicable, add the following below the CDDL Header,
   with the fields enclosed by brackets [] replaced by
   your own identifying information:
   "Portions Copyrighted [year] [name of copyright owner]"
  
   $Id: unittestexe.jsp,v 1.2 2009/09/10 16:35:39 veiming Exp $
  
--%>

<%@ page pageEncoding="UTF-8" %>

<%@page import="com.iplanet.am.util.SystemProperties" %>
<%@page import="com.sun.identity.shared.Constants" %>
<%@page import="com.sun.identity.unittest.UnittestLog" %>
<%@page import="java.text.*" %>
<%@page import="java.util.*" %>

<%
    String tests = request.getParameter("tests");
    DateFormat dateFormat = new SimpleDateFormat("MMMM_dd_yyyy_h_mm_a");
    Date date = new Date();
    String datestamp = dateFormat.format(date);
    UnittestLog.flush(null);
    String logLoc = SystemProperties.get(SystemProperties.CONFIG_PATH) +
        SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR) +
        "/unittest/" + datestamp;
%>
<html>
<head>
    <title>OpenSSO - Developer's Unit Tests</title>
    <link rel="stylesheet" type="text/css" href="../com_sun_web_ui/css/css_ns6up.css" />
    <link rel="styleSheet" href="../console/css/commontask.css" type="text/css" rel="stylesheet" />
    <link rel="styleSheet" href="../console/css/css_master.css" type="text/css" rel="stylesheet" />
    <link rel="shortcut icon" href="../com_sun_web_ui/images/favicon/favicon.ico" type="image/x-icon" />

    <script language="Javascript" src="../console/js/am.js"></script>
    <script language="Javascript">
        var ajaxObj = getXmlHttpRequestObject();
        var timeId;

        function runtest() {
            var frm = document.forms[0];
            frm.elements['tests'].value = '<%= tests %>';
            frm.submit();
            timeId = setInterval("getLog()", 1000);
        }

        function getLog() {
            ajaxPost(ajaxObj, 'unittestlog.jsp', 'd=<%=datestamp %>',
                displayLog);
        }

        function displayLog() {
            if (ajaxObj.readyState == 4) {
                var result = ajaxObj.responseText;
                var resultArray = result.split("\n");
                for (var i = 0; i < resultArray.length; i++) {
                    var str = resultArray[i];
                    if (str.length > 0) {
                        if (str == "MESSAGE: TestHarness:DONE") {
                            clearTimeout(timeId);
                            setTimeout("doneWithTest()", 1000);
                        } else {
                            writeToLog(str);
                        }
                    }
                }
            }
        }

        function doneWithTest() {
            var obj = document.getElementById("status");
            obj.innerHTML = "<b>Test Completed</b>";
        }

        function writeToLog(str) {
            var obj = document.getElementById("resulttext");
            var log = (str.indexOf('ERORR:') == 0) ?
                "<font color='red'><b>" + str + "</b></font><br />" :
                "<font color='black'>" + str + "</font><br />";
            obj.innerHTML += log;
            obj = document.getElementById("result");
            obj.scrollTop = obj.scrollHeight;
        }
    </script>
</head>
<body class="DefBdy" onload="runtest();">
    <div class="SkpMedGry1"><a href="#SkipAnchor3860"><img src="../com_sun_web_ui/images/other/dot.gif" alt="Jump to End of Masthead" border="0" height="1" width="1" /></a></div><div class="MstDiv">
    <table class="MstTblBot" title="" border="0" cellpadding="0" cellspacing="0" width="100%">
        <tr>
        <td class="MstTdTtl" width="99%">
        <br />
        <div class="MstDivTtl"><img name="AMConfig.configurator.ProdName" src="../console/images/PrimaryProductName.png" alt="OpenSSO" border="0" /></div>
        </td>
        <td class="MstTdLogo" width="1%"><img name="AMConfig.configurator.BrandLogo" src="../com_sun_web_ui/images/other/javalogo.gif" alt="Java(TM) Logo" border="0" height="55" width="31" /></td>
        </tr>
    </table>
    <table class="MstTblEnd" border="0" cellpadding="0" cellspacing="0" width="100%"><tr><td><img name="RMRealm.mhCommon.EndorserLogo" src="../com_sun_web_ui/images/masthead/masthead-sunname.gif" alt="Sun(TM) Microsystems, Inc." align="right" border="0" height="10" width="108" /></td></tr></table>
    </div>
    <table class="SkpMedGry1" border="0" cellpadding="5" cellspacing="0" width="100%"><tr><td><img src="../com_sun_web_ui/images/other/dot.gif" alt="Jump to End of Masthead" border="0" height="1" width="1" /></a></td></tr></table>
    <table border="0" cellpadding="10" cellspacing="0" width="100%"><tr><td></td></tr></table>

<table cellpadding=5 width="100%">
<tr>
<td>
<span class="TskPgeSbHdr">Developer's Unit Tests</span> <a href="unittest.jsp"><< Back to unit test page.</a>
<hr size="1" noshade="yes" />

<form name="frm" action="unittestrun.jsp" method="POST" target="hiddenframe">
    <input type="hidden" name="tests" />
</form>

<iframe id="hiddenframe" name="hiddenframe"  src="" height=0 width=0
    frameborder="0"></iframe>

<center>
<span id="status"><blink><b><font color="green">Test Started.</font></b></blink></span>
</center>
<p id="result" style="height:350px; overflow:auto; border:1px solid grey;">
<span id="resulttext"><p>Log is also written to <%= logLoc %></p></span>
</p>
</td></tr>
</table>
</body>
</html>
