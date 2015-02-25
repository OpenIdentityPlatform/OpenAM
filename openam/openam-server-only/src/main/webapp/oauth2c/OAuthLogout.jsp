<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

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
<%--
   Portions Copyrighted 2012 Open Source Solution Technology Corporation
--%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<%@ page language="java" pageEncoding="UTF-8" %>
<%@ page import="org.owasp.esapi.*" %>
<%@ page import="com.iplanet.am.util.SystemProperties" %>
<%@ page import="com.sun.identity.shared.Constants" %>
<%@ page import="static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.*" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.MissingResourceException" %>
<%@ page import="org.forgerock.openam.authentication.modules.oauth2.OAuthUtil" %>
<%
   // Internationalization stuff. You can use any internationalization framework
   String lang = request.getParameter("lang");
   if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + lang, lang,
        "HTTPParameterValue", 2000, true)){
            lang = null;
   }
   ResourceBundle resources;
   Locale locale = null;
   try {
        if (lang != null && lang.length() != 0) {
           locale = new Locale(lang);
        } else {
           locale = request.getLocale();
        }
        resources = ResourceBundle.getBundle("amAuthOAuth", locale);
        OAuthUtil.debugMessage("OAuthLogout: obtained resource bundle with locale " + locale);
   } catch (MissingResourceException mr) {
        OAuthUtil.debugError("OAuthLogout: Resource Bundle not found", mr);
        resources = ResourceBundle.getBundle("amAuthOAuth");
   }
   
   String logoutForm = ESAPI.encoder().encodeForHTML(LOGOUT_FORM);
   String loggedoutParam = ESAPI.encoder().encodeForHTML(PARAM_LOGGEDOUT);
   String gotoParam = ESAPI.encoder().encodeForHTML(PARAM_GOTO);
   String logoutURLParam = ESAPI.encoder().encodeForHTML(PARAM_LOGOUT_URL);
   
   String logmeoutValue = ESAPI.encoder().encodeForHTML(resources.getString("logmeout"));
   String donotValue = ESAPI.encoder().encodeForHTML(resources.getString("donot")); 
   String doYouWantToLogout = resources.getString("doYouWantToLogout");
   String youVeBeenLogedOut = resources.getString("youVeBeenLogedOut");
   String loggingYouOut = resources.getString("loggingYouOut");
   
   // Getting and validating params
   String gotoURL = request.getParameter(PARAM_GOTO);
   String gotoURLencAttr = "";
   String OAuth2IdP = "";
   
   String ServiceURI = SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR); 
   if (gotoURL == null || gotoURL.isEmpty() ) {
      gotoURL = ServiceURI + "/UI/Logout"; 
   } else {
       boolean isValidURL = ESAPI.validator().
               isValidInput("URLContext", gotoURL, "URL", 255, false); 
       boolean isValidURI = ESAPI.validator().
               isValidInput("HTTP URI: " + gotoURL, gotoURL, "HTTPURI", 2000, false);      
       if (!isValidURL && !isValidURI) {
           OAuthUtil.debugError("OAuthLogout: wrong goto URL attempted to be used "
                   + "in the Logout page: " + gotoURL);
           gotoURL = "wronggotoURL";
       } 
   }
   
   String logoutURL = request.getParameter(PARAM_LOGOUT_URL);
   if (logoutURL == null) {
      logoutURL = "";
   } else {
       boolean isValidURL = ESAPI.validator().
               isValidInput("URLContext", logoutURL, "URL", 255, false); 
       if (!isValidURL) {   
           OAuthUtil.debugError("OAuthLogout: wrong logoutURL URL attempted to be used "
                   + "in the Logout page: " + logoutURL);
           logoutURL = "wronglogoutURL";
       } else {
           int loc1 = logoutURL.indexOf("//") + 2;
           OAuth2IdP = logoutURL.substring(loc1, logoutURL.indexOf("/", loc1));
           doYouWantToLogout = doYouWantToLogout.replace("#IDP#", OAuth2IdP);
       }
   }
   String copyrightNotice = null;
   try{
       copyrightNotice = ResourceBundle.getBundle("amAuthUI", locale).getString("copyright.notice");
   } catch (MissingResourceException mr) {
   }
    
  String loggedout = request.getParameter(PARAM_LOGGEDOUT);
  System.out.println("loggedout=" + loggedout);
  
%>
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link href="<%= ServiceURI%>/css/new_style.css" rel="stylesheet" type="text/css" />
        <!--[if IE 9]> <link href="<%= ServiceURI %>/css/ie9.css" rel="stylesheet" type="text/css"> <![endif]-->
        <!--[if lte IE 7]> <link href="<%= ServiceURI %>/css/ie7.css" rel="stylesheet" type="text/css"> <![endif]-->
        <script language="JavaScript" type="text/javascript">
            function adios() {
                window.location = "<%= gotoURL %>";
            }
                
            function logoutAll() {
                // Creates an iFrame to log out from the OAuth 2.0 IdP
                var frame = document.getElementById('frame');
                if (!frame){return};
                var logMsg = document.getElementById('logoutMsg');
                var logMsgVs = document.getElementById('logoutMsgVisible');
                var main = document.getElementById('main');
                logMsg.style.display = '';
                main.style.display = 'none';
                logMsgVs.style.display = 'none';
                var iframe = document.createElement('iframe');
                iframe.setAttribute('src', '<%= logoutURL %>');
                iframe.setAttribute('width', 0);
                iframe.setAttribute('height', 0);
                iframe.setAttribute('frameborder', 0);
                frame.innerHTML = '';
                iframe.onload = adios();
                frame.appendChild(iframe);
            }

            <% if (loggedout != null && loggedout.equalsIgnoreCase("logmeout")){
                   out.println("window.onload = function() {");
                   out.println("logoutAll(); }");
                }
            %>
        </script>
        <title>Logout</title>
    </head>
    <body>
        <div class="container_12">
            <div class="grid_4 suffix_8">
                <a class="logo" href="<%= ServiceURI%>"></a>
            </div>
            <div class="box box-spaced clear-float">
                <div class="grid_3">
                    <div class="product-logo"></div>
                </div>
                <div class="grid_9">
                    <div class="box-content clear-float">
                        <div class="message">
                            <span class="icon info"></span>
                            <div id="logoutMsg" style="display:none">
                                <h3><%= loggingYouOut %></h3>
                            </div>
                            <div id="logoutMsgVisible">
                                <h3><%= doYouWantToLogout %></h3>
                            </div>
                        </div>
                        <div id="frame">
                            <noscript>
                                Your browser does not support scripts.
                                This page needs javascript to be enabled in your browser.
                            </noscript>
                        </div>
                        <div id="main">
                            <form name="<%= logoutForm %>" method="POST" action="">
                                <input name="<%= loggedoutParam %>" type="button" class="button" onClick="adios()" onmousedown="adios()" value="<%= donotValue %>" />
                                <input name="<%= loggedoutParam %>" type="button" class="button right" onClick="logoutAll()" onmousedown="adios()" value="<%= logmeoutValue %>" />
                            </form>  
                        </div>
                    </div>
                </div>
            </div>
            <div class="footer alt-color">
                <div class="grid_6 suffix_3">
                    <p>
                        <% if (copyrightNotice != null){
                               out.println(copyrightNotice);
                            }
                        %>
                    </p>
                </div>
            </div>
        </div>
    </body>
</html>
