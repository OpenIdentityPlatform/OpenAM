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
   "Portions Copyrighted 2008 Miguel Angel Alonso Negro <miguelangel.alonso@gmail.com>"

   $Id: deniedAccess.jsp,v 1.1 2008/12/28 21:40:26 malonso Exp $

--%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<html>
    <body>
        <f:view>
            <h2>You don't have privileges to access to this feature</h2>
            <h:outputLink value="../main.jsp">Main menu</h:outputLink>
        </f:view>
    </body>
</html>
