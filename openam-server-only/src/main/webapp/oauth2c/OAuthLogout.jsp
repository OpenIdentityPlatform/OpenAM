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
   Portions Copyright 2026 3A Systems, LLC.
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
<%@ page import="com.iplanet.sso.SSOTokenManager" %>
<%@ page import="com.iplanet.sso.SSOException" %>
<%@ page import="com.iplanet.sso.SSOToken" %>
<%@ page import="com.sun.identity.authentication.service.AuthD" %>
<%@ page import="com.sun.identity.idm.AMIdentity" %>
<%@ page import="com.sun.identity.authentication.service.AuthUtils" %>
<%@ page import="java.util.HashMap" %>
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
   // The realm is taken from the session, and failing that from the realm the authentication framework put on
   // the request; it is deliberately not read from the request parameters, which would let the caller name a
   // realm whose goto whitelist happens to be permissive.
   String realm = null;
   SSOTokenManager manager = SSOTokenManager.getInstance();
   try {
       SSOToken ssoToken = manager.createSSOToken(request);
       realm = ssoToken.getProperty("Organization");
       if (realm == null || realm.length() == 0) {
           // Resolved here rather than only below, so that the two URLs this page judges are measured against
           // the same realm: a session carrying no Organization would otherwise have the goto URL fall to the
           // origin check while the logout URL was still read against the realm the request resolves to.
           realm = AuthUtils.getDomainNameByRequest(request, new HashMap<String, String>());
       }
       // gotoURL is assigned to window.location, so the realm has to accept it as a goto URL. The ESAPI
       // "HTTPURI" type this used to rely on accepts //attacker.example.com, and isGotoUrlValid(url, realm)
       // accepts everything while the realm has no goto whitelist, so neither of them closed the redirect.
       if (!AuthD.getAuth().isGotoUrlValid(request, gotoURL, realm)) {
           // The URL stays out of the message: it has been refused, and a value carrying CRLF would
           // forge lines in the plain text debug file.
           OAuthUtil.debugError("OAuthLogout: the goto URL is not a permitted redirect target, "
                   + "the default logout URL is used instead");
           gotoURL = ServiceURI + "/UI/Logout";
       }
   } catch (SSOException e) {
       OAuthUtil.debugMessage("OAuthLogout: using default goto URL because we failed to get a session");
       gotoURL = ServiceURI + "/UI/Logout";
   }
   if (realm == null || realm.length() == 0) {
       realm = AuthUtils.getDomainNameByRequest(request, new HashMap<String, String>());
   }
   gotoURL = ESAPI.encoder().encodeForJavaScript(gotoURL);
   
   String logoutURL = request.getParameter(PARAM_LOGOUT_URL);
   if (logoutURL == null) {
      logoutURL = "";
   } else {
       // logoutURL is loaded in an iframe, so the realm has to accept it as a goto URL.
       if (!AuthD.getAuth().isGotoUrlValid(request, logoutURL, realm)) {
           // The URL stays out of the message, for the same reason as the goto URL above.
           OAuthUtil.debugError("OAuthLogout: the logout URL is not a permitted redirect target, "
                   + "it is not loaded");
           logoutURL = "wronglogoutURL";
       } else {
           // A logout URL naming no authority is resolved against this server, and the base ESAPI "URL"
           // check - which rejected everything - meant this branch never had to name a host at all.
           String logoutHost = OAuthUtil.getUrlHost(logoutURL);
           OAuth2IdP = logoutHost != null ? logoutHost : request.getServerName();
           doYouWantToLogout = doYouWantToLogout.replace("#IDP#",
                   ESAPI.encoder().encodeForHTML(OAuth2IdP));
       }
   }
   logoutURL = ESAPI.encoder().encodeForJavaScript(logoutURL);
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
