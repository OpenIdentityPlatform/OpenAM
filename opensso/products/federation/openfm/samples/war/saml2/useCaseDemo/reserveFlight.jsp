<!--
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

   $Id: reserveFlight.jsp,v 1.5 2008/11/25 23:50:43 exu Exp $

-->

<%@ include file="init.jspf" %>


<%  if (ssoToken == null) {
        response.sendRedirect("home.jsp");
    }
%>

<html>
<head>
<title>Reserve Flight</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
</head>

<body>

<%@ include file="header.jspf" %>

<h3 align="center"> <%= myTitle%> appreciates your business, <%= userLabel %> </h3>

<hr/>
<form>                                              
<table cellpadding="2" cellspacing="2" border="0" width="100%" align="justify">
    <tr><td align="right" colspan="4"><a href="home.jsp"><%= myTitle %> Home</a></td></tr>
    <tr><td align="center" colspan="4"><b>Reserve Flight</b></td></tr>
    <tr>
        <td valign="top" align="right">Departing From</td>
        <td valign="top" align="left">                  
                <select>
                    <option>San Jose</option>
                    <option>San Francisco</option>
                    <option>Los Angeles</option>
                    <option>San Diego</option>
                </select>
        </td>
        <td valign="top" align="right">Arriving At</td>
        <td valign="top" align="left">                        
                <select>
                    <option>Los Angeles</option>
                    <option>San Jose</option>
                    <option>San Francisco</option>
                    <option>San Diego</option>
                </select>
        </td>
    </tr>
    <tr><td colspan="4" align="center"><input type="submit" value="Submit"></td></tr>

</table>
</form>
                                                    
</body>
</html>
