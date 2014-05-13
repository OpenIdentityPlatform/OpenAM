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

   $Id: deleteconsumer.jsp,v 1.1 2009/11/20 19:25:15 huacui Exp $

--%>
<%--
   Portions Copyrighted 2012-2014 ForgeRock AS
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.sun.identity.shared.debug.Debug"
        import="com.sun.identity.common.HttpURLConnectionManager" %>
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
                java.net.HttpURLConnection conn = HttpURLConnectionManager.getConnection(url);
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
                    Debug.getInstance("CoreToken").warning("Error while deleting consumer", ex);
                    out.println("An error occured while deleting the consumer");
                }
        %>
        <hr><br>
        <form name="return_ind" action="index.jsp" method="GET">
            Return to Main Menu <input type="submit" value="Return" name="return_ind" /><br>
        </form>
    </body>
</html>
