<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

   Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved

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

   $Id: swekeyLogin.jsp,v 1.1 2009/03/03 22:32:27 superpat7 Exp $

--%>

<%@page info="Swekey Login Page" language="java"%>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>
<%@taglib uri="/WEB-INF/auth.tld" prefix="auth"%>
<jato:useViewBean className="com.sun.identity.authentication.UI.LoginViewBean">

<%@page contentType="text/html" %>
<%@page import="com.sun.identity.authentication.modules.swekey.SwekeyWebServiceClient"%>

<%
String ServiceURI = (String) viewBean.getDisplayFieldValue(viewBean.SERVICE_URI);
String encoded = "false";
String gotoURL = (String) viewBean.getValidatedInputURL(
    request.getParameter("goto"), request.getParameter("encoded"), request);
String encodedQueryParams = (String) viewBean.getEncodedQueryParams(request);
if ((gotoURL != null) && (gotoURL.length() != 0)) {
    encoded = "true";
}

// In a real implementation, we would use getFastRndToken(), particularly since
// we call this every time we render the login page
String rt = SwekeyWebServiceClient.getRndToken();
%>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title><jato:text name="htmlTitle_Login" /></title>

<link rel="stylesheet" href="<%= ServiceURI %>/css/styles.css" type="text/css" />
<script type="text/javascript" src="<%= ServiceURI %>/js/browserVersion.js"></script>
<script type="text/javascript" src="<%= ServiceURI %>/js/auth.js"></script>
<script type="text/javascript" src="<%= ServiceURI %>/js/swekey.js"></script>
<script type="text/javascript" src="<%= ServiceURI %>/js/swekey_integrate.js"></script>

<script type="text/javascript">
<jato:content name="validContent">
function swekey_add_data_to_form(hiddenFrm, rt)
{
    // Get the ids of the plugged swekeys
    var swekey_ids = Swekey_ListKeyIds();

    // in case of we have multiple swekey plugged we take only
    // the first one (32 = length of a swekey id)
    var swekey_id = swekey_ids.substr(0, 32);

    // Get the OTP from the Swekey
    var otp = Swekey_GetOtp(swekey_id, rt);

    // Stuff our Swekey bits into the hidden form
    hiddenFrm.elements['IDToken2'].value = swekey_id;
    hiddenFrm.elements['IDToken3'].value = rt;
    hiddenFrm.elements['IDToken4'].value = otp;

    // Oops - no Swekey - tell the user to insert it!
    if (swekey_id == null || swekey_id.length==0) {
        alert("Please insert your Swekey and try again");
        return false;
    }

    return true;
}
</jato:content>
</script>

<script type="text/javascript">

    writeCSS('<%= ServiceURI %>');

<jato:content name="validContent">
    var defaultBtn = 'Submit';
    var elmCount = 0;

    /** submit form with default command button */
    function defaultSubmit() {
        LoginSubmit(defaultBtn);
    }

    /**
     * submit form with given button value
     *
     * @param value of button
     */
    function LoginSubmit(value) {
        aggSubmit();

        var hiddenFrm = document.forms['Login'];

        if (hiddenFrm != null) {
            if ( ! swekey_add_data_to_form(hiddenFrm, '<%= rt %>') ) {
                // Swekey missing - don't submit the form'
                return;
            }

    	    hiddenFrm.elements['IDButton'].value = value;
            if (this.submitted) {
                alert("The request is currently being processed");
            }
            else {
                this.submitted = true;
                hiddenFrm.submit();
            }
        }
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
      <td width="50%"><img src="<%= ServiceURI %>/images/dot.gif" width="1" height="1" alt="" /></td>
      <td><img src="<%= ServiceURI %>/images/dot.gif" width="728" height="1" alt="" /></td>
      <td width="50%"><img src="<%= ServiceURI %>/images/dot.gif" width="1" height="1" alt="" /></td>
    </tr>
    <tr class="LogTopBnd" style="background-image: url(<%= ServiceURI %>/images/gradlogtop.jpg);
    background-repeat: repeat-x; background-position: left top;">
      <td>&nbsp;</td>
      <td><img src="<%= ServiceURI %>/images/dot.gif" width="1" height="30" alt="" /></td>
      <td>&nbsp;</td>
    </tr>
    <tr>
      <td class="LogMidBnd" style="background-image: url(<%= ServiceURI %>/images/gradlogsides.jpg);
        background-repeat:repeat-x;background-position:left top;">&nbsp;</td>
      <td class="LogCntTd" style="background-image: url(<%= ServiceURI %>/images/login-backimage.jpg);
        background-repeat:no-repeat;background-position:left top;" height="435" align="center" valign="middle">
        <table border="0" background="<%= ServiceURI %>/images/dot.gif" cellpadding="0" cellspacing="0"
        width="100%" title="">
          <tr>
            <td width="260"><img src="<%= ServiceURI %>/images/dot.gif" width="260" height="245" alt="" /></td>
            <td width="415" bgcolor="#ffffff" valign="top"><img src="<%= ServiceURI %>/images/dot.gif" width="30" height="1" alt="" /><img name="Login.productLogo"
            src="<%= ServiceURI %>/images/PrimaryProductName.png" alt="<auth:resBundle bundleName="amAuthUI" resourceKey="basic_realm" />"
            border="0" />
              <table border="0" cellspacing="0" cellpadding="0">
                <tr>
                  <td colspan="2">
                      <img src="<%= ServiceURI %>/images/dot.gif" width="1" height="25" alt="" />
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

        <script type="text/javascript">
            elmCount++;
        </script>

        <jato:content name="textBox">
        <!-- text box display -->
        <tr>
        <form name="frm<jato:text name="txtIndex" />" action="blank"
            onSubmit="defaultSubmit(); return false;" method="post">

        <td nowrap="nowrap"><div class="logLbl">
            <jato:content name="isRequired">
            <img src="<%= ServiceURI %>/images/required.gif" alt="Required Field"
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
                value="<jato:text name="txtValue" />" class="TxtFld"></div>
        </td>
        </form>
        </tr>
        <!-- end of textBox -->
        </jato:content>

        <jato:content name="password">
        <!-- password display -->
        <form name="frm<jato:text name="txtIndex" />" action="blank"
            onSubmit="defaultSubmit(); return false;" method="post">
            <!--
            password fields carry Swekey ID, RT and OTP values, so they're
            hidden
            -->
            <input type="hidden" name="IDToken<jato:text name="txtIndex" />"
                id="IDToken<jato:text name="txtIndex" />"
                value="" class="TxtFld">
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
            <img src="<%= ServiceURI %>/images/required.gif" alt="Required Field"
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

        <jato:content name="ContentButtonLogin">
        <!-- Submit button -->

        <jato:content name="hasButton">
        <script type="text/javascript">
            defaultBtn = '<jato:text name="defaultBtn" />';
        </script>
        <tr>
        <td><img src="<%= ServiceURI %>/images/dot.gif"
        width="1" height="15" alt="" /></td>
        <td>
            <table border=0 cellpadding=0 cellspacing=0>
            <tr>
            <jato:tiledView name="tiledButtons"
                type="com.sun.identity.authentication.UI.ButtonTiledView">
                <script type="text/javascript">
                    markupButton(
                        '<jato:text name="txtButton" />',
                        "javascript:LoginSubmit('<jato:text name="txtButton" />')");
                </script>
            </jato:tiledView>
            </tr>
            </table>
        </td>
        </tr>
        <!-- end of hasButton -->
        </jato:content>

        <jato:content name="hasNoButton">
        <tr>
        <td><img src="<%= ServiceURI %>/images/dot.gif"
        width="1" height="15" alt="" /></td>
            <script type="text/javascript">
                markupButton(
                    '<jato:text name="lblSubmit" />',
                           "javascript:LoginSubmit('<jato:text name="lblSubmit" />')");
            </script>
        </tr>
        <!-- end of hasNoButton -->
        </jato:content>

        <!-- end of ContentButtonLogin -->
        </jato:content>
        <tr>
            <td>&nbsp;</td>
        </tr>
        <tr>
            <td><img src="<%= ServiceURI %>/images/dot.gif"
            width="1" height="33" alt="" /></td>
            <td>&nbsp;</td>
        </tr>
        </table>
      </td>
      <td width="45"><img src="<%= ServiceURI %>/images/dot.gif"
      width="45" height="245" alt="" /></td>
    </tr>
    </table>
    </td>
    <td class="LogMidBnd" style="background-image: url(<%= ServiceURI %>/images/gradlogsides.jpg);
    background-repeat:repeat-x;background-position:left top;">&nbsp;</td>
    </tr>
    <tr class="LogBotBnd" style="background-image: url(<%= ServiceURI %>/images/gradlogbot.jpg);
    background-repeat:repeat-x;background-position:left top;">
      <td>&nbsp;</td>
      <td><div class="logCpy"><span class="logTxtCpy">
        <auth:resBundle bundleName="amAuthUI" resourceKey="copyright.notice" /></span></div>
      </td>
      <td>&nbsp;</td>
    </tr>
  </table>

<jato:content name="validContent">
<auth:form name="Login" method="post" defaultCommandChild="DefaultLoginURL" >
<script type="text/javascript">
    if (elmCount != null) {
        for (var i = 0; i < elmCount; i++) {
            document.write(
                "<input name=\"IDToken" + i + "\" type=\"hidden\">");
        }
        document.write("<input name=\"IDButton"  + "\" type=\"hidden\">");
    }
</script>
<input type="hidden" name="goto" value="<%= gotoURL %>">
<input type="hidden" name="SunQueryParamsString" value="<%= encodedQueryParams %>">
<input type="hidden" name="encoded" value="<%= encoded %>">
</auth:form>

<!-- Swekey Integration Begin -->
<!--
    This code sets up the Swekey 'key' icon and the handler to swap the icon
    image as the key is inserted/removed.
-->
<script type="text/javascript">
// These are the bare minimum to make this work
swekey_loginname_path = ["IDToken1"];
swekey_image_xoffset = "1px";
swekey_image_yoffset = "-1px";
// These are not required, but can be set if desired
//swekey_str_unplugged = "No swekey plugged";
//swekey_str_plugged = "A swekey is plugged";
// Putting the images somewhere local is a good optimization :-)
//swekey_artwork_path = "/some-image-path";
// URL to load when the user clicks the 'key' icon
//swekey_promo_url = "http://myserver.com/";
swekey_login_integrate();
</script>
<!-- Swekey Integration End -->
</jato:content>

	</body>
</html>
</jato:useViewBean>
