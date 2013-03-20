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

   $Id: dumpcookies.jsp,v 1.5 2009/01/27 18:01:07 weisun2 Exp $

--%>

<%@ page import="java.io.*"%>
<%@ page import="java.util.Enumeration"%>
<%@ page import="com.iplanet.sso.*"%>
<%@ page import="com.sun.identity.common.SystemConfigurationUtil"%>

<%!
public void jspInit()
{
    System.out.println("dumpcookies.jsp : JSPINIT CALLED...." + 
        getServletConfig().getServletContext());
}
%>
<%
    String deployuri = SystemConfigurationUtil.getProperty(
        "com.iplanet.am.services.deploymentDescriptor");
    if ((deployuri == null) || (deployuri.length() == 0)) {
        deployuri = "../../..";
    }
%>
<html>
<head>
<title>Secure Attributes Exchange IDP APP SAMPLE</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
</head>
<body>
<%@ include file="header.jspf" %>
<br>
<% 

    try 
    {
        SSOTokenManager manager = SSOTokenManager.getInstance();
        SSOToken token = manager.createSSOToken(request);
    
        if (!manager.isValidToken(token)) {
           out.println("No FM session");
        } else {
           out.println("FM session:"+token.getPrincipal());
           out.println("<br>  prop branch="+token.getProperty("branch"));
           out.println("<br>  prop mail="+token.getProperty("mail"));
        }
       
    } catch (Exception ex2) {
        out.println("no FM session ");
    }
   out.println("<br><br>Cookies:<br>");
   Cookie[] cookies = request.getCookies();
   for (int i =0; i < cookies.length; i++) {
       Cookie ck = cookies[i];
       String cn = ck.getName();
       out.println("<br> cookiename="+cn+" val="+ ck.getValue());
   }
   out.println("<br><br>Headers:<br>");
   Enumeration en = request.getHeaderNames();
   while (en.hasMoreElements())
   {
        String hn = (String) en.nextElement();
        out.println("<br>name="+hn+" val="+request.getHeader(hn));
   }
%>
</body>
</html>
