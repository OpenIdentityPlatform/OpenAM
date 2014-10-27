<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

   Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved

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

   $Id: Debug.jsp,v 1.15 2009/01/28 05:35:06 ww203982 Exp $

--%>

<%--
   Portions copyright 2010-2014 ForgeRock AS.
--%>

<%@ page pageEncoding="UTF-8" %>
<%@ page 
    import="
        com.iplanet.sso.SSOToken,
        com.sun.identity.shared.debug.Debug,
        com.sun.identity.shared.encode.Hash,
        java.text.MessageFormat,
        java.util.ArrayList,
        java.util.Enumeration,
        java.util.Collections,
        java.util.HashMap,
        java.util.Iterator,
        java.util.List,
        java.util.Map,
        java.util.MissingResourceException,
        java.util.ResourceBundle,
        org.owasp.esapi.ESAPI"
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>OpenAM</title>
    <link rel="stylesheet" type="text/css" href="com_sun_web_ui/css/css_ns6up.css" />
    <link rel="shortcut icon" href="com_sun_web_ui/images/favicon/favicon.ico" type="image/x-icon" />
</head>
<body class="DefBdy">
    <div class="SkpMedGry1"><a href="#SkipAnchor3860"><img src="com_sun_web_ui/images/other/dot.gif" alt="Jump to End of Masthead" border="0" height="1" width="1" /></a></div><div class="MstDiv">
    <table class="MstTblBot" title="" border="0" cellpadding="0" cellspacing="0" width="100%">
        <tr>
        <td class="MstTdTtl">
        <div class="MstDivTtl"><img name="AMConfig.configurator.ProdName" src="com_sun_web_ui/images/PrimaryProductName.png" alt="OpenAM" border="0" /></div>
        </td>
        </tr>
    </table>
    </div>
    <table class="SkpMedGry1" border="0" cellpadding="5" cellspacing="0" width="100%"><tr><td><img src="com_sun_web_ui/images/other/dot.gif" alt="Jump to End of Masthead" border="0" height="1" width="1" /></a></td></tr></table>
    <table border="0" cellpadding="10" cellspacing="0" width="100%"><tr><td></td></tr></table>

<%@ include file="/WEB-INF/jsp/admincheck.jsp" %>
<%

    SSOToken ssoToken = requireAdminSSOToken(request, response, out, "showServerConfig.jsp");
    if (ssoToken == null) {
%>
</body></html>
<%
        return;
    }

    String category = request.getParameter("category");
    String instance = request.getParameter("instance");
    String level = request.getParameter("level");
    if (!ESAPI.validator().isValidInput("category", category, "HTTPParameterValue", 512, true)
            || !ESAPI.validator().isValidInput("instance", instance, "HTTPParameterValue", 512, true)
            || !ESAPI.validator().isValidInput("level", level, "HTTPParameterValue", 512, true)) {
        //Invalid values received, let's null them out and ignore them.
        category = null;
        instance = null;
        level = null;
    }
    boolean performAction = Boolean.valueOf(request.getParameter("do"));

    ResourceBundle resourceBundle = ResourceBundle.getBundle("debug", request.getLocale());
    ResourceBundle rbFiles = ResourceBundle.getBundle("debugfiles");
    Map categories = new HashMap();
    List<String> instances = new ArrayList<String>();
    String formToken;
    try {

        formToken = Hash.hash(ssoToken.getTokenID().toString());

        // Make a copy to prevent ConcurrentModificationException
        List<Debug> temp = new ArrayList<Debug>(Debug.getInstances());
        for (Debug debug : temp) {
            instances.add(debug.getName());
        }
        Collections.sort(instances);
        for (Enumeration e = rbFiles.getKeys(); e.hasMoreElements();) {
            String key = (String)e.nextElement();
            String val = rbFiles.getString(key);
            List lst = (List) categories.get(val);
            if (lst == null) {
                lst = new ArrayList();
            }
            lst.add(key);
            categories.put(val, lst);
        }

    } catch (MissingResourceException e) {
        out.println(e.getMessage());
        return;
    }

    if (performAction) {
        String receivedToken = request.getParameter("formToken");
        if (!formToken.equals(receivedToken)) {
            out.println("Invalid form token provided!");
            return;
        }
    }
%>

<table cellpadding=5>
<tr>
<td>

<%
if ((instance == null || instance.length() == 0) && (category == null || category.length() == 0)
    || level == null || level.length() == 0) {
%>
<form name="frm" action="Debug.jsp" method="POST">
<table>
<tr>
<td>
<%
    out.println(resourceBundle.getString("label-category"));
%>
:</td>
<td>
<select name="category">
<%
    for (Iterator i = categories.keySet().iterator(); i.hasNext(); ) {
        String key = (String)i.next();
        out.println("<option>" + key + "</option>");
    }
%>
</select>
</td>
<td>&nbsp;&nbsp;</td>
<td>
<%
    out.println(resourceBundle.getString("label-level"));
%>
:</td>
<td>
<select name="level">
<%
    out.println("<option value=\"1\">" +
        resourceBundle.getString("label-level-error") + "</option>");
    out.println("<option value=\"2\">" +
        resourceBundle.getString("label-level-warning") + "</option>");
    out.println("<option value=\"3\">" +
        resourceBundle.getString("label-level-message") + "</option>");
%>
</select>
</td>
<td><input type="submit" value="Submit" class="Btn1" onclick="this.form.submit();" onmouseover="javascript: this.className='Btn1Hov'" onmouseout="javascript: this.className='Btn1'" onblur="javascript: javascript: this.className='Btn1'" onfocus="javascript: this.className='Btn1Hov'" /> 
</td>
</tr>
</table>
</form>

<form name="frm" action="Debug.jsp" method="POST">
<table>
<tr>
<td>
<%
    out.println(resourceBundle.getString("label-instance"));
%>
:</td>
<td>
<select name="instance">
<%
    for (String inst : instances) {
        out.println("<option>" + inst + "</option>");
    }
%>
</select>
</td>
<td>&nbsp;&nbsp;</td>
<td>
<%
    out.println(resourceBundle.getString("label-level"));
%>
:</td>
<td>
<select name="level">
<%
    out.println("<option value=\"1\">" +
        resourceBundle.getString("label-level-error") + "</option>");
    out.println("<option value=\"2\">" +
        resourceBundle.getString("label-level-warning") + "</option>");
    out.println("<option value=\"3\">" +
        resourceBundle.getString("label-level-message") + "</option>");
%>
</select>
</td>
<td><input type="submit" value="Submit" class="Btn1" onclick="this.form.submit();" onmouseover="javascript: this.className='Btn1Hov'" onmouseout="javascript: this.className='Btn1'" onblur="javascript: javascript: this.className='Btn1'" onfocus="javascript: this.className='Btn1Hov'" /> 
</td>
</tr>
</table>
</form>

<p>&nbsp;</p>

<table border=1 cellpadding=5 cellspacing=0>
<tr>
<th>
<%
    out.println(resourceBundle.getString("label-category"));
%>
</th>
<th>
<%
    out.println(resourceBundle.getString("label-filenames"));
%>
</th>
</tr>
<%
    for (Iterator i = categories.keySet().iterator(); i.hasNext(); ) {
        String key = (String)i.next();
        out.println("<tr><td valign=top><b>" + key + "</b></td>");
        List values = (List)categories.get(key);
        out.println("<td>");
        String msg = "message-category-"+key; 
        try {
            msg = resourceBundle.getString("message-category-"+key); 
        } catch (Exception ex) {}
        out.println("<b>"+msg+"</b><br>");
        out.print("<span class=\"HlpFldTxt\">");   
        for (Iterator j = values.iterator(); j.hasNext(); ) {
            out.println((String)j.next() + " " );
        }
        out.print("</span>");
        out.println("</td></tr>");
    }
%>
</table>

<%
} else {
    if (category != null) {
        out.println(resourceBundle.getString("label-category") + " = " + category);
    } else {
        out.println("Instance" + " = " + instance);
    }
    out.println("<br />");
    String strLevel = "message";
    if (level.equals("1")) {
        strLevel = "error";
    } else if (level.equals("2")) {
        strLevel = "warning";
    }

    out.println(resourceBundle.getString("label-level") + " = " + strLevel);
    out.println("<br />");
    int levelint = Integer.parseInt(level);
    if (category != null) {
        Object[] param = {strLevel};
        out.println(MessageFormat.format(
            resourceBundle.getString("message-setting-level-on-modules"), param));
        out.println("<br />");
        List values = (List)categories.get(category);
        out.println("<ul>");
        for (Iterator i = values.iterator(); i.hasNext(); ) {
            String mname = (String)i.next();
            out.println( "<li>" + mname + "</li>" );

            if (performAction) {
                Debug debug = Debug.getInstance(mname); 
                debug.setDebug(levelint);
            }
        }
        out.println("</ul>");
    } else {
        Object[] param = {strLevel, instance};
        out.println(MessageFormat.format(
            resourceBundle.getString("message-setting-level-on-instance"), param));
        if (performAction) {
            Debug.getInstance(instance).setDebug(levelint);
        }
    }

    String backURL = "Debug.jsp";

    if (!performAction) {
        out.println("<form name='frm' method='POST' action='Debug.jsp'>");
        if (category != null) {
            out.println("<input name='category' type='hidden' value='" + category + "' />");
        } else {
            out.println("<input name='instance' type='hidden' value='" + instance + "' />");
        } 
        out.println("<input name='level' type='hidden' value='" + levelint + "' />");
        out.println("<input name='do' type='hidden' value='true' />");
        out.println("<input type='hidden' name='formToken' value='" + formToken + "' />");
        out.println("<table border=0>");
        out.println("<tr><td>");
        out.println("<input type=\"button\" name=\"do\" value=\"" + resourceBundle.getString("button-confirm") + "\" class=\"Btn1\" onclick=\"this.form.submit();\" onmouseover=\"javascript: this.className='Btn1Hov'\" onmouseout=\"javascript: this.className='Btn1'\" onblur=\"javascript: javascript: this.className='Btn1'\" onfocus=\"javascript: this.className='Btn1Hov'\" /></form>");
        out.println("</td><td>");
    out.println("<input type=\"button\" name=\"back\" value=\"" + resourceBundle.getString("button-back") + "\" class=\"Btn1\" onclick=\"var elements=this.form.elements;for (var i=0;i<elements.length;i++){if(elements[i].type && elements[i].type==='hidden'){elements[i].value=''}};this.form.submit();\" onmouseover=\"javascript: this.className='Btn1Hov'\" onmouseout=\"javascript: this.className='Btn1'\" onblur=\"javascript: this.className='Btn1'\" onfocus=\"javascript: this.className='Btn1Hov'\" />");
        out.println("</td></tr></table>");
        out.println("</form>");

    } else {
        Object[] params = {backURL};
        out.println("<p>");
        out.println(MessageFormat.format(
            resourceBundle.getString("message-succeed"), params));
    }
}
%>

</td></tr>
</table>
</body>
</html>

