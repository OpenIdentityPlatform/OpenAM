<%--
/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */
--%>

<%@ page pageEncoding="UTF-8" %>
<%@ page import="org.forgerock.openam.openidconnect.CheckSessionImpl" %>
<%@ page import="org.forgerock.openidconnect.CheckSession" %>
<%@ page import="org.owasp.esapi.ESAPI" %>
<%
    CheckSession checkSession = new CheckSessionImpl();
    String cookieName = checkSession.getCookieName();
    String clientSessionURI = checkSession.getClientSessionURI(request);
    Boolean validSession = checkSession.getValidSession(request);

%>
<!DOCTYPE html>
<html>
<head>
    <title></title>
</head>
<body>
<script src="../../js/sha256.js"></script>
<script type="text/javascript">

    window.addEventListener("message", receiveMessage, false);
    var client_id;
    function receiveMessage(e){
        data = e.data.split(' ');
        client_id = data[0];
        var clientURI = "<%=ESAPI.encoder().encodeForJavaScript(clientSessionURI)%>";
        if (e.origin !== clientURI){
            return;
        }
        var session_state = data[1];
        var opbs = getBrowserState();
        var ss = CryptoJS.SHA256(client_id + e.origin  + opbs);
        if (session_state == ss) {
            stat = 'unchanged';
        } else {
            stat = 'changed';
        }
        e.source.postMessage(stat, e.origin);
     }

    function getBrowserState(){
        var validSession = "<%=validSession%>";

        if (!validSession){
            return "";
        }
        var cookieName = "<%=ESAPI.encoder().encodeForJavaScript(cookieName)%>" + "=";
        var cookies = document.cookie+";";
        var cookieStart = cookies.indexOf(cookieName);
        if (cookieStart != -1) {
            var end = cookies.indexOf(";", cookieStart);
            return unescape(cookies.substring(cookieStart + cookieName.length, end));
        }
        return "";
    }
</script>
</body>
</html>