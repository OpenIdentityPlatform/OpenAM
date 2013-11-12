<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: saeerror.jsp,v 1.4 2009/08/28 00:25:39 sean_brydon Exp $

--%>

<%@ page import="org.owasp.esapi.ESAPI" %>

<html>
<head>
    <title>SAML2 SAE Error Page</title>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
</head>

<body>
<%
    String codeParam = request.getParameter("errorcode");
    codeParam = ESAPI.encoder().encodeForHTML(codeParam);
    String messageParam = request.getParameter("errorstring");
    messageParam  = ESAPI.encoder().encodeForHTML(messageParam);
%>
    errorcode= <%= codeParam %>
    messageParam= <%= messageParam %>

</body>
</html>    
