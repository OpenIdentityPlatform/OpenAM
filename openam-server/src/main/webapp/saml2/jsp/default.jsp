<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: default.jsp,v 1.3 2008/10/29 03:11:52 veiming Exp $

--%>





<%@page
import="com.sun.identity.saml2.common.SAML2Constants,
com.sun.identity.saml2.common.SAML2Utils
"
%>

<html>
<head>
    <title>SAML2 Plugin Default Page</title>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
</head>

<body>
<%
    String messageParam = request.getParameter(SAML2Constants.MESSAGE);
    if (messageParam != null && messageParam.length() != 0) {
	%>
	<%= SAML2Utils.bundle.getString(messageParam) %>
	<%
    } else {
	%>
	<%= SAML2Utils.bundle.getString("missingMessageParam") %>
	<%
    }
%>

</body>
</html>
