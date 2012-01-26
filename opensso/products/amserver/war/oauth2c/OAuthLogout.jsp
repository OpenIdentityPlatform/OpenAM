<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright © 2011 ForgeRock AS. All rights reserved.
  
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

<%@ page language="java"%>
<%@ page import="org.owasp.esapi.*" %>
<%@ page import="com.iplanet.am.util.SystemProperties" %>
<%@ page import="com.sun.identity.shared.Constants" %>
<%@ page import="static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.*" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.MissingResourceException" %>
<%@ page import="org.forgerock.openam.authentication.modules.oauth2.OAuthUtil" %>


<%
   // Internationalization stuff. You can use any internationalization framework
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
    
  String loggedout = request.getParameter(PARAM_LOGGEDOUT);
  System.out.println("loggedout=" + loggedout);
  
%>

<html>


    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
        <link rel="stylesheet" href="<%= ServiceURI %>/css/styles.css" type="text/css" />
        <script language="JavaScript" src="<%= ServiceURI %>/js/browserVersion.js"></script>
        <script language="JavaScript" src="<%= ServiceURI %>/js/auth.js"></script>
        <script language="JavaScript">
            writeCSS('<%= ServiceURI %>');
        </script>
        <script type="text/javascript"><!--// Empty script so IE5.0 Windows will draw table and button borders
            //-->
        </script>
        <script>
                function adios() { 
                    window.location = "<%= gotoURL %>";
                }
                
                function logoutAll() {
                    // Creates an iFrame to log out from the OAuth 2.0 IdP
                    var frame = document.getElementById('frame');
                    if (!frame){return};
                    var logMsg = document.getElementById('logoutMsg');
                    var main = document.getElementById('main');
                    logMsg.style.display = '';
                    main.style.display = 'none';
                    var iframe = document.createElement('iframe');
                    iframe.setAttribute('src', '<%= logoutURL %>');
                    iframe.setAttribute('width', 0);
                    iframe.setAttribute('height', 0);                  
                    iframe.setAttribute('frameborder', 0);
                    frame.innerHTML = '';
                    iframe.onload = adios();
                    frame.appendChild(iframe);
                }
      </script>
      <script>
                <% if (loggedout != null && loggedout.equalsIgnoreCase("logmeout")){
                   out.println("window.onload = function() {");
                   out.println("logoutAll(); }");
                }
                %>
                    
     </script>
       <title>Logout</title>
    </head>

    <body>


        <div style="height: 50px; width: 100%;">

        </div>
        <center>
            <div style="background-image:url('<%= ServiceURI%>/images/login-backimage.jpg'); background-repeat:no-repeat; 
                 height: 435px; width: 728px; vertical-align: middle; text-align: center;">

                <table>
                    <tr height="100px"><td width="295px"></td>
                        <td></td></tr>
                    <tr><td width="295px"></td>
                        <td align="left"><img src="<%= ServiceURI %>/images/PrimaryProductName.png" /></td></tr>    
                    <tr><td width="295px"></td>
                        <td>
                            <div id="logoutMsg" style="display:none">
                                <center>
                                <p><%= loggingYouOut %></p>
                                </center>
                            </div>
                            <div id="frame">
                                <noscript>
                                   Your browser does not support scripts.
                                   This page needs javascript to be enabled in your browser.                  
                                </noscript>
                            </div>
                            <div id="main">
                            <form name="<%= logoutForm %>" method="POST" action="">
                                <p><%= doYouWantToLogout %></p>       
                                <button type="button" name="<%= loggedoutParam %>"
                                      value="<%= logmeoutValue %>" onClick="logoutAll()"
                                      onmousedown="logoutAll()">
                                      <%= logmeoutValue%>
                                </button>
                                <button type="button" name="<%= loggedoutParam %>"
                                      value="<%= donotValue %>" onClick="adios()" 
                                      onmousedown="adios()" >
                                      <%= donotValue %>
                                </button> 
                            </form>  
                            </div>
                        </td>
                    </tr>

                </table>
            </div>
        </center>

    </body>
</html>      