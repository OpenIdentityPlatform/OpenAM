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

   $Id: welcome.jsp,v 1.6 2008/08/19 19:09:40 veiming Exp $

--%>

<%--
   Portions Copyrighted 2010 ForgeRock AS
--%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ page import="com.sun.identity.setup.AMSetupServlet"%>
<%@ page import="com.sun.identity.setup.SetupConstants"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.servlet.ServletContext"%>

<%@taglib uri="/WEB-INF/configurator.tld" prefix="config" %>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>OpenAM</title>
    <link rel="stylesheet" type="text/css" href="com_sun_web_ui/css/css_ns6up.css" />
    <link rel="shortcut icon" href="com_sun_web_ui/images/favicon/favicon.ico" type="image/x-icon" />

    <script language="Javascript">
        function gotoLoginPage() {
            this.location.replace("./index.html");
        }

    </script>

<%@ page contentType="text/html" %>

<script type="text/javascript"><!--// Empty script so IE5.0 Windows will draw table and button borders
//-->
</script>
  
</head>

<body class="LogBdy" onload="placeCursorOnFirstElm();">
<%  
    String deployURI = request.getRequestURI();
    if (deployURI != null) {
        int idx = deployURI.indexOf("/welcome.jsp");
        if (idx > 0) {
            deployURI = deployURI.substring(0, idx);
        }
    }
%>
  <table border="0" cellpadding="0" cellspacing="0" align="center" title="">
    <tr>
      <td width="50%"><img src="<%= deployURI %>/images/dot.gif" width="1" height="1" alt="" /></td>
      <td><img src="<%= deployURI %>/images/dot.gif" width="728" height="1" alt="" /></td>
      <td width="50%"><img src="<%= deployURI %>/images/dot.gif" width="1" height="1" alt="" /></td>
    </tr>
    <tr class="LogTopBnd" style="background-image: url(<%= deployURI %>/images/gradlogtop.jpg); 
    background-repeat: repeat-x; background-position: left top;">
      <td>&nbsp;</td>
      <td><img src="<%= deployURI %>/images/dot.gif" width="1" height="30" alt="" /></td>
      <td>&nbsp;</td>
    </tr>
    <tr>
        <td class="LogMidBnd" style="background-image: url(<%= deployURI %>/images/gradlogsides.jpg);
            background-repeat:repeat-x;background-position:left top;">&nbsp;</td>
        <td  style="background-image: url(<%= deployURI %>/images/login-backimage.jpg);
            background-repeat:no-repeat;background-position:left top;" height="435" align="center">
            <table border="0" background="<%= deployURI %>/images/dot.gif" cellpadding="0" cellspacing="0" width="100%" title="">
                <tr>
                    <td width="260"><img src="<%= deployURI %>/images/dot.gif" width="260" height="2" alt="" /></td>
                    <td>

                    <!-- welcome message -->
                    <div style="color: #50697d; font-size:16px">
                        Welcome to the 
                    </div>
                    <p>
                    <div style="color: #f88017; font-size:24px">
                        OpenSSO
                    </div>
                    <p>
                    <div style="color: #50697d; font-size:16px">
                        Select one of the options below to get started.
                    </div>

                    <table width="100%" border=0>
                        <!-- spacer -->
                        <tr>
                            <td colspan=2>&nbsp;</td>
                        </tr>
                        <tr>
                            <td>
                                <div style="color: #50697d; font-size:18px" >
                                    Simple
                                </div>
                            </td>
                        </tr>
                        <TR VALIGN=TOP>              
                            <TD>
                                <i>
                                  <a href="<%= deployURI %>/configurator.jsp?type=simple">Enter only the password</a>
                                </i>
                                for the default administrator. All other data is configured
                                using default parameters.                        
                            </TD>
                        </TR>

                        <!-- spacer  -->
                        <tr>
                            <td colspan=2>&nbsp;</td>
                        </tr>
                        <tr>
                            <td>
                                <div style="color: #50697d; font-size:18px" >
                                    Custom
                                </div>
                            </td>
                        </tr>
                        <TR VALIGN=TOP>
                            <TD>
                                <i>
                                  <a href="<%= deployURI %>/configurator.jsp?type=custom">Specify all parameters</a>
                                </i>
                                including the type of configuration data store, encryption 
                                properties, user data store, etc. This option has the most
                                flexibility in setting up your installation.
                            </TD>
                        </TR>
                    </table>
                    <p>    
   
                    </td>

                </tr>
            </table>
        </td>
        <td class="LogMidBnd" style="background-image: url(<%= deployURI %>/images/gradlogsides.jpg);
        background-repeat:repeat-x;background-position:left top;">&nbsp;</td>
    </tr>
    <tr class="LogBotBnd" style="background-image: url(<%= deployURI %>/images/gradlogbot.jpg);
        background-repeat:repeat-x;background-position:left top;">
          <td>&nbsp;</td>
          <td>
              <div class="logCpy"><span class="logTxtCpy">
                  Copyright © 2010 ForgeRock AS, Philip Pedersens vei 1, 1366 Lysaker, Norway. All rights reserved. Licensed for use under the Common Development and Distribution License (CDDL), see http://www.forgerock.com/license/CDDLv1.0.html  for details. This software is based on the OpenSSO/OpenAM open source project and the source includes the copyright works of other authors, granted for use under the CDDL. This distribution may include other materials developed by third parties. All Copyrights and Trademarks are property of their owners.<br>
              </span></div>
          </td>
          <td>&nbsp;</td>
    </tr>
  </table>

</body>
</html>
