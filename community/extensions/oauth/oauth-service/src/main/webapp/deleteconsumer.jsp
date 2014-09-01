<%-- 
    Document   : deleteconsumer
    Created on : May 15, 2009, 11:25:16 AM
    Author     : Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Consumer Deletion</title>
    </head>
    <body>
        <h1>Deleting the service consumer...</h1>
        <%
            String conskey = request.getParameter("conskey");

            try {

                java.net.URL url = new java.net.URL(conskey);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.connect();
                int resp = conn.getResponseCode();
                if (resp == 200) {
                    out.println("Service consumer deleted.");
                    }
                else {
                    out.println("Service could not be deleted - Unauthorized.");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace(new java.io.PrintWriter(out));
                    }
        %>
        <hr><br>
        <form name="return_ind" action="index.jsp" method="GET">
            Return to Main Menu <input type="submit" value="Return" name="return_ind" /><br>
        </form>
    </body>
</html>
