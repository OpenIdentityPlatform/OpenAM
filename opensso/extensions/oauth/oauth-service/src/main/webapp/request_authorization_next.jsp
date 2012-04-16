<%-- 
    Document   : ResourceOwnerAuthorization
    Created on : May 14, 2009, 9:37:29 AM
    Author     : Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Token Service - Resource Owner Authorization</title>
    </head>
    <body>
        <%
            if (request.getParameter("oauth_callback") == null) {
                out.println("<h1>Error</h1><h2>No oauth_callback.</h1>");
                } else {
                if (request.getParameter("oauth_token") == null) {
                    out.println("<h1>Error</h1><h2>No oauth_token.</h1>");
                    }
                String svcname = request.getParameter("consumer_name");
                String svcpath = com.sun.oauth.resources.PathDefs.ServicePath;
                String authenticate = svcpath + "/authenticate.jsp" + "?oauth_callback=" + request.getParameter("oauth_callback");
                authenticate += "&oauth_token=" + request.getParameter("oauth_token");
        %>
        <h2>
            To authorize <%= svcname%> to access your resource, please
            enter your login credentials
            and press the Login button...<br>
        </h2>
        <form name="authn" action=<%= authenticate%> method="POST">
            Username: <input type="text" name="username" value="" size="25" /><br><br>
            Password: <input type="password" name="password" value="" size="25" /><br><br><br>

            Authenticate and Authorize <%= svcname%> to access of your resource <input type="submit" value="Login" name="auth" /><br>
            Enter REST URL: <input type="text" name="url" value="http://localhost:8080/opensso/identity" size="100" />
        </form>

        <%
            }
        %>
    </body>
</html>
