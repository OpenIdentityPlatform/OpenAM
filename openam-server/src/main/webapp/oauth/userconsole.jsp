<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

   Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved

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

   $Id: userconsole.jsp,v 1.3 2010/01/20 17:51:38 huacui Exp $

--%>
<%--
   Portions Copyrighted 2012 ForgeRock AS
--%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="com.sun.identity.oauth.service.OAuthServiceException" %>
<%@page import="com.sun.identity.oauth.service.PathDefs" %>
<%@page import="com.sun.identity.oauth.service.util.OAuthProperties" %>
<%@page import="com.sun.identity.oauth.service.util.OAuthServiceUtils" %>
<%@page import="java.net.URLEncoder" %>
<%@page import="javax.servlet.http.Cookie" %>
<%@page import="javax.servlet.http.HttpServletRequest" %>
<%@page import="javax.servlet.http.HttpServletResponse" %>

<%--
The taglib directive below imports the JSTL library. If you uncomment it,
you must also add the JSTL library to the project. The Add Library... action
on Libraries node in Projects view can be used to add the JSTL 1.1 library.
--%>
<%--
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
--%>

    <%!
        static String openssoCookieName = OAuthProperties.get(
                                PathDefs.OPENSSO_COOKIE_NAME);
        String getUid(String cookieValue) 
            throws OAuthServiceException {
            String uuid = null;
            try {
                uuid = OAuthServiceUtils.getUUIDByTokenId(cookieValue);
            } catch (OAuthServiceException oe) {
                throw oe;
            }
            return uuid;
        }
    %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Authentication</title>

        <script type="text/javascript">
            function makeithappen(oauthtk, id) {
                var path = window.location.pathname;
                var strs = path.split("/");
                var contextRoot = strs[1]; 
                var redir = window.location.protocol;
                redir += "//" + window.location.host;
                redir += "/" + contextRoot;
                redir += "/resources/1/oauth/AuthorizationFactory";
                redir += "?oauth_token=" + oauthtk.toString();
                redir += "&id=" + id.toString();
                window.location = redir;
            }
            
            function revoke(oauthtk) {
                var redir = "./deletetoken.jsp?oauth_token=";
                redir += oauthtk.toString();
                window.location = redir;
            }
        </script>
    </head>
    <body>

    <h1 align="center">OAuth User Authorization Page</h1><hr>


    <%
        String otk = request.getParameter("oauth_token");
        if (otk == null) {
            out.println("<h1>Error</h1><h2>OAuth token is missing.</h2>");
            return;
        }
        otk = java.net.URLEncoder.encode(otk);
        String uid = null;

        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (int i=0; i < cookies.length; i++) {
                Cookie nextCookie = cookies[i];
                String name = nextCookie.getName();
                String value = nextCookie.getValue();
                if (name.equals(openssoCookieName)) {
                    try {
                        if (OAuthServiceUtils.isTokenValid(value)) {
                        // get the UUID of the user based on the OpenAM session cookie
                            uid = getUid(value);
                        }
                    } catch (OAuthServiceException oe) {
                        uid = null;
                        //throw new ServletException(oe.getMessage());
                    }
                    break;
                }
            }
        }
        if (uid == null) {
            // session is not valid so redirect the user to OpenAM login page
            String loginURL = 
                OAuthProperties.get(PathDefs.OPENSSO_SERVER_URL) +
                OAuthProperties.get(PathDefs.OPENSSO_SERVER_LOGIN_URI);
            StringBuffer requestURL = httpRequest.getRequestURL();
            String query = httpRequest.getQueryString();
            if (query != null) {
                requestURL.append("?").append(query);
            }
            StringBuffer redirectURL = new StringBuffer();
            redirectURL.append(loginURL);
            if (loginURL.indexOf("?") > 0) {
                redirectURL.append("&");
            } else {
                redirectURL.append("?");
            }
            redirectURL.append("goto");
            redirectURL.append("=");
            redirectURL.append(URLEncoder.encode(requestURL.toString()));
            httpResponse.sendRedirect(redirectURL.toString());
            return;
        }

        out.println("User ID: " + uid + "<br>");
        uid = java.net.URLEncoder.encode(uid);
    %>

    <hr><br>
    <h2>Do you authorize the Service Consumer to access your resource?</h2>
    <form name="AuthoriseToken" >
        <input type="button" onclick="revoke('<%= otk%>')" value="Revoke" name="Revoke">
        <input type="button" onclick="makeithappen('<%= otk%>', '<%= uid%>')" value="Authorize" name="Authorize">
    </form>

    <hr>
    </body>
</html>
