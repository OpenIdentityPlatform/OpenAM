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

   $Id: index.jsp,v 1.2 2009/12/15 01:28:22 huacui Exp $

--%>


<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Token Service</title>
    </head>
    <body>
        <h1>Service Consumers Metadata Management</h1>
        <h2>
            Register a service consumer<br>
        </h2>
        <form name="consumer_reg" action="registerconsumer.jsp" method="POST">
            Service Consumer Name: <input type="text" name="name" value="" size="30" /><br><br>
            Service Consumer X509 Certificate (optional): <br><textarea name="cert" rows="10" cols="80"></textarea><br><br><br>
            Register this Service Consumer <input type="submit" value="Register" name="cons_reg" /><br>
        </form>
        <hr><br>
        <h2>
            Delete a service consumer<br>
        </h2>
        <form name="consumer_del" action="deleteconsumer.jsp" method="POST">
            Service Consumer Key: <input type="text" name="conskey" value="" size="100" /><br><br>

            Delete this Service Consumer <input type="submit" value="Delete" name="cons_del" /><br>
        </form>
        <hr><br>
    </body>
</html>
