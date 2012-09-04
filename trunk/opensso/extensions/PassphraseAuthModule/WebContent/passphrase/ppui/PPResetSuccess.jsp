<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: PWResetSuccess.jsp,v 1.5 2008/08/28 06:41:11 mahesh_prasad_r Exp $
   
   "Portions Copyrighted [2012] [Forgerock AS]"
--%>

<%@include file="../ppui/PPResetBase.jsp"%>
<%@page info="PPResetSuccess" language="java" pageEncoding="UTF-8"%>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
	<jato:useViewBean className="com..passphrase.ui.PPResetSuccessViewBean" fireChildDisplayEvents="true">
		<head>
			<title>Passphrase Reset Confirmation</title>
			<link href="../css/portal.css" rel="stylesheet" type="text/css" />
			<script language="JavaScript" src="<%=passwordUrl%>/js/browserVersion.js" type="text/javascript"></script>
			<script language="JavaScript" src="<%=passwordUrl%>/js/password.js" type="text/javascript"></script>
			<script language="JavaScript" type="text/javascript">
				writeCSS('<%=passwordUrl%>');
			</script>
		</head>
		<body>
			<div id="box">
				<img src="../images/brand-logo.gif" alt="." />
				<div id="messagebox">
					<div id="message">
						<span id="title"><jato:text name="ccTitle"/></span>
						<span id="body"><jato:text name="resetMsg"/></span>
						<span id="body"><br/><a href="../">Return to Login page</a></span>
					</div>
			    </div>
			</div>
		</body>
	</jato:useViewBean>
</html>