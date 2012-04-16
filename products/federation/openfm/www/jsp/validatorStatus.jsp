<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: validatorStatus.jsp,v 1.3 2009/01/05 23:23:25 veiming Exp $

--%>

<%@ page contentType="text/html; charset=utf-8" language="java" %>

<html>
<head>
<script language="Javascript">
<%
    String step = request.getParameter("s");
    String status = request.getParameter("v");
    if (step != null) {
        if (step.equals("idpauth")) {
            if (status.equals("1")) {
                out.println("top.authIdpPassed()");
            } else {
                out.println("top.authIdpFailed()");
            }
        } else if (step.equals("spauth")) {
            if (status.equals("1")) {
                out.println("top.authSpPassed()");
            } else {
                out.println("top.authSpFailed()");
            }
        } else if (step.equals("acclink")) {
            if (status.equals("1")) {
                out.println("top.accLinkPassed()");
            }
        } else if (step.equals("slo")) {
            if (status.equals("1")) {
                out.println("top.singleLogoutPassed()");
            }
        } else if (step.equals("sso")) {
            if (status.equals("1")) {
                out.println("top.singleLoginPassed()");
            }
        } else if (step.equals("accTerm")) {
            if (status.equals("1")) {
                out.println("top.accTermPassed()");
            }
        }
    }
%>
</script>
</head>
</html>
