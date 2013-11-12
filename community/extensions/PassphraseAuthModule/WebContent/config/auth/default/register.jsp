<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
  
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
                                                                                
   $Id: register.jsp,v 1.1.1.1 2008/10/29 08:05:21 svbld Exp $

   "Portions Copyrighted [2012] [Forgerock AS]"                                                                                
--%>




<html>

<%@page info="Self Registration Module" language="java"%>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>
<%@taglib uri="/WEB-INF/auth.tld" prefix="auth"%>
<%@include file="init.jsp"%>
<jato:useViewBean className="com.sun.identity.authentication.UI.LoginViewBean">


<%@ page contentType="text/html" %>

<head>
<title><jato:text name="htmlTitle_SelfRegModule" /></title>

<% 
String ServiceURI = (String) viewBean.getDisplayFieldValue(viewBean.SERVICE_URI);
%>

<link rel="stylesheet" href="<%= serviceURL %>/css/styles.css" type="text/css" />
<script language="JavaScript" src="<%= ServiceURI %>/js/browserVersion.js"></script>
<script language="JavaScript" src="<%= ServiceURI %>/js/auth.js"></script>

<script language="javascript">

    writeCSS('<%= ServiceURI %>');

<jato:content name="validContent">
    var defaultBtn = 'Submit';
    var elmCount = 0;

    function defaultSubmit() {
        LoginSubmit(defaultBtn);
    }

    function LoginSubmit(value) {
        aggSubmit();
        var hiddenFrm = document.forms['Login'];

        if (hiddenFrm != null) {
            hiddenFrm.elements['IDButton'].value = value;
            hiddenFrm.submit();
        }
    }

    function resetForms() {
        var frms = document.forms;
        for (var i = 0; i < elmCount; i++) {
            var frm = frms['frm' + i];

            if (frm != null) {
                clearFormElms(frm);
            }
        }
        placeCursorOnFirstElm();
    }
</jato:content>
</script>
<script type="text/javascript"><!--// Empty script so IE5.0 Windows will draw table and button borders
//-->
</script>
</head>

<body class="LogBdy" onload="placeCursorOnFirstElm();">

  <table border="0" cellpadding="0" cellspacing="0" align="center" title="">
    <tr>
      <td width="50%"><img src="<%= serviceURL %>/images/dot.gif" width="1" height="1" alt="" /></td>
      <td><img src="<%= serviceURL %>/images/dot.gif" width="728" height="1" alt="" /></td>
      <td width="50%"><img src="<%= serviceURL %>/images/dot.gif" width="1" height="1" alt="" /></td>
    </tr>
    <tr class="LogTopBnd" style="background-image: url(<%= serviceURL %>/images/gradlogtop.jpg); 
    background-repeat: repeat-x; background-position: left top;">
      <td>&nbsp;</td>
      <td><img src="<%= serviceURL %>/images/dot.gif" width="1" height="30" alt="" /></td>
      <td>&nbsp;</td>
    </tr>
    <tr>
      <td class="LogMidBnd" style="background-image: url(<%= serviceURL %>/images/gradlogsides.jpg);
        background-repeat:repeat-x;background-position:left top;">&nbsp;</td>
      <td class="LogCntTd" style="background-image: url(<%= serviceURL %>/images/login-backimage.jpg);
        background-repeat:no-repeat;background-position:left top;" height="435" align="center" valign="middle">
        <table border="0" background="<%= serviceURL %>/images/dot.gif" cellpadding="0" cellspacing="0" 
        width="100%" title="">
          <tr>
            <td width="260"><img src="<%= serviceURL %>/images/dot.gif" width="260" height="245" alt="" /></td>
            <td width="415" bgcolor="#ffffff" valign="top"><img name="Login.productLogo" 
            src="<%= serviceURL %>/images/PrimaryProductName.png" alt="<auth:resBundle bundleName="amAuthUI" resourceKey="basic_realm" />" 
            border="0" />
              <table border="0" cellspacing="0" cellpadding="0">
                <tr>
                  <td colspan="2">
                      <img src="<%= serviceURL %>/images/dot.gif" width="1" height="25" alt="" />                                
                  </td>
                </tr>

        <!-- display authentication scheme -->

        <!-- Header display -->
        <tr>
        <td nowrap="nowrap"></td>
        <td><div class="logTxtSvrNam">                    
        <jato:content name="ContentStaticTextHeader">
            <jato:getDisplayFieldValue name='StaticTextHeader'
                defaultValue='Authentication' fireDisplayEvents='true'
                escape='false'/>
        </jato:content>        
        </div></td>
        </tr>
        <!-- End of Header display -->      
  
        <jato:content name="validContent">

        <jato:tiledView name="tiledCallbacks"
            type="com.sun.identity.authentication.UI.CallBackTiledView">

        <script language="javascript">
            elmCount++;
        </script>

        <jato:content name="textBox">
        <!-- text box display -->
        <tr>
        <form name="frm<jato:text name="txtIndex" />" action="blank"
            onSubmit="defaultSubmit(); return false;" method="post">

        <td nowrap="nowrap"><div class="logLbl">
            <jato:content name="isRequired">
            <img src="<%= serviceURL %>/images/required.gif" alt="Required Field" 
            title="Required Field" width="7" height="14" />
            </jato:content>
            <span class="LblLev2Txt">
            <label for="IDToken<jato:text name="txtIndex" />">                
                <jato:text name="txtPrompt" defaultValue="User name:" 
                                                        escape="false" />                            
            </label></span></div>
        </td>
        
        <td><div class="logInp">
            <input type="text" name="IDToken<jato:text name="txtIndex" />"
                id="IDToken<jato:text name="txtIndex" />"
                value="" class="TxtFld"></div>
        </td>
        </form>
        </tr>        
        <!-- end of textBox -->
        </jato:content>

        <jato:content name="password">
        <!-- password display -->
        <tr>
        <form name="frm<jato:text name="txtIndex" />" action="blank"
            onSubmit="defaultSubmit(); return false;" method="post">

        <td nowrap="nowrap"><div class="logLbl">
            <jato:content name="isRequired">
            <img src="<%= serviceURL %>/images/required.gif" alt="Required Field" 
            title="Required Field" width="7" height="14" />
            </jato:content>
            <span class="LblLev2Txt">
            <label for="IDToken<jato:text name="txtIndex" />">  
                <jato:text name="txtPrompt" defaultValue="Password:" 
                                                        escape="false" />                
            </label></span></div>
        </td>

        <td><div class="logInp">
            <input type="password" name="IDToken<jato:text name="txtIndex" />"
                id="IDToken<jato:text name="txtIndex" />"
                value="" class="TxtFld"></div>
        </td>
        </form>
        </tr>        
        <!-- end of password -->
        </jato:content>

        <jato:content name="choice">
        <!-- choice value display -->
        <tr>
        <form name="frm<jato:text name="txtIndex" />" action="blank"
            onSubmit="defaultSubmit(); return false;" method="post">

        <td nowrap="nowrap"><div class="logLbl">
            <jato:content name="isRequired">
            <img src="<%= serviceURL %>/images/required.gif" alt="Required Field" 
            title="Required Field" width="7" height="14" />
            </jato:content>
            <span class="LblLev2Txt">
            <label for="IDToken<jato:text name="txtIndex" />">  
                <jato:text name="txtPrompt" defaultValue="RadioButton:" 
                                                            escape="false" />                
            </label></span></div>
        </td>

        <td><div class="logInp">
            <jato:tiledView name="tiledChoices"
                type="com.sun.identity.authentication.UI.CallBackChoiceTiledView">

            <jato:content name="selectedChoice">
                <input type="radio"
                    name="IDToken<jato:text name="txtParentIndex" />"
                    id="IDToken<jato:text name="txtParentIndex" />"
                    value="<jato:text name="txtIndex" />" class="Rb"
                    checked><jato:text name="txtChoice" /><br>
            </jato:content>

            <jato:content name="unselectedChoice">
                <input type="radio"
                    name="IDToken<jato:text name="txtParentIndex" />"
                    id="IDToken<jato:text name="txtParentIndex" />"
                    value="<jato:text name="txtIndex" />" class="Rb"
                    ><jato:text name="txtChoice" /><br>
            </jato:content>

            </jato:tiledView></div>
        </td>
        </form>
        </tr>
        <tr></tr>
        <!-- end of choice -->
        </jato:content>

        <!-- end of tiledCallbacks -->
        </jato:tiledView>

        <!-- end of validContent -->
        </jato:content>


        <jato:content name="ContentStaticTextResult">
        <!-- after login output message -->
        <p><b><jato:getDisplayFieldValue name='StaticTextResult'
            defaultValue='' fireDisplayEvents='true' escape='false'/></b></p>
        </jato:content>

        <jato:content name="ContentHref">
        <!-- URL back to Login page -->
            <p><auth:href name="LoginURL"
                    fireDisplayEvents='true'>
                <jato:text
                name="txtGotoLoginAfterFail" /></auth:href></p>
        </jato:content>

        <jato:content name="ContentImage">
        <!-- customized image defined in properties file -->
            <p><img name="IDImage"
                src="<jato:getDisplayFieldValue name='Image'/>" alt=""></p>
        </jato:content>

        <center>
        <img src="<%= serviceURL %>/images/required.gif" alt="Required Field" 
                    title="Required Field" width="7" height="14" />&nbsp;
        <auth:resBundle bundleName="amAuthUI" resourceKey="reqfield.desc" />
        </center>

        <jato:content name="ContentButtonLogin">
        <!-- Submit button -->

        <jato:content name="hasButton">
        <script language="javascript">
            defaultBtn = '<jato:text name="defaultBtn" />';
        </script>
        <tr>
        <td><img src="<%= serviceURL %>/images/dot.gif" 
        width="1" height="15" alt="" /></td>
        <td>
            <table border=0 cellpadding=0 cellspacing=0>
            <tr>
            <jato:tiledView name="tiledButtons"
                type="com.sun.identity.authentication.UI.ButtonTiledView">            
                <script language="javascript">
                    markupButton(
                        '<jato:text name="txtButton" />',
                        "javascript:LoginSubmit('<jato:text name="txtButton" />')");
                </script>            
            </jato:tiledView>
            <script language="javascript">
                    markupButton(
                        "<jato:text name="lblReset" />",
                        'javascript:resetForms()');
            </script>
            </tr>
            </table>
        </td>
        </tr>
        <!-- end of hasButton -->
        </jato:content>

        <jato:content name="hasNoButton">
        <tr>
        <td><img src="<%= serviceURL %>/images/dot.gif" 
        width="1" height="15" alt="" /></td>
        <td>
            <table border=0 cellpadding=0 cellspacing=0>
            <tr>
            <script language="javascript">
                    markupButton(
                        "<jato:text name="lblSubmit" />",
                        "javascript:LoginSubmit('<jato:text name="cmdSubmit" />')");
            </script>            
            <script language="javascript">
                    markupButton(
                        "<jato:text name="lblReset" />",
                        'javascript:resetForms()');
            </script>
            </tr>
            </table>
        </td>
        </tr>
        <!-- end of hasNoButton -->
        </jato:content>

        <!-- end of ContentButtonLogin -->
        </jato:content>
        <tr>
            <td>&nbsp;</td>
        </tr>
        <tr>
            <td><img src="<%= serviceURL %>/images/dot.gif" 
            width="1" height="33" alt="" /></td>
            <td>&nbsp;</td>
        </tr>
        </table>
      </td>
      <td width="45"><img src="<%= serviceURL %>/images/dot.gif" 
      width="45" height="245" alt="" /></td>
    </tr>
    </table>
    </td>
    <td class="LogMidBnd" style="background-image: url(<%= serviceURL %>/images/gradlogsides.jpg);
    background-repeat:repeat-x;background-position:left top;">&nbsp;</td>
    </tr>
    <tr class="LogBotBnd" style="background-image: url(<%= serviceURL %>/images/gradlogbot.jpg);
    background-repeat:repeat-x;background-position:left top;">
      <td>&nbsp;</td>
      <td><div class="logCpy"><span class="logTxtCpy">
        <auth:resBundle bundleName="amAuthUI" resourceKey="copyright.notice" /></span></div>
      </td>
      <td>&nbsp;</td>
    </tr>
  </table>

<jato:content name="validContent">
<auth:form name="Login" method="post"
    defaultCommandChild="DefaultLoginURL">


<script language="javascript">
    if (elmCount != null) {
        for (var i = 0; i < elmCount; i++) {
            document.write(
                "<input name=\"IDToken" + i + "\" type=\"hidden\">");
        }
    document.write("<input name=\"IDButton"  + "\" type=\"hidden\">");        
    }
</script>
<input type="hidden" name="page_state" value="<auth:value key='PageState' />">
</auth:form>
</jato:content>

</body>

</jato:useViewBean>

</html>
