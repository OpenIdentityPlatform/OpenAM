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

<% // Internationalization
    String ServiceURI = SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
    String termsAndConditionsPage = ServiceURI + "/tc.html";
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
        OAuthUtil.debugMessage("OAuthPwd: obtained resource bundle with locale " + locale);
    } catch (MissingResourceException mr) {
        OAuthUtil.debugError("OAuthPwd: Resource Bundle not found", mr);
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

    String errLength = ESAPI.encoder().encodeForHTML(resources.getString("errLength"));
    String errNumbers = ESAPI.encoder().encodeForHTML(resources.getString("errNumbers"));
    String errLowercase = ESAPI.encoder().encodeForHTML(resources.getString("errLowercase"));
    String errUppercase = ESAPI.encoder().encodeForHTML(resources.getString("errUppercase"));
    String errInvalidPass = ESAPI.encoder().encodeForHTML(resources.getString("errInvalidPass"));
    String errNoMatch = ESAPI.encoder().encodeForHTML(resources.getString("errNoMatch"));
    String errEmptyPass = ESAPI.encoder().encodeForHTML(resources.getString("errEmptyPass"));
    String emptyField = ESAPI.encoder().encodeForHTML("");
    String errTandC = ESAPI.encoder().encodeForHTML(resources.getString("errTandC"));
    String passwordSetMsg = resources.getString("passwordSetMsg");
    String newPassLabel = resources.getString("newPassLabel");
    String token1 = ESAPI.encoder().encodeForHTML(PARAM_TOKEN1);
    String token2 = ESAPI.encoder().encodeForHTML(PARAM_TOKEN2);
    String confirmPassLabel = resources.getString("confirmPassLabel");
    String terms = ESAPI.encoder().encodeForHTML("terms");
    String termsAndCondsLabel = resources.getString("termsAndCondsLabel");
    String settingForm = ESAPI.encoder().encodeForHTML("settingForm");
    String button1 = ESAPI.encoder().encodeForHTML("button1");
    String submitValue = ESAPI.encoder().encodeForHTML(resources.getString("submit"));
    String cancelValue = ESAPI.encoder().encodeForHTML(resources.getString("cancel"));
    String accept = ESAPI.encoder().encodeForHTML("accept");
    String outputField = ESAPI.encoder().encodeForHTML("output");
    String passwordRules = resources.getString("passwordRules");
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
    <title>Password change</title>
    <script type="text/javascript" language="JavaScript">

        function validatePassword(form, token1, token2) {
            // The following are just example rules for a password
            // For the implementation you can modify them to suit your needs
            var out = document.getElementById("msgcnt");

            out.innerHTML = "";
            if (form.elements[token1].value != '') {
                if (form.elements[token1].value == form.elements[token2].value) {
                    if (form.elements[token1].value.length < 8) {
                        out.innerHTML = "<%= errLength %>";
                        form.elements[token1].focus();
                        return false;
                    }

                    var re = /[0-9]/;    // Include numbers
                    if (!re.test(form.elements[token1].value)) {
                        out.innerHTML = "<%= errNumbers %>";
                        form.elements[token1].focus();
                        return false;
                    }
                    re = /[a-z]/;       // Include lowercase
                    if (!re.test(form.elements[token1].value)) {
                        out.innerHTML =
                                "<%= errLowercase %>";
                        form.elements[token1].focus();
                        return false;
                    }
                    re = /[A-Z]/;     // Include Uppercase
                    if (!re.test(form.elements[token1].value)) {
                        out.innerHTML = "<%= errUppercase %>";
                        form.elements[token1].focus();
                        return false;
                    }
                    re = /^[a-zA-Z0-9.\\-\\/+=_ ]*$/   // Any of these
                    if (!re.test(form.elements[token1].value)) {
                        out.innerHTML = "<%= errInvalidPass %>";
                        form.elements[token1].focus();
                        return false;
                    }
                } else {
                    out.innerHTML = "<%= errNoMatch %>";
                    form.elements[token1].focus();
                    return false;
                }

                out.innerHTML = "<%= emptyField %>";
                return true;
            } else {
                form.elements[token1].focus();
                out.innerHTML = "<%= errEmptyPass %>";
                return false;
            }

        }

        function validateTerms(form, terms) {
            var out = document.getElementById("msgcnt");
            out.innerHTML = '';
            if (form.elements[terms].checked == true) {
                return true;
            }

            form.elements[terms].focus();
            out.innerHTML = "<%= errTandC %>";
            return false;

        }

        function validateButton(form, Login) {

            if (form.elements[Login].value == '<%= cancelValue %>') {
                return false;
            } else {
                return true;
            }
        }

        function adios() {
            window.location = "<%= logoutURL %>";
        }

        function validateNow(form, token1, token2, terms, login) {
            if (validatePassword(form, token1, token2)) {
                if (validateTerms(form, terms)) {
                    if (validateButton(form, login)) {
                        return true;
                    }
                }
            }
            return false;
        }

        function newPopup(url) {
            popupWindow = window.open(
                    url, 'popUpWindow', 'height=500,width=600,left=10,top=10,\n\
resizable=yes,scrollbars=yes,toolbar=yes,menubar=no,location=no,directories=no,status=yes')
        }

        function toggleWdw(att) {
            var wdw = document.getElementById(att);
            if (wdw.style.display == "none") {
                wdw.style.display = "block";
            } else {
                wdw.style.display = "none";
            }
        }

        function closeWindow(att) {
            var wdw = document.getElementById(att);
            wdw.style.display = "none";
        }
    </script>
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
                <h1>Password change</h1>

                <p class="message"><a href="javascript:toggleWdw('rules');"><span class="icon info"></a></span><span
                        id="msgcnt"><%= passwordSetMsg %></span>
                    <span id="rules"
                          style="display:none; text-align: left; font-weight:400;"><%= passwordRules %></span></p>

                <form name="<%= settingForm %>" method="POST" action="<%= emptyField %>"
                      onsubmit="return validateNow(this, '<%= token1 %>',
                              '<%= token2 %>','<%= terms %>','<%= button1 %>')">
                    <fieldset>
                        <div class="row"><label for="<%= token1 %>"><%= newPassLabel %>
                        </label><input class="textbox" type="password" id="<%= token1 %>" name="<%= token1 %>"/></div>
                        <div class="row"><label for="<%= token2 %>"><%= confirmPassLabel %>
                        </label><input class="textbox" type="password" id="<%= token2 %>" name="<%= token2 %>"/></div>
                        <div class="row">
                            <label>Terms</label>

                            <div class="checkbox">
                                <input type="checkbox" id="<%= terms %>" value="<%= accept%>"
                                       name="<%= terms %>"/><label for="<%= terms %>">I accept <a
                                    href="JavaScript:newPopup('<%= termsAndConditionsPage %>');"><%= termsAndCondsLabel %>
                            </a></label>
                            </div>
                        </div>
                        <div class="row">
                            <input type="submit" class="button primary" value="<%= submitValue %>"
                                   name="<%= button1 %>"/>
                            <input type="button" class="button" value="<%= cancelValue %>" name="<%= button1 %>"
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
