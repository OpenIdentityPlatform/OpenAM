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

   $Id: validatorRpt.jsp,v 1.4 2009/01/05 23:23:25 veiming Exp $

--%>

<%--
   Portions Copyrighted 2013 ForgeRock AS
--%>

<%@ page import="com.sun.identity.common.SystemConfigurationUtil" %>
<%@ page import="com.sun.identity.shared.Constants" %>
<%@ page import="com.sun.identity.workflow.ValidateSAML2" %>
<%@ page import="org.owasp.esapi.ESAPI" %>
<%@ page contentType="text/html; charset=utf-8" language="java" %>

<html>
<head>

<%
    String deployuri = SystemConfigurationUtil.getProperty(
        Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
    String idp = request.getParameter("idp");
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + idp, idp,"HTTPParameterValue", 2000, false)) {
            idp = "";
    }
    String sp = request.getParameter("sp");
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + sp, sp, "HTTPParameterValue", 2000, false)) {
            sp = "";
    }
    String fedlet = request.getParameter("fedlet");
    if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + fedlet, fedlet, "HTTPParameterValue", 2000, true)) {
            fedlet = null;
    }
    boolean bFedlet = (fedlet != null);

    String authidp = request.getParameter("authidp");
    boolean bAuthidp = (authidp != null) && (authidp.length() > 0);
    boolean bAuthidpPassed = bAuthidp && authidp.equals("1");

    String authsp = request.getParameter("authsp");
    boolean bAuthsp = (authsp != null) && (authsp.length() > 0);
    boolean bAuthspPassed = bAuthsp && authsp.equals("1");

    String acclink = request.getParameter("acclink");
    boolean bAcclink = (acclink != null) && (acclink.length() > 0);
    boolean bAcclinkPassed = bAcclink && acclink.equals("1");

    String slo = request.getParameter("slo");
    boolean bSLO = (slo != null) && (slo.length() > 0);
    boolean bSLOPassed = bSLO && slo.equals("1");

    String sso = request.getParameter("sso");
    boolean bSSO = (sso != null) && (sso.length() > 0);
    boolean bSSOPassed = bSSO && sso.equals("1");

    String accterm = request.getParameter("accterm");
    boolean bAccTerm = (accterm != null) && (accterm.length() > 0);
    boolean bAccTermPassed = bAccTerm && accterm.equals("1");

    String locale = request.getParameter("locale");
%>

<link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
<link rel="stylesheet" type="text/css" href="<%= deployuri %>/console/css/openam.css" />

<script language="JavaScript">
</script>
</head>

<body class="DefBdy">
    <div style="padding:10px">
    <div style="height:20px; width:100%; background-color:white">&nbsp;</div>

<table border=0 cellpadding=5 cellspacing=0 width="100%">
<tr>
<td bgcolor="#949ea5">
<%
    out.print("<div class=\"ConFldSetLgdDiv\" style=\"color:#FFFFFF\">&#160;");
    out.print(ValidateSAML2.getMessage("validate.report.title", locale));
    out.print("</div>");
%>
</td>
</tr>
</table>
<table border=0 cellpadding=10 cellspacing=0 width="100%">
<tr>
<td bgcolor="#c0c6cf">
    <table border=0 cellpadding=1 cellspacing=0 width="100%">
    <tr>
    <td bgcolor="#999999">
    <table border=0 cellpadding=1 cellspacing=0 width="100%">
    <tr>
    <td bgcolor="#e9ecee">
        <table border=0 cellpadding=2 cellspacing=0 width="100%">
        <tr>
        <td><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="" border="0" height="5" width="1" /></td>
        </tr>
        <tr>
        <td width="1%" nowrap><b>
        <%
            out.println(ValidateSAML2.getMessage(
                "validate.report.task.label.idp", locale));
        %>
        :</b></td>

        <td width="99%"><%= idp %></td>
        </tr>
        <tr>
        <td width="1%" nowrap>
        <%
            if (bFedlet) {
                out.print("<b>" + ValidateSAML2.getMessage(
                    "validate.report.task.label.fedlet", locale) + ":</b>");
            } else {
                out.print("<b>" + ValidateSAML2.getMessage(
                    "validate.report.task.label.sp", locale) + ":</b>");
            }
        %>
        </td>
        <td width="99%">
        <%
            if (fedlet != null) {
                out.print(fedlet);
            } else {
                out.print(sp);
            }
        %>
        </td>
        </tr>
        <tr>
        <td><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="" border="0" height="5" width="1" /></td>
        </tr>
        </table>
    </td></tr>
    </table>
    </td></tr>
    </table>

    <table border=0 cellpadding=0 cellspacing=0 width="100%">
    <tr>
    <td bgcolor="#999999">
        <table border=0 cellpadding=5 cellspacing=1 width="100%">
        <tr style="background-color:#e9ecee">
<%
        out.println("<th width=\"1%\">" +
            ValidateSAML2.getMessage("validate.report.tbl.hdr.test", locale) +
            "</th>");
        out.println("<th width=\"99%\">" +
            ValidateSAML2.getMessage("validate.report.tbl.hdr.result", locale) +
            "</th>");
        out.println("</tr>");

    if (!bFedlet) {
        out.println("<tr style=\"background-color:#FFFFFF\">");
        out.println("<td nowrap=\"true\">" +
            ValidateSAML2.getMessage("validate.report.task.auth.idp", locale) +
            "</td>");
        if (bAuthidp) {
            if (bAuthidpPassed) {
                out.println("<td>" + ValidateSAML2.getMessage("validator.report.auth.idp.passed", locale) + "</td>");
            } else {
                out.println("<td>" + ValidateSAML2.getMessage("validator.report.auth.idp.failed", locale) + "</td>");
            }
        } else {
            out.println("<td>" + ValidateSAML2.getMessage("validator.report.auth.idp.not.tested", locale) + "</td>");
        }
        out.println("</tr>");

        out.println("<tr style=\"background-color:#FFFFFF\">");
        out.println("<td nowrap=\"true\">" +
            ValidateSAML2.getMessage("validate.report.task.auth.sp", locale) + "</td>");
        if (bAuthsp) {
            if (bAuthspPassed) {
                out.println("<td>" + ValidateSAML2.getMessage("validator.report.auth.sp.passed", locale) + "</td>");
            } else {
                out.println("<td>" + ValidateSAML2.getMessage("validator.report.auth.sp.failed", locale) + "</td>");
            }
        } else {
            out.println("<td>" + ValidateSAML2.getMessage("validator.report.auth.sp.not.tested", locale) + "</td>");
        }

        out.println("</tr>");
        out.println("<tr style=\"background-color:#FFFFFF\">");
        out.println("<td nowrap=\"true\">" +
            ValidateSAML2.getMessage("validate.report.task.account.linking", locale) +
            "</td>");
        if (bAcclink) {
            if (bAcclinkPassed) {
                out.println("<td>" + ValidateSAML2.getMessage("validator.report.account.linking.passed", locale) + "</td>");
            } else {
                out.println("<td>" + ValidateSAML2.getMessage("validator.report.account.linking.failed", locale) + "</td>");
            }
        } else {
            out.println("<td>" + ValidateSAML2.getMessage("validator.report.account.linking.not.tested", locale) + "</td>");
        }
        out.println("</tr>");

        out.println("<tr style=\"background-color:#FFFFFF\">");
        out.println("<td nowrap=\"true\">" +
            ValidateSAML2.getMessage("validate.report.task.single.logout", locale) +
            "</td>");
        if (bSLO) {
            if (bSLOPassed) {
                out.println("<td>" + ValidateSAML2.getMessage("validator.report.single.logout.passed", locale) + "</td>");
            } else {
                out.println("<td>" + ValidateSAML2.getMessage("validator.report.single.logout.failed", locale) + "</td>");
            }
        } else {
            out.println("<td>" + ValidateSAML2.getMessage("validator.report.single.logout.not.tested", locale) + "</td>");
        }
        out.println("</tr>");
    }
%>
        <tr style="background-color:#FFFFFF">
<%
        out.println("<td nowrap=\"true\">" +
            ValidateSAML2.getMessage("validate.report.task.single.login", locale) +
            "</td>");
        if (bSSO) {
            if (bSSOPassed) {
                out.println("<td>" + ValidateSAML2.getMessage("validator.report.single.login.passed", locale) + "</td>");
            } else {
                out.println("<td>" + ValidateSAML2.getMessage("validator.report.single.login.failed", locale) + "</td>");
            }
        } else {
            out.println("<td>" + ValidateSAML2.getMessage("validator.report.single.login.not.tested", locale) + "</td>");
        }
%>
        </tr>

<%
    if (!bFedlet) {
        out.println("<tr style=\"background-color:#FFFFFF\">");
        out.println("<td nowrap=\"true\">" + ValidateSAML2.getMessage("validate.report.task.account.termination", locale) + "</td>");
        if (bAccTerm) {
            if (bAccTermPassed) {
                out.println("<td>" + ValidateSAML2.getMessage("validator.report.account.termination.passed", locale) + "</td>");
            } else {
                out.println("<td>" + ValidateSAML2.getMessage("validator.report.account.termination.failed", locale) + "</td>");
            }
        } else {
                out.println("<td>" + ValidateSAML2.getMessage("validator.report.account.termination.not.tested", locale) + "</td>");
        }
        out.println("</tr>");
    }
%>
        </table>
    </td></tr>
    </table>
</td></tr>
</table>
</div>
       
</body>
</html>
