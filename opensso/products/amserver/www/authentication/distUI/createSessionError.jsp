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
                                                                                
   $Id: createSessionError.jsp,v 1.6 2008/08/19 19:10:45 veiming Exp $
                                                                                
--%>




<html>

<%@page info="Session Creation Error" language="java"%>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>
<%@taglib uri="/WEB-INF/auth.tld" prefix="auth"%>
<jato:useViewBean className="com.sun.identity.authentication.distUI.LoginViewBean">

<%@ page contentType="text/html" %>

<head>
<TITLE><jato:text name="htmlTitle_CreateSessionError" /></TITLE>

<% 
String ServiceURI = (String) viewBean.getDisplayFieldValue(viewBean.SERVICE_URI);
%>

<link rel="stylesheet" href="<%= ServiceURI %>/css/styles.css" type="text/css" />
<script language="JavaScript" src="<%= ServiceURI %>/js/browserVersion.js"></script>
<script language="JavaScript" src="<%= ServiceURI %>/js/auth.js"></script>

<script language="javascript">
    writeCSS('<%= ServiceURI %>');
</script>
<script type="text/javascript"><!--// Empty script so IE5.0 Windows will draw table and button borders
//-->
</script>
</head>

<body bgcolor="#FFFFFF" text="#000000" leftmargin="9" marginwidth="9"
    topmargin="9" marginheight="9">
<br>
<table width="100%" border="0" cellspacing="0" cellpadding="0">

    <!-- branding -->
    <tr>
    <td width="110"><img src="<%= ServiceURI %>/login_images/logo_sun.gif" width="110"
        height="82" alt="Sun Microsystems Logo"></td>
    <td><img src="<%= ServiceURI %>/login_images/spacer.gif" width="9" height="1" 
        alt=""></td>
    <td valign="bottom" bgcolor="#ACACAC" width="100%"><img
        src="<%= ServiceURI %>/login_images/Identity_LogIn.gif" alt="OpenSSO"></td>
    </tr>
    <tr>
    <td colspan="3"><img src="<%= ServiceURI %>/login_images/spacer.gif" width="1"
        height="39" alt=""></td>
    </tr>

    <tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
    <td>
    <table border="0" cellspacing="0" cellpadding="10">
        <tr>
            <td>
            <table border="0" cellspacing="0" cellpadding="2" class="alert-error-frame">
            <tr>
                <td>
                <table cellspacing="0" cellpadding="5" border="0" class="alert-error-content">
                <tr>
                    <td valign="top">
                    <img src="<%= ServiceURI %>/login_images/error_32_sunplex.gif" 
                    width="32" height="32" border="0">
                    </td>
                    <td>
                    <div class="alert-header-text">
                    <auth:resBundle bundleName="amAuthUI" resourceKey="create.session.error" />
                    </div>
                    <div class="alert-normal-text">
                    <!---- hyperlink ---->
                    <jato:content name="ContentHref">
                    <p><auth:href name="LoginURL"
                        fireDisplayEvents='true'><jato:text
                        name="txtGotoLoginAfterFail" /></auth:href></p>
                    </jato:content>
                    </div>
                    </td>
                </tr>
                </table>
                </td>
            </tr>
            </table>
            </td>
        </tr>
    </table>
    </td>
    </tr>

    <tr>
    <td colspan="3"><img src="<%= ServiceURI %>/login_images/spacer.gif" width="1"
        height="57" alt=""></td>
    </tr>

    <!-- copyrights -->
    <tr>
    <td width="110" align="right" valign="top"><img
        src="<%= ServiceURI %>/login_images/Java.gif" width="52" height="83" 
        alt="Sun Java System Software"></td>
    <td><img src="<%= ServiceURI %>/login_images/spacer.gif" width="9" height="1" 
        alt=""></td>
    <td valign="top" class="footerText">
        <auth:resBundle bundleName="amAuthUI" resourceKey="copyright.notice" />
    </td>
    </tr>
      
</table>
</body>

</jato:useViewBean>
</html>
