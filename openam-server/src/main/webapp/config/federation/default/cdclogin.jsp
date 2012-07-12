<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

   Copyright (c) 2011 ForgeRock AS. All Rights Reserved

   The contents of this file are subject to the terms
   of the Common Development and Distribution License
   (the License). You may not use this file except in
   compliance with the License.

   You can obtain a copy of the License at
   http://forgerock.org/license/CDDLv1.0.html
   See the License for the specific language governing
   permission and limitations under the License.

   When distributing Covered Code, include this CDDL
   Header Notice in each file and include the License file
   at http://forgerock.org/license/CDDLv1.0.html
   If applicable, add the following below the CDDL Header,
   with the fields enclosed by brackets [] replaced by
   your own identifying information:
   "Portions Copyrighted [year] [name of copyright owner]"
--%>
<%@ page language="java"
import="com.sun.identity.federation.common.IFSConstants,
com.sun.identity.federation.common.FSUtils"
isELIgnored="false"
%>
<html>
 <body onload="document.Response.submit()">
  <form name="Response" method="post" action="${destURL}">
   <input type="hidden" name="<%= IFSConstants.POST_AUTHN_RESPONSE_PARAM %>" value="${authnResponse}"/>
   <noscript>
    <center>
     <input type="submit" value="<%= FSUtils.bundle.getString("laresPostCustomKey") %>"/>
    </center>
   </noscript>
  </form>
 </body>
</html>
