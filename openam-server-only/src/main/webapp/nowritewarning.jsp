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

   $Id: nowritewarning.jsp,v 1.7 2008/10/23 06:21:06 mahesh_prasad_r Exp $

--%>

<%--
   Portions Copyrighted 2010-2012 ForgeRock Inc
--%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ page pageEncoding="UTF-8"%>
<%@ page import="com.sun.identity.setup.AMSetupServlet"%>
<%@ page import="com.sun.identity.setup.SetupConstants"%>
<%@ page import="java.text.MessageFormat"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="javax.servlet.ServletContext"%>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>OpenAM</title>
    <link rel="stylesheet" type="text/css" href="com_sun_web_ui/css/css_ns6up.css" />
    <link rel="shortcut icon" href="com_sun_web_ui/images/favicon/favicon.ico" type="image/x-icon" />

<%@ page contentType="text/html" %>

<script type="text/javascript"><!--// Empty script so IE5.0 Windows will draw table and button borders
//-->
</script>
  
</head>

<body class="LogBdy" onload="placeCursorOnFirstElm();">
<%  
    String deployURI = request.getRequestURI();
    if (deployURI != null) {
        int idx = deployURI.indexOf("/nowritewarning.jsp");
        if (idx > 0) {
            deployURI = deployURI.substring(0, idx);
        }
    }

    ResourceBundle rb = ResourceBundle.getBundle("amConfigurator", request.getLocale());
%>
  <table border="0" cellpadding="0" cellspacing="0" align="center" title="">
    <tr>
      <td width="50%"><img src="<%= deployURI %>/images/dot.gif" width="1" height="1" alt="" /></td>
      <td><img src="<%= deployURI %>/images/dot.gif" width="728" height="1" alt="" /></td>
      <td width="50%"><img src="<%= deployURI %>/images/dot.gif" width="1" height="1" alt="" /></td>
    </tr>
    <tr class="LogTopBnd" style="background-image: url(/fam/images/gradlogtop.jpg); 
    background-repeat: repeat-x; background-position: left top;">
      <td>&nbsp;</td>
      <td><img src="<%= deployURI %>/images/dot.gif" width="1" height="30" alt="" /></td>
      <td>&nbsp;</td>
    </tr>
    <tr>
        <td class="LogMidBnd" style="background-image: url(<%= deployURI %>/images/gradlogsides.jpg);
            background-repeat:repeat-x;background-position:left top;">&nbsp;</td>
        <td  style="background-image: url(<%= deployURI %>/images/login-backimage.jpg);
            background-repeat:no-repeat;background-position:left top;" height="435">
            <table border="0" background="<%= deployURI %>/images/dot.gif" cellpadding="0" cellspacing="0" width="100%" title="">
                <tr>
                    <td width="260"><img src="<%= deployURI %>/images/dot.gif" width="260" height="2" alt="" /></td>
                    <td align="left">
                        <div style="color: #f88017; font-size:24px">
                            <% out.print(rb.getString("product.name")); %>
                        </div>
                        <p>
                        <div style="color: #50697d; font-size:16px">
                            <img src="<%= deployURI %>/com_sun_web_ui/images/warning_large.gif" width="21" height="21" />
                            <%
                                Object[] paramDir = {System.getProperty("user.home")};
                                out.print(MessageFormat.format(
                                    rb.getString("nowriteaccess.title"), paramDir));
                            %>
                            <ul>
                                <li><a href="<%= deployURI %>/config/options.htm"><% out.print(rb.getString("nowriteaccess.proceed")); %></a><br />
                                <% out.print(rb.getString("nowriteaccess.proceed.note")); %>
                            </ul>
                        </div>   
                    </td>
                </tr>
            </table>
        </td>
        <td class="LogMidBnd" style="background-image: url(<%= deployURI %>/images/gradlogsides.jpg);
            background-repeat:repeat-x;background-position:left top;">&nbsp;
        </td>
    </tr>
    <tr class="LogBotBnd" style="background-image: url(<%= deployURI %>/images/gradlogbot.jpg);
        background-repeat:repeat-x;background-position:left top;">
          <td>&nbsp;</td>
          <td>  
              <div class="logCpy"><span class="logTxtCpy"><% out.print(rb.getString("product.copyrights")); %>
              </span></div>
          </td>
          <td>&nbsp;</td>
    </tr>
  </table>
</body>
</html>
