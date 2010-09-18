<%-- 
    Document   : register
    Created on : May 15, 2009, 9:53:21 AM
    Author     : Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Consumer Registration</title>
    </head>
    <body>
        <h1>Registering Service...</h1>
        <%
            String name = request.getParameter("name");
            String svcuri = request.getParameter("svcuri");
            String iconuri = request.getParameter("iconuri");
            String rsakey = request.getParameter("rsakey");
            String regurl = "http://localhost:8080/TokenService/resources/oauth/v1/consumer_registration";

            try {
                java.net.URL url = new java.net.URL(regurl);
                java.net.URLConnection conn = url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                java.io.DataOutputStream dos = new java.io.DataOutputStream(conn.getOutputStream());
                String postmsg = "name=" + java.net.URLEncoder.encode(name)
                        + "&service=" + java.net.URLEncoder.encode(svcuri)
                        + "&icon=" + java.net.URLEncoder.encode(iconuri);
                if (rsakey != null) {
                    rsakey = rsakey.replaceAll("[\\r\\n]", "");
                    postmsg += "&rsapublickey=" + java.net.URLEncoder.encode(rsakey);
                    }
                dos.writeBytes(postmsg);
                dos.flush();
                dos.close();
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(
                        (java.io.InputStream) conn.getContent()));
                out.println("<h2> Service Consumer registered.</h2>");
                String line;
                String buf = "";
                while ((line = reader.readLine()) != null) {
                    buf += line;
                    }
                java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(buf, "&");
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    out.println(java.net.URLDecoder.decode(token) + "<br>");
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
