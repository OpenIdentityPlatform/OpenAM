<%-- 
    Document   : authenticate
    Created on : May 14, 2009, 10:01:24 AM
    Author     : Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
--%>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%--
The taglib directive below imports the JSTL library. If you uncomment it,
you must also add the JSTL library to the project. The Add Library... action
on Libraries node in Projects view can be used to add the JSTL 1.1 library.
--%>
<%--
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
--%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Authentication</title>

        <script type="text/javascript">
            function makeithappen(oauthtk, cbk, id) {
                var redir = "http://localhost:8080/TokenService/resources/oauth/v1/AuthorizationFactory";
                redir += "?oauth_callback=" + cbk.toString();
                redir += "&oauth_token=" + oauthtk.toString();
                redir += "&id=" + id.toString();
                window.location = redir;
            }
            
            function cancel(oauthtk) {
                var redir = "./index.jsp?oauth_token=";
                redir += oauthtk.toString();
                window.location = redir;
            }
        </script>
    </head>
    <body>

    <h1 align="center">Authentication Results</h1><hr>

    <%
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String type = request.getParameter("auth");
        String url = request.getParameter("url");
        String cbk = request.getParameter("oauth_callback");
        String otk = request.getParameter("oauth_token");
        String uid = null;


        try {

            if (username == null || username.length() == 0 ||
                password == null || password.length() == 0) {
                out.println("<h2>Invalid Username or Password</h2>");
                out.println("<br><br>Either user name or password is null.");
            } else {
                url += "/authenticate";
                java.net.URL iurl = new java.net.URL(url);
                java.net.URLConnection connection = iurl.openConnection();
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
                // Send POST output.
                java.io.DataOutputStream printout = new java.io.DataOutputStream(
                    connection.getOutputStream ());
                String content =
                    "username=" + java.net.URLEncoder.encode (username) +
                    "&password=" + java.net.URLEncoder.encode (password);
                printout.writeBytes (content);
                printout.flush ();
                printout.close ();
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(
                    (java.io.InputStream) connection.getContent()));
                out.println("<h2>Successful Authentication</h2>");
                String line;
                while ((line = reader.readLine()) != null) {
                    out.println(line + "<br>");
                    int index = line.indexOf("token");
                    if (index != -1)
                        uid = line.substring(9);
                    }
                }   
               } catch (Exception ex) {
                   try {
                       ex.printStackTrace(new java.io.PrintWriter(out));
                       } catch (Exception e) {
                                // Ignore
                           }
                   }
        uid = "hubert";
        if (uid != null) {
    %>
    <hr><br>
    <h2>Do you authorize the Service Consumer to access your resource?</h2>
    <form>
        <input type="button" onclick="cancel('<%= otk%>')" value="Cancel">
        <input type="button" onclick="makeithappen('<%= otk%>', '<%= cbk%>', '<%= uid%>')" value="Authorize">
    </form>

    <hr>
    <%
        }
        else {
            %>
            <h2>Ooooh - You're not who you claimed to be!!</h2>
            <%
            }
    %>
    <hr/>
    </body>
</html>
