<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
  
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
                                                                                
   $Id: session_timeout.jsp,v 1.1.1.1 2008/10/29 08:05:21 svbld Exp $
   "Portions Copyrighted [2012] [Forgerock AS]"

--%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@page info="Account Locked" language="java"%>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>
<%@taglib uri="/WEB-INF/auth.tld" prefix="auth"%>
<%@ page contentType="text/html" %>
<%@include file="init.jsp"%>

<html xmlns="http://www.w3.org/1999/xhtml">
<jato:useViewBean className="com.sun.identity.authentication.UI.LoginViewBean">
	<head>
		<title>Account Locked</title>
		<% 
		String ServiceURI = (String) viewBean.getDisplayFieldValue(viewBean.SERVICE_URI);
		%>
		<link rel="stylesheet" href="<%=serviceURL%>/css/main.css" type="text/css" />
		<!--[if lt IE 8]>
		<link rel="stylesheet" type="text/css" href="<%=serviceURL%>/css/ielt8.css" />
		<![endif]-->
		<!--[if IE 7]>
		<link rel="stylesheet" type="text/css" href="<%=serviceURL%>/css/ie7.css" />
		<![endif]-->
		<!--[if IE 6]>
		<link rel="stylesheet" type="text/css" href="<%=serviceURL%>/css/ie6.css" />
		<![endif]-->
		<script language="JavaScript" src="<%= ServiceURI %>/js/browserVersion.js"></script>
		<script language="JavaScript" src="<%= ServiceURI %>/js/auth.js"></script>
	
		<script language="javascript">
			writeCSS('<%= ServiceURI %>');
		</script>
		<script type="text/javascript"><!--// Empty script so IE5.0 Windows will draw table and button borders
		//-->
		</script>
	</head>
	<body>
		<div id="outer">
				<div id="container">
					<div id="inner">						
						<div id="logo"><a href="/"><img src="<%=serviceURL%>/images/logo150.gif" alt=". Logo" /></a></div>			
							<div id="messagebox">
								<div id="message">
									<div class="indented">
										<span id="title">
											<auth:resBundle bundleName="amAuthUI" resourceKey="usernot.active" />
											<auth:resBundle bundleName="amAuthUI" resourceKey="contactadmin" />
										</span>
									</div>	
									<div class="indented" style="margin-top:10px;">
										<jato:content name="ContentHref">
											<!-- hyperlink -->
											<span id="body"><auth:href name="LoginURL" fireDisplayEvents='true'><jato:text name="txtGotoLoginAfterFail" /></auth:href></span>
										</jato:content>
									</div>
								</div>
							</div>
					</div>
				</div>
				<div id="footerMessage"></div>
			</div>	
		</div>
	</body>
	</jato:useViewBean>
</html>