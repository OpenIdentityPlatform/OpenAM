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
                                                                                
   $Id: ChangePassword.jsp,v 1.1 2009/07/24 23:12:45 manish_rustagi Exp $
                                                                                
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
%>

<link rel="stylesheet" href="<%= ServiceURI %>/css/styles.css" type="text/css" />
<script language="JavaScript" src="<%= ServiceURI %>/js/browserVersion.js"></script>
<script language="JavaScript" src="<%= ServiceURI %>/js/auth.js"></script>
</head>

<%
    System.out.println("AccountId: " + request.getParameter("IDToken1"));
    System.out.println("goto: " + request.getParameter("goto"));
	System.out.println("plaingoto: " + request.getParameter("plaingoto"));

    String accountId = request.getParameter("IDToken1");
    String gotoURL = request.getParameter("plaingoto");

    //Change the IDM url so that it points to the correct IDM application
    String redirectURL = "http://localhost:8081/idm/anonuser/anonResetPassword.jsp";
    if(accountId != null){
        redirectURL = redirectURL + "?accountId=" + accountId;
	}
    if(gotoURL != null && !gotoURL.equals("null") && (gotoURL.length() > 0)){
        if(accountId == null){
            redirectURL = redirectURL + "?goto=" + gotoURL;
		}else{
            redirectURL = redirectURL + "&goto=" + gotoURL; 
		}
	}
    System.out.println("Redirect URL is:" + redirectURL);
	response.sendRedirect(redirectURL);
%>

</jato:useViewBean>

</html>
