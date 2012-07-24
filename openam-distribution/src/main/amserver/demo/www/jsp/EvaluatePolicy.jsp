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
  
   $Id: EvaluatePolicy.jsp,v 1.2 2008/06/25 05:40:25 qcheng Exp $
  
-->
<%@ page import="com.iplanet.sso.SSOToken"%>
<%@ page import="com.iplanet.am.util.SystemProperties"%>
<%@ page import="import com.sun.identity.shared.debug.Debug"%>
<!  --%@ page import="com.iplanet.am.util.Debug"% -->
<%@ page import="com.sun.identity.common.Constants"%>
<%@ page import="com.iplanet.sso.SSOTokenManager"%>
<%@ page import="com.iplanet.sso.SSOException"%>

<html>
    <head>
        <title>OpenAM Demo | Policy Evaluation</title>
    </head>
    <%
    String serverUrl = SystemProperties.get(Constants.AM_SERVER_PROTOCOL)		+ "://" + SystemProperties.get(Constants.AM_SERVER_HOST)
	+ ":" + SystemProperties.get(Constants.AM_SERVER_PORT)
	+ SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
    SSOTokenManager mgr = SSOTokenManager.getInstance();
    SSOToken token = null;
    Debug debug = Debug.getInstance("amEvaluatePolicyServlet");
    String requestUrl = request.getRequestURL().toString();
    try {
        token = mgr.createSSOToken(request);
    } catch( SSOException e)  {
	debug.error("evaluatePolicy.jsp:: error during token creation.",e);
        String redirectUrl = serverUrl+"/UI/Login" + "?goto=" + requestUrl;
        response.sendRedirect(redirectUrl);
    %>
 <% } %>
    <% if ((token == null) || !(mgr.isValidToken(token)))  { %>
	<p> <b>Sorry, you do not have a valid token to access this site. 
        Please login....</b>
     <% } %>
    <body bgcolor="#FFFFFF" text="#000000">
        <table width="600">
        <tr>
        <td>
        <h3>Evaluate Policy</h3>
        <p>
        Please enter  the resource which you wish to access.
        </p>        
        <form action="<%=request.getContextPath()%>/evaluatePolicy" 
            method="POST">
            <table bgcolor="000000" cellpadding="1" cellspacing="0">
            <tr><td>
            <table bgcolor="F0F0F0" border="0" cellpadding="5" cellspacing="0">
                <tr>
                    <td>Resource to be accessed</td>
                    <td><input type="text" name="resource" size="80"></td>
                </tr>
                <tr>
                    <td colspan="2" align="center">
                        <input type="submit" name="submit" value="Evaluate">
                        <input type="reset">
                    </td>
                </tr>
            </table>
            </td></tr>
            </table>
        </form>
        </td>
        </tr>
        </table>
    </body>
</html>
