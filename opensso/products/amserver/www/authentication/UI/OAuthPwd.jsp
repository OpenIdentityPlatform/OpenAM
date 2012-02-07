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
   String termsAndCondsLabel =resources.getString("termsAndCondsLabel");
   String settingForm = ESAPI.encoder().encodeForHTML("settingForm");
   String button1 = ESAPI.encoder().encodeForHTML("button1");
   String submitValue = ESAPI.encoder().encodeForHTML(resources.getString("submit"));
   String cancelValue = ESAPI.encoder().encodeForHTML(resources.getString("cancel"));
   String accept = ESAPI.encoder().encodeForHTML("accept");
   String outputField = ESAPI.encoder().encodeForHTML("output");
   String passwordRules = resources.getString("passwordRules");
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

        <title>Password change</title>

    </head>

    <body>

        <script type="text/javascript" language="JavaScript">
            
            function validatePassword(form, token1, token2)
            {
                // The following are just example rules for a password
                // For the implementation you can modify them to suit your needs
                form.output.value = "";
                if(form.elements[token1].value != '') {
                    if (form.elements[token1].value == form.elements[token2].value) {
                        if(form.elements[token1].value.length < 8) {
                            form.output.value = "<%= errLength %>";
                            form.elements[token1].focus();
                            return false;
                        }
                                                        
                        var re = /[0-9]/;    // Include numbers
                        if(!re.test(form.elements[token1].value)) {
                            form.output.value = "<%= errNumbers %>";
                            form.elements[token1].focus();
                            return false;
                        }
                        re = /[a-z]/;       // Include lowercase
                        if(!re.test(form.elements[token1].value)) {
                            form.output.value = 
                                "<%= errLowercase %>";
                            form.elements[token1].focus();
                            return false;
                        }
                        re = /[A-Z]/;     // Include Uppercase
                        if(!re.test(form.elements[token1].value)) {
                            form.output.value =  "<%= errUppercase %>";
                            form.elements[token1].focus();
                            return false;
                        }
                        re = /^[a-zA-Z0-9.\\-\\/+=_ ]*$/   // Any of these
                        if(!re.test(form.elements[token1].value)) {
                            form.output.value = "<%= errInvalidPass %>";
                            form.elements[token1].focus();
                            return false;
                        }
                    } else {
                        form.output.value = "<%= errNoMatch %>";
                        form.elements[token1].focus();
                        return false;
                    }
                                                    
                    form.output.value = "<%= emptyField %>";
                    return true;
                } else {
                    form.elements[token1].focus();
                    form.output.value = "<%= errEmptyPass %>";
                    return false;
                }
                                                
           }               
             
            function validateTerms(form, terms)
            {
                form.output.value = '';
                if(form.elements[terms].checked == true) {
                    return true;
                } 

                form.elements[terms].focus();
                form.output.value = "<%= errTandC %>";
                return false;

            }
             
            function validateButton(form, Login)
            {   
                
                if(form.elements[Login].value == '<%= cancelValue %>') {
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
                       if (validateButton(form, login)){
                         return true;
                       }
                   }
                }
                return false;
            }
            
        </script>
        <script type="text/javascript">
            
            function newPopup(url) {
                popupWindow = window.open(
                url,'popUpWindow','height=500,width=600,left=10,top=10,\n\
resizable=yes,scrollbars=yes,toolbar=yes,menubar=no,location=no,directories=no,status=yes')
            }
            
        </script> 
        <script type="text/javascript" language="JavaScript">

              function toggleWdw(att)
              {   var wdw=document.getElementById(att);
                  if (wdw.style.display == "none") {
                      wdw.style.display="block";
                  } else {
                      wdw.style.display="none";
                  }
              }

            function closeWindow(att){
                var wdw=document.getElementById(att);
                wdw.style.display="none";
            }
    </script>

        <div style="height: 50px; width: 100%;">

        </div>
        <center>
            <div style="background-image:url('<%= ServiceURI%>/images/login-backimage.jpg'); 
                 background-repeat:no-repeat;  height: 435px; width: 728px; 
                 vertical-align: middle; text-align: center;">
                <table>

                    <form name="<%= settingForm %>" method="POST" action="<%= emptyField %>" 
                          onSubmit="return validateNow(this, '<%= token1 %>', 
                              '<%= token2 %>','<%= terms %>','<%= button1 %>')">
                        <tr height="100px"><td width="295px"></td><td></td></tr>
                        <tr><td width="295px"></td>
                            <td align="left"><img src="<%= ServiceURI %>/images/PrimaryProductName.png" /></td>
                        </tr>    
                        <tr><td width="295px"></td>
                            <td>
                                <p><%= passwordSetMsg %>
                                    <a href="javascript:toggleWdw('rules');">
                                       <img src="<%= ServiceURI%>/images/info_large.gif" 
                                         alt="information" height="12" width="12" border="none">
                                    </a></p>
                                    <div id="rules" style="display:none; width: 80%;">
                                                <div style="background-color: #EAEAEA; text-align: left">
                                                    <%= passwordRules %>
                                                </div>
                                   </div>
                                <table align="center" border="0" cellpadding="2" cellspacing="2" >

                                    <tr><td>
                                            <label for="<%= token1 %>" ><%= newPassLabel %></label>
                                        </td>
                                        <td>
                                            <input type="password" size="15" name="<%= token1 %>">
                                        </td>
                                    </tr>
                                    <tr><td>
                                            <label for="<%= token2 %>"><%= confirmPassLabel %></label>
                                        </td>
                                        <td>
                                            <input type="password" size="15" name="<%= token2 %>">
                                        </td>
                                    </tr>
                                    <tr><td colspan="2">
                                            <p>
                                                <input type="checkbox" name="<%= terms %>" 
                                                       value="<%= accept%>">
                                                I accept <a href="JavaScript:newPopup('<%= termsAndConditionsPage %>');">
                                                    <%= termsAndCondsLabel %></a> <br>
                                            </p>
                                            <br />
                                            <input type="submit" name="<%= button1 %>" 
                                                   value="<%= submitValue %>" >
                                            <input type="submit" name="<%= button1 %>"
                                                   value="<%= cancelValue %>" onClick="adios()">
                                        </td>
                                    </tr>
                                </table>

                            </td>
                        </tr>
                        <tr><td width="295px"></td><td align="center">
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
