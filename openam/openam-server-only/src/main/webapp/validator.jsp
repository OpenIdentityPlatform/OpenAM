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

   $Id: validator.jsp,v 1.8 2009/11/20 22:45:57 ggennaro Exp $

--%>

<%--
   Portions Copyrighted 2013 ForgeRock AS
--%>

<%@ page pageEncoding="UTF-8"%>
<%@ page import="com.sun.identity.common.SystemConfigurationUtil" %>
<%@ page import="com.sun.identity.shared.Constants" %>
<%@ page import="com.sun.identity.workflow.ValidateSAML2" %>
<%@ page import="com.sun.identity.workflow.WorkflowException" %>
<%@ page import="java.net.MalformedURLException" %>
<%@ page import="java.net.URL" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.text.MessageFormat" %>
<%@ page import="java.util.Locale" %>
<%@ page import="org.owasp.esapi.ESAPI" %>

<html>
<head>
<%
    String deployuri = null;
    String serverURL = null;
    try {
        URL url = new URL(request.getRequestURL().toString());
        String prot = url.getProtocol();
        String port = Integer.toString(url.getPort());
        if (port.equals(-1)) {
            port = prot.equals("http") ? "443" : "80";
        }
        deployuri = url.getPath();
        int idx = deployuri.indexOf("/", 1);
        if (idx != -1) {
            deployuri = deployuri.substring(0, idx);
        }
        serverURL = prot + "://" + url.getHost() + ":" + port + deployuri;
    } catch (MalformedURLException e) {
        deployuri = SystemConfigurationUtil.getProperty(
                Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        String serverProtocol = SystemConfigurationUtil.getProperty(
                Constants.AM_SERVER_PROTOCOL);
        String serverHost = SystemConfigurationUtil.getProperty(
                Constants.AM_SERVER_HOST);
        String serverPort = SystemConfigurationUtil.getProperty(
                Constants.AM_SERVER_PORT);
        serverURL = serverProtocol + "://" + serverHost + ":" +
                serverPort + deployuri;
    }

    request.setCharacterEncoding("UTF-8");
    String realm = request.getParameter("realm");
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + realm, realm, "HTTPParameterValue", 2000, false)) {
        realm = "";
    }
    String cot = request.getParameter("cot");
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + cot, cot, "HTTPParameterValue", 2000, false)) {
        cot = "";
    }

    String idp = request.getParameter("idp");
    if (!ESAPI.validator().isValidInput("Invalid IdP entityID", idp, "HTTPQueryString", 2000, false)) {
        idp = "";
    }

    String sp = request.getParameter("sp");
    if (!ESAPI.validator().isValidInput("Invalid SP entityID", sp, "HTTPQueryString", 2000, false)) {
        sp = "";
    }
    String locale = request.getParameter("locale");
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + locale, locale, "HTTPParameterValue", 2000, false)) {
            locale = "";
    }
    String setupFailedTitle = "";
    String setupError = "";

    String authIDPTitle = "";
    String authIDPPassed = ValidateSAML2.getMessage(
        "validate.help.auth.idp.passed", locale);
    String authIDPFailed = ValidateSAML2.getMessage(
        "validate.help.auth.idp.failed", locale);
    String authIDPAgain = ValidateSAML2.getMessage(
        "validate.auth.idp.again", locale);

    String authSPTitle = "";
    String authSPPassed = ValidateSAML2.getMessage(
        "validate.help.auth.sp.passed", locale);
    String authSPFailed = ValidateSAML2.getMessage(
        "validate.help.auth.sp.failed", locale);
    String authSPAgain = ValidateSAML2.getMessage(
        "validate.auth.sp.again", locale);

    String accLinkTitle = ValidateSAML2.getMessage(
        "validate.title.account.linking", locale);
    String accLinkPassed = ValidateSAML2.getMessage(
        "validate.help.account.linking.passed", locale);
    String accLinkFailed = ValidateSAML2.getMessage(
        "validate.help.account.linking.failed", locale);

    String sloTitle = ValidateSAML2.getMessage(
        "validate.title.single.logout", locale);
    String sloPassed = ValidateSAML2.getMessage(
        "validate.help.single.logout.passed", locale);
    String sloFailed = ValidateSAML2.getMessage(
        "validate.help.single.logout.failed", locale);

    String ssoTitle = "";
    String ssoPassed = ValidateSAML2.getMessage(
        "validate.help.single.login.passed", locale);
    String ssoFailed = ValidateSAML2.getMessage(
        "validate.help.single.login.failed", locale);
    String ssoAgain = ValidateSAML2.getMessage(
        "validate.help.single.login.again", locale);

    String accTermTitle = ValidateSAML2.getMessage(
        "validate.title.account.termination", locale);
    String accTermPassed = ValidateSAML2.getMessage(
        "validate.help.account.termination.passed", locale);
    String accTermFailed = ValidateSAML2.getMessage(
        "validate.help.account.termination.failed", locale);

    ValidateSAML2 validator = null;

    try {
        validator = new ValidateSAML2(realm, idp, sp);
        {
            Object[] param = {validator.getIDPEntityId()};
            authIDPTitle = MessageFormat.format(validator.getMessage(
                "validate.title.auth.idp", locale), param);
        }
        {
            Object[] param = {validator.getSPEntityId()};
            authSPTitle = MessageFormat.format(validator.getMessage(
                "validate.title.auth.sp", locale), param);
        }
        if (validator.isIDPHosted()) {
            Object[] param = {validator.getIDPEntityId()};
            ssoTitle = MessageFormat.format(validator.getMessage(
                "validate.title.single.login_hosted", locale), param);
        } else {
            Object[] param = {validator.getSPEntityId()};
            ssoTitle = MessageFormat.format(validator.getMessage(
                "validate.title.single.login_remote", locale), param);
        }
    } catch (WorkflowException e) {
        setupFailedTitle = ValidateSAML2.getMessage(
            "validate.title.setup.failed", locale);
        setupError = e.getL10NMessage(Locale.getDefault());
    }
%>

<link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
<link rel="stylesheet" type="text/css" href="<%= deployuri %>/console/css/openam.css" />

<script language="JavaScript">
var statusAuthIdp = -1;
var statusAuthSp = -1;
var statusAccLink = -1;
var statusSLO = -1;
var statusSSO = -1;
var statusAccTerm = -1;

function onLoad() {
<%
    if (setupError.length() > 0) {
        out.println("document.getElementById('setupError').style.display='';");
        out.println("top.errorOccured();");
    } else {
        out.println("logout();");
        if (!validator.isFedlet()) {
            out.println("setTimeout(\"authIdp()\", 3000);");
        } else {
            out.println("setTimeout(\"singleLogin()\", 3000);");
        }
    }
%>
}

function logout() {
<%
    if (validator != null) {
        out.println("top.gotoHiddenFrame1('" +
            validator.getIDPLogoutURL() + "');");
        if (!validator.isFedlet()) {
            out.println("top.gotoHiddenFrame2('" +
                validator.getSPLogoutURL() + "');");
        }
    }
%>
}

function authIdp() {
    document.getElementById('authIdpFailed').style.display = 'none';
    document.getElementById('authSp').style.display = '';
    document.getElementById('accLink').style.display = '';
    document.getElementById('slo').style.display = '';
    document.getElementById('sso').style.display = '';
    document.getElementById('accTerm').style.display = '';
    document.getElementById('authIdpProcessing').style.display = '';
<%
    if ((validator != null) && !validator.isFedlet()) {
        out.println("top.gotoURL('" +
            validator.getIDPLoginURL() +
            "?goto=" + URLEncoder.encode(serverURL +
            "/validatorStatus.jsp?s=idpauth&v=1") +
            "&gotoOnFail=" + URLEncoder.encode(serverURL +
            "/validatorStatus.jsp?s=idpauth&v=-1") + "');");
        out.println("top.showFooter('validate.footer.auth.idp');");
    }
%>
    window.scrollTo(0, 0);
}

function authIdpPassed() {
    document.getElementById('authSp').style.display = 'none';
    document.getElementById('authIdpProcessing').style.display = 'none';
    document.getElementById('authIdpPassed').style.display = '';
    top.showFooter('');
    statusAuthIdp = 1;
    setTimeout("authSp()", 2000);
}

function authIdpFailed() {
    document.getElementById('authIdpProcessing').style.display = 'none';
    document.getElementById('authIdpFailed').style.display = '';
    top.showFooter('');
    statusAuthIdp = 0;
    top.gotoURL("validatorAuthFail.jsp?m=idp&locale=<%= locale %>");
}

function authSp() {
    document.getElementById('authSpFailed').style.display = 'none';
    document.getElementById('authSpProcessing').style.display = '';

<%
    if ((validator != null) && !validator.isFedlet())  {
        out.println("top.gotoURL('" +
            validator.getSPLoginURL() +
            "?goto=" + URLEncoder.encode(serverURL +
            "/validatorStatus.jsp?s=spauth&v=1") +
            "&gotoOnFail=" + URLEncoder.encode(serverURL +
            "/validatorStatus.jsp?s=spauth&v=-1") + "');");
        out.println("top.showFooter('validate.footer.auth.sp');");
    }
%>
    window.scrollTo(0, 50);
}

function authSpPassed() {
    document.getElementById('authSpProcessing').style.display = 'none';
    document.getElementById('authSpPassed').style.display = '';
    document.getElementById('accLink').style.display = 'none';
    top.showFooter('');
    statusAuthSp = 1;
    accountLinking();
}

function authSpFailed() {
    document.getElementById('authSpProcessing').style.display = 'none';
    document.getElementById('authSpFailed').style.display = '';
    top.showFooter('');
    statusAuthSp = 0;
    top.gotoURL("validatorAuthFail.jsp?m=sp");
}

function accountLinking() {
    document.getElementById('accLinkFailed').style.display = 'none';
    document.getElementById('accLinkProcessing').style.display = '';
<%
    if ((validator != null) && !validator.isFedlet()) {
        out.println("top.gotoURL('validateWait.jsp?locale=" + locale + "&m=" +
            URLEncoder.encode("validate.wait.account.linking") +
            "');");
        out.println("top.gotoHiddenFrame1('" + validator.getSSOURL() +
            "&RelayState=" + URLEncoder.encode(serverURL +
            "/validatorStatus.jsp?s=acclink&v=1") + "');");
        out.println("top.showFooter('validate.footer.account.linking');");
    }
%>
    window.scrollTo(0, 150);
    top.trackAccountLink();
}

function accLinkPassed() {
    document.getElementById('accLinkFailed').style.display = 'none';
    document.getElementById('accLinkProcessing').style.display = 'none';
    document.getElementById('accLinkPassed').style.display = '';
    document.getElementById('slo').style.display = 'none';
    top.showFooter('');
    statusAccLink = 1;
    singleLogout();
}

function accLinkFailed() {
    document.getElementById('accLinkProcessing').style.display = 'none';
    document.getElementById('accLinkFailed').style.display = '';
    top.showFooter('');
    statusAccLink = 0;
    getReport();
}

function singleLogout() {
    document.getElementById('sloFailed').style.display = 'none';
    document.getElementById('sloProcessing').style.display = '';
<%
    if ((validator != null) && !validator.isFedlet()) {
        out.println("top.gotoURL('validateWait.jsp?locale=" + locale + "&m=" +
            URLEncoder.encode("validate.wait.single.logout") + "');");
        out.println("top.gotoHiddenFrame1('" + validator.getSLOURL() +
            "&RelayState=" + URLEncoder.encode(serverURL +
            "/validatorStatus.jsp?s=slo&v=1") + "');");
        out.println("top.showFooter('validate.footer.single.logout');");
    }
%>
    window.scrollTo(0, 200);
    top.trackSingleLogout();
}

function sloPassed() {
    document.getElementById('sloFailed').style.display = 'none';
    document.getElementById('sloProcessing').style.display = 'none';
    document.getElementById('sloPassed').style.display = '';
    document.getElementById('slo').style.display = 'none';
    top.showFooter('');
    statusSLO = 1;
    singleLogin();
}

function sloFailed() {
    document.getElementById('sloProcessing').style.display = 'none';
    document.getElementById('sloFailed').style.display = '';
    top.showFooter('');
    statusSLO = 0;
    getReport();
}

function singleLogin() {
    document.getElementById('ssoFailed').style.display = 'none';
    document.getElementById('sso').style.display = 'none';
    document.getElementById('ssoProcessing').style.display = '';
<%
    if (validator != null ) {
        
        if( validator.isSalesforceSP() ) {
        
            // SF doesn't support relay states outside of their domain
            out.println("top.gotoURL('" + validator.getSSOURL() + "');");
            out.println("top.showFooter('validate.footer.single.login');");

        } else {
            
            out.println("top.gotoURL('" + validator.getSSOURL() +
                    "&RelayState=" + URLEncoder.encode(serverURL +
                    "/validatorStatus.jsp?s=sso&v=1&sendRedirectForValidationNow=true") + "');");
            out.println("top.showFooter('validate.footer.single.login');");

        }
        
    }
%>
    window.scrollTo(0, 250);
    top.trackSingleLogin();
}

function getReport() {
    var url = "validatorRpt.jsp";
    <%
        if (validator != null) {
            out.println("url += '?idp=' + '" +
                URLEncoder.encode(validator.getIDPEntityId()) + "';");
            if (validator.isFedlet()) {
                out.println("url += '&fedlet=' + '" +
                    URLEncoder.encode(validator.getSPEntityId()) + "';");
            } else {
                out.println("url += '&sp=' + '" +
                    URLEncoder.encode(validator.getSPEntityId()) + "';");
            }
        }
    %>
    if (statusAuthIdp > -1) {
        url += '&authidp=' + statusAuthIdp;
    }

    if (statusAuthSp >  -1) {
        url += '&authsp=' + statusAuthSp;
    }
    if (statusAccLink > -1) {
        url += '&acclink=' + statusAccLink;
    }
    if (statusSLO > -1) {
        url += '&slo=' + statusSLO;
    }
    if (statusSSO > -1) {
        url += '&sso=' + statusSSO;
    }
    if (statusAccTerm > -1) {
        url += '&accterm=' + statusAccTerm;
    }

    url += "&locale=<%= locale %>";

    top.gotoURL(url);
}

function ssoPassed() {
    document.getElementById('ssoFailed').style.display = 'none';
    document.getElementById('ssoProcessing').style.display = 'none';
    document.getElementById('ssoPassed').style.display = '';
    document.getElementById('sso').style.display = 'none';
    document.getElementById('accTerm').style.display = 'none';
    top.showFooter('');
    statusSSO = 1;
    <%
    if (validator != null) {
        if (!validator.isFedlet()) {
            out.println("accTermination();");
        } else {
            out.println("getReport();");
        }
    }
    %>
}

function ssoFailed() {
    document.getElementById('sso').style.display = 'none';
    document.getElementById('ssoProcessing').style.display = 'none';
    document.getElementById('ssoFailed').style.display = '';
    top.showFooter('');
    statusSSO = 0;
    top.gotoURL("validatorAuthFail.jsp?m=sso&locale=<%= locale %>");
}

function accTermPassed() {
    document.getElementById('accTermFailed').style.display = 'none';
    document.getElementById('accTermProcessing').style.display = 'none';
    document.getElementById('accTermPassed').style.display = '';
    document.getElementById('accTerm').style.display = 'none';
    top.showFooter('');
    statusAccTerm = 1;
}

function accTermFailed() {
    document.getElementById('accTermProcessing').style.display = 'none';
    document.getElementById('accTermFailed').style.display = '';
    top.showFooter('');
    statusAccTerm = 0;
    getReport();
}

function accTermination() {
    document.getElementById('accTermFailed').style.display = 'none';
    document.getElementById('accTermProcessing').style.display = '';
<%
    if (validator != null) {
        out.println("top.gotoURL('validateWait.jsp?locale=" + locale + "&m=" +
            URLEncoder.encode("validate.wait.account.termination") + "');");
        out.println("top.gotoHiddenFrame1('" +
            validator.getAccountTerminationURL() +
            "&RelayState=" + URLEncoder.encode(serverURL +
                "/validatorStatus.jsp?s=accTerm&v=1") + "');");
        out.println("top.showFooter('validate.footer.account.termination');");
    }
%>
    window.scrollTo(0, 250);
    top.trackAccountTermination();
}
</script>
</head>

<body class="DefBdy" onLoad="onLoad();">
<table border=0 cellpadding=2 cellspacing=10 width="100%">
<tr>
<td bgcolor="#666699">
    <table border=0 cellpadding=1 cellspacing=0 width="100%">
    <tr>
    <td bgcolor="#FFFFFF">

<!-- content -->
    <table border=0 cellpadding=10 cellspacing=0>
    <tr>
    <td>
        <div id="setupError" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progressfailed.gif" width="28" height="26" /></td>
        <td><span class="ProgressFailedTitle" id="ErrorTitle"><%= setupFailedTitle %></span></td>
        </tr>
        <tr><td></td>
        <td><span class="ProgressText" id="ErrorText"><%= ESAPI.encoder().encodeForHTML(setupError) %></span>
        </tr>
        </table>
        </div>

        <div id="authIdpProcessing" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progresspointer.gif" width="33" height="33" /></td>
        <td><span class="ProgressTitle"><%=ESAPI.encoder().encodeForHTML(authIDPTitle) %></span></td>
        </tr>
        </table>
        </div>

        <div id="authIdpPassed" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progresspassed.gif" width="28" height="26" /></td>
        <td><span class="ProgressDoneTitle"><%=ESAPI.encoder().encodeForHTML(authIDPPassed) %></span></td>
        </tr>
        </table>
        </div>

        <div id="authIdpFailed" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progressfailed.gif" width="28" height="26" /></td>
        <td><span class="ProgressFailedTitle"><%= ESAPI.encoder().encodeForHTML(authIDPFailed) %></span></td>
        </tr>
        </table>
        </div>

        <div id="authSp" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><div><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="" border="0" height="10" width="30" /></div></td>
        <td><span class="ProgressText"><%=ESAPI.encoder().encodeForHTML(authSPTitle) %></span></td>
        </tr>
        </table>
        </div>

        <div id="authSpProcessing" style="width:100%;display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progresspointer.gif" width="33" height="33" /></td>
        <td><span class="ProgressTitle"><%= ESAPI.encoder().encodeForHTML(authSPTitle) %></span><br /></td>
        </tr>
        </table>
        </div>

        <div id="authSpPassed" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progresspassed.gif" width="28" height="26" /></td>
        <td><span class="ProgressDoneTitle"><%=ESAPI.encoder().encodeForHTML(authSPPassed) %></span></td>
        </tr>
        </table>
        </div>

        <div id="authSpFailed" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progressfailed.gif" width="28" height="26" /></td>
        <td><span class="ProgressFailedTitle"><%=ESAPI.encoder().encodeForHTML(authSPFailed) %></span></td>
        </tr>
        </table>
        </div>

        <div id="accLink" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><div><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="" border="0" height="10" width="30" /></div></td>
        <td><span class="ProgressText"><%= ESAPI.encoder().encodeForHTML(accLinkTitle) %></span></td>
        </tr>
        </table>
        </div>

        <div id="accLinkProcessing" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progresspointer.gif" width="33" height="33" /></td>
        <td><span class="ProgressTitle"><%= ESAPI.encoder().encodeForHTML(accLinkTitle) %></span><br /></td>
        </tr>
        </table>
        </div>

        <div id="accLinkPassed" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progresspassed.gif" width="28" height="26" /></td>
        <td><span class="ProgressDoneTitle"><%=ESAPI.encoder().encodeForHTML(accLinkPassed) %></span></td>
        </tr>
        </table>
        </div>

        <div id="accLinkFailed" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progressfailed.gif" width="28" height="26" /></td>
        <td><span class="ProgressFailedTitle"><%=ESAPI.encoder().encodeForHTML(accLinkFailed) %></span></td>
        </tr>
        </table>
        </div>

        <div id="slo" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><div><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="" border="0" height="10" width="30" /></div></td>
        <td><span class="ProgressText"><%= ESAPI.encoder().encodeForHTML(sloTitle) %></span></td>
        </tr>
        </table>
        </div>

        <div id="sloProcessing" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progresspointer.gif" width="33" height="33" /></td>
        <td><span class="ProgressTitle"><%= ESAPI.encoder().encodeForHTML(sloTitle) %></span><br /></td>
        </tr>
        </table>
        </div>

        <div id="sloPassed" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progresspassed.gif" width="28" height="26" /></td>
        <td><span class="ProgressDoneTitle"><%=ESAPI.encoder().encodeForHTML(sloPassed) %></span></td>
        </tr>
        </table>
        </div>

        <div id="sloFailed" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progressfailed.gif" width="28" height="26" /></td>
        <td><span class="ProgressFailedTitle"><%=ESAPI.encoder().encodeForHTML(sloFailed) %></span></td>
        </tr>
        </table>
        </div>


        <div id="sso" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><div><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="" border="0" height="10" width="30" /></div></td>
        <td><span class="ProgressText"><%= ESAPI.encoder().encodeForHTML(ssoTitle) %></span></td>
        </tr>
        </table>
        </div>

        <div id="ssoProcessing" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progresspointer.gif" width="33" height="33" /></td>
        <td><span class="ProgressTitle"><%= ESAPI.encoder().encodeForHTML(ssoTitle) %></span><br /></td>
        </tr>
        </table>
        </div>

        <div id="ssoPassed" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progresspassed.gif" width="28" height="26" /></td>
        <td><span class="ProgressDoneTitle"><%=ESAPI.encoder().encodeForHTML(ssoPassed) %></span></td>
        </tr>
        </table>
        </div>

        <div id="ssoFailed" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progressfailed.gif" width="28" height="26" /></td>
        <td><span class="ProgressFailedTitle"><%=ESAPI.encoder().encodeForHTML(ssoFailed) %></span></td>
        </tr>
        </table>
        </div>

        <div id="accTerm" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><div><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="" border="0" height="10" width="30" /></div></td>
        <td><span class="ProgressText"><%= ESAPI.encoder().encodeForHTML(accTermTitle) %></span></td>
        </tr>
        </table>
        </div>

        <div id="accTermProcessing" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progresspointer.gif" width="33" height="33" /></td>
        <td><span class="ProgressTitle"><%= ESAPI.encoder().encodeForHTML(accTermTitle) %></span><br /></td>
        </tr>
        </table>
        </div>

        <div id="accTermPassed" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progresspassed.gif" width="28" height="26" /></td>
        <td><span class="ProgressDoneTitle"><%=ESAPI.encoder().encodeForHTML(accTermPassed) %></span></td>
        </tr>
        </table>
        </div>

        <div id="accTermFailed" style="display:none">
        <table border=0 cellpadding=0 cellspacing=2>
        <tr><td><img src="<%= deployuri %>/console/images/progressfailed.gif" width="28" height="26" /></td>
        <td><span class="ProgressFailedTitle"><%=ESAPI.encoder().encodeForHTML(accTermFailed) %></span></td>
        </tr>
        </table>
        </div>

    </td>
    </tr>
    </table>

<!-- content -->
    </td>
    </tr>
    </table>
</td>
</tr>
</table>
       
</body>
</html>
