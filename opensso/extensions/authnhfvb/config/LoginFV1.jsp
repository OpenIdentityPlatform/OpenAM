<%--
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
                                                                                                                                                                
   Copyright 2008 Sun Microsystems Inc. All Rights Reserved
--%>


<html>

<%@page info="Login" language="java"%>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>
<%@taglib uri="/WEB-INF/auth.tld" prefix="auth"%>
<jato:useViewBean className="com.sun.identity.authentication.UI.LoginViewBean">

<%@ page contentType="text/html" %>

<head>
<title><jato:text name="htmlTitle_Login" /></title>

<%
String ServiceURI = (String) viewBean.getDisplayFieldValue(viewBean.SERVICE_URI);
String encoded = "false";
String gotoURL = (String) viewBean.getValidatedInputURL(
    request.getParameter("goto"), request.getParameter("encoded"), request);
if ((gotoURL != null) && (gotoURL.length() != 0)) {
    encoded = "true";
}
%>

<link rel="stylesheet" href="<%= ServiceURI %>/css/styles.css" type="text/css">
<script language="JavaScript" src="<%= ServiceURI %>/js/browserVersion.js"></script>
<script language="JavaScript" src="<%= ServiceURI %>/js/auth.js"></script>

<script language="JavaScript">

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

<body class="LogBdy" onload="defaultSubmit();">

<jato:content name="validContent">
<auth:form name="Login" method="post"
    defaultCommandChild="DefaultLoginURL" >

<script language="javascript">
    if (elmCount != null) {
        for (var i = 0; i < elmCount; i++) {
            document.write(
                "<input name=\"IDToken" + i + "\" type=\"hidden\">");
        }
        document.write("<input name=\"IDButton"  + "\" type=\"hidden\">");
    }
</script>
<input type="hidden" name="goto" value="<%= gotoURL %>">
<input type="hidden" name="encoded" value="<%= encoded %>">
</auth:form>
</jato:content>

</body>

</jato:useViewBean>

</html>
