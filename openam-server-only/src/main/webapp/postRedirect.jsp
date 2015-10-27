<%--
    Copyright 2015 ForgeRock AS.

    The contents of this file are subject to the terms of the Common Development and
    Distribution License (the License). You may not use this file except in compliance with the
    License.

    You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
    specific language governing permission and limitations under the License.

    When distributing Covered Software, include this CDDL Header Notice in each file and include
    the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
    Header, with the fields enclosed by brackets [] replaced by your own identifying
    information: "Portions copyright [year] [name of copyright owner]".
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map" %>

<html>
<head>
    <title>Mailman</title>
</head>
<body onload="document.forms[0].submit()">
<form method="post" action="${postURL}">

    <%
        Map postData = (Map) request.getAttribute("postData");
        for (Object o : postData.keySet()) {
            String k = (String) o;
            String v = (String) postData.get(k);
    %>
        <input type="hidden" name="<%=k%>" value="<%=v%>" />
    <%
        }
    %>
    <noscript>
        <input type="submit" value="Submit" />
    </noscript>
</form>
</body>
</html>