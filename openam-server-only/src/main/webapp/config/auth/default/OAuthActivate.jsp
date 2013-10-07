<%--
   DO  NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
  
   The contents of this file are subject to the terms
   of the Common Development and Distribution License
   (the License). You may not use this file except in
   compliance with the License.
                                                                                
   You can obtain a copy of the License at
   http://forgerock.org/license/CDDLv1.0.html 
   See the License for the specific language governing
   permission and limitations under the License.
                                                                                
   When distributing Covered Code, include this CDDL
   Header Notice in each file and include the License file
   at http://forgerock.org/license/CDDLv1.0.html
   If applicable, add the following below the CDDL Header,
   with the fields enclosed by brackets [] replaced by
   your own identifying information:
   "Portions Copyrighted [year] [name of copyright owner]"
                                                              
--%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<%@ page language="java" %>
<%@ page import="org.owasp.esapi.ESAPI" %>
<%@ page import="com.iplanet.am.util.SystemProperties" %>
<%@ page import="com.sun.identity.shared.Constants" %>
<%@ page import="static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.*" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="java.util.MissingResourceException" %>
<%@ page import="java.util.Locale" %>
<%@ page import="org.forgerock.openam.authentication.modules.oauth2.OAuthUtil" %>
<%@ page import="org.owasp.esapi.ESAPI" %>

<%
    // Internationalization stuff. This is just an example. You can use any i18n framework
    // as long as you use amAuthOAuth as the resource bundle.
    String ServiceURI = SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
    String lang = request.getParameter("lang");
    ResourceBundle resources;
    Locale locale = null;
    try {
        if (lang != null && lang.length() != 0) {
            locale = new Locale(lang);
        } else {
            locale = request.getLocale();
        }
        resources = ResourceBundle.getBundle("amAuthOAuth", locale);
        OAuthUtil.debugMessage("OAuthActivate: obtained resource bundle with locale " + locale);
    } catch (MissingResourceException mr) {
        OAuthUtil.debugError("OAuthActivate:: Resource Bundle not found", mr);
        resources = ResourceBundle.getBundle("amAuthOAuth");
    }

    String logoutURL = request.getParameter(PARAM_GOTO);
    if (logoutURL == null) {
        logoutURL = ServiceURI;
    } else {
        boolean isValidURL = ESAPI.validator().
                isValidInput("URLContext", logoutURL, "URL", 255, false);
        if (!isValidURL) {
            OAuthUtil.debugError("OAuthPwd: wrong logoutURL URL attempted to be used "
                    + "in the OAuthPwd page: " + logoutURL);
            logoutURL = ServiceURI;
        }
    }

    String activationTitle = resources.getString("activationTitle");
    String emptyCode = ESAPI.encoder().encodeForHTML(resources.getString("emptyCode"));
    String errInvalidCode = ESAPI.encoder().encodeForHTML(resources.getString("errInvalidCode"));
    String activation = ESAPI.encoder().encodeForHTML(PARAM_ACTIVATION);
    String activationLabel = resources.getString("activationLabel");
    String activationCodeMsg = resources.getString("activationCodeMsg");
    String submitValue = ESAPI.encoder().encodeForHTML(resources.getString("submit"));
    String submitButton = ESAPI.encoder().encodeForHTML("Submit");
    String cancelValue = ESAPI.encoder().encodeForHTML(resources.getString("cancel"));
    String outputField = ESAPI.encoder().encodeForHTML("output");
    String emptyField = ESAPI.encoder().encodeForHTML("");
%>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
    <link href="<%= ServiceURI%>/css/new_style.css" rel="stylesheet" type="text/css"/>
    <!--[if IE 9]>
    <link href="<%= ServiceURI%>/css/ie9.css" rel="stylesheet" type="text/css"> <![endif]-->
    <!--[if lte IE 7]>
    <link href="<%= ServiceURI%>/css/ie7.css" rel="stylesheet" type="text/css"> <![endif]-->
    <script language="JavaScript" src="<%= ServiceURI %>/js/auth.js"></script>
    <script language="JavaScript">

        function validateEntry(form, activation, submit) {
            var out = document.getElementById("msgcnt");
            out.innerHTML = "";
            if (form.elements[submit].value == '<%= cancelValue %>') {
                return false;
            }
            if (form.elements[activation].value == '') {
                out.innerHTML = "<%= emptyCode %>";
                form.elements[activation].focus();
                return false;
            }
            var re = /^[a-zA-Z0-9.\\-\\/+=_ ]*$/
            if (!re.test(form.elements[activation].value)) {
                out.innerHTML = "<%= errInvalidCode %>";
                form.elements[activation].focus();
                return false;
            }
            out.innerHTML = "<%= ESAPI.encoder().encodeForHTML("") %>";
            return true;
        }

        function adios() {
            window.location = "<%= logoutURL %>";
        }

    </script>
    <title><%= activationTitle %>
    </title>
</head>
<body>
<div class="container_12">
    <div class="grid_4 suffix_8">
        <a class="logo" href="<%= ServiceURI%>"></a>
    </div>
    <div class="box clear-float">
        <div class="grid_3">
            <div class="product-logo"></div>
        </div>
        <div class="grid_9 left-seperator">
            <div class="box-content clear-float">
                <h1>Activation Code</h1>

                <p class="message"><span class="icon info"></span><span id="msgcnt"><%= activationCodeMsg %></span></p>

                <form name="Login" method="POST" action="<%= emptyField %>"
                      onsubmit="return validateEntry(this,'<%= activation %>' ,
                              '<%= submitButton %>');">
                    <fieldset>
                        <div class="row"><label for="<%= activation %>"><%=activationLabel%>
                        </label><input class="textbox" type="text" id="<%= activation %>" name="<%= activation %>"/>
                        </div>
                        <div class="row">
                            <input type="submit" class="button primary" value="<%= submitValue %>"
                                   name="<%= submitButton %>"/>
                            <input type="button" class="button" value="<%= cancelValue %>" name="<%= submitButton %>"
                                   onclick="adios(); return false;"/>
                        </div>
                    </fieldset>
                </form>
            </div>
        </div>
    </div>
    <div class="footer alt-color">
        <div class="grid_3">
            <p>Copyright &copy; 208-2013, ForgeRock AS. <br/>All Rights Reserved. Use of this software is subject to the
                terms and conditions of the ForgeRock&trade; License and Subscription Agreement.</p>
        </div>
    </div>
</div>
</body>
</html>
