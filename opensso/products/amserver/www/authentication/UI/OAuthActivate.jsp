<%--
   DO  NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
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

<%@ page  language="java"%>
<%@ page import="org.owasp.esapi.ESAPI" %>
<%@ page import="com.iplanet.am.util.SystemProperties" %>
<%@ page import="com.sun.identity.shared.Constants" %>
<%@ page import="static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.*" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="java.util.MissingResourceException" %>
<%@ page import="java.util.Locale" %>
<%@ page import="org.forgerock.openam.authentication.modules.oauth2.OAuthUtil" %>

<%
   // Internationalization stuff. Tis is just an example. You can use any i18n framework
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
        
        <script language="JavaScript">
            
        function validateEntry(form, activation, submit) {
            form.output.value = "";
            if(form.elements[submit].value == '<%= cancelValue %>') {
                    return false;
            }
            if(form.elements[activation].value == '') {
                form.output.value = "<%= emptyCode %>";
                form.elements[activation].focus();
                return false;
            }
            var re = /^[a-zA-Z0-9.\\-\\/+=_ ]*$/
            if(!re.test(form.elements[activation].value)) {
                form.output.value = "<%= errInvalidCode %>";
                form.elements[token1].focus();
                return false;
           }
           form.output.value = "<%= ESAPI.encoder().encodeForHTML("") %>";
           return true;
        }
        
        function adios() { 
             window.location = "<%= logoutURL %>";
        }
        
        </script>
        
        <title><%= activationTitle %></title>

    </head>

    <body>
            <div style="height: 50px; width: 100%;">
                
        </div>
        <center>
            <div style="background-image:url('<%= ServiceURI%>/images/login-backimage.jpg'); 
                 background-repeat:no-repeat; height: 435px; width: 728px; 
                 vertical-align: middle; text-align: center;" >
                <table>
                    <form name="Login" method="POST" action="<%= emptyField %>" 
                          onSubmit="return validateEntry(this,'<%= activation %>' , 
                              '<%= submitButton %>');" >

                        <tr height="100px"><td width="295px"></td><td></td></tr>
                        <tr><td width="295px"></td>                          
                            <td align="left">
                                <img src="<%= ServiceURI %>/images/PrimaryProductName.png" />
                            </td>
                        </tr>    
                        <tr><td width="295px"></td>
                            <td>
                                <p><%= activationCodeMsg %>
                                </p>
                                <table align="center" border="0" cellpadding="2" cellspacing="2" >
                                    
                                    <tr><td>
                                            <label for="<%= activation %>" >
                                                <%=activationLabel%>
                                            </label>
                                        </td>
                                        <td>
                                            <input type="text" size="30" 
                                                   name="<%= activation %>">
                                        </td>
                                    </tr>
                                    <tr><td colspan="2">
                                            <input type="submit" name="<%= submitButton %>" 
                                                   value="<%= submitValue %>" >
                                            <input type="submit" name="<%= submitButton %>"
                                                   value="<%= cancelValue %>" onClick="adios()">
                                        </td>                                
                                    </tr>                                 

                                </table>
                            </td>
                        </tr>
                        <tr><td width="295px"></td>
                            <td align="center">
                                <input type="text" name="<%= outputField %>" 
                                       style="border: 0; font-family: verdana; color: blue; text-align: center;" 
                                       value="<%= emptyField %>" size="60" readonly>
                            </td>
                        </tr>
                    </form>   
                </table>
            </div>
        </center>
    </body>
</html>
