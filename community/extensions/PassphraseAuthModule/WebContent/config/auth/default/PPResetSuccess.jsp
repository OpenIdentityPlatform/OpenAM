<%--
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [2012] [Forgerock AS]"
 */
--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ page contentType="text/html" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>
<%@taglib uri="/WEB-INF/auth.tld" prefix="auth"%>
<%@include file="init.jsp"%>

<html xmlns="http://www.w3.org/1999/xhtml">
	<jato:useViewBean className="com.sun.identity.authentication.UI.LoginViewBean">
		<head>
			<title>Passphrase Reset Confirmation</title>
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
			<script type="text/javascript"><!--// Empty script so IE5.0 Windows will draw table and button borders//--></script>
		</head>
		<body>			
			<div id="outer">
				<div id="container">
					<div id="inner">
						<div id="logo"><a href="/"><img src="<%=serviceURL%>/images/logo150.gif" alt=". Logo" /></a></div>							
						<div class="indented">
							<span id="title" style="display:none">Passphrase Reset Confirmation</span>
							<jato:content name="ContentStaticWarning">
								<span id="body" style="color:red">
									<jato:getDisplayFieldValue name='StaticTextWarning' escape='false'/>
								</span><br/>
							</jato:content>
							<jato:content name="ContentStaticTextHeader">
								<span id="body" style="padding: 0pt 5px;">
									<jato:getDisplayFieldValue name='StaticTextHeader' escape='false' />
								</span><br/>
							</jato:content>
							<span id="body"><a href="<%= ServiceURI %>/UI/Login"><jato:text name="txtGotoLoginAfterFail" /></a></span>
						</div>
						<script>
							if (document.getElementById('body').innerHTML.indexOf("color=\"red\"") == -1)
								document.getElementById('title').style.display = 'block';
						</script>
					</div>
				</div>
				<div id="footer"></div>		
			</div>			
		</body>
	</jato:useViewBean>
</html>
