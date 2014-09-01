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
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>
<%@taglib uri="/WEB-INF/auth.tld" prefix="auth"%>
<%@include file="init.jsp"%>

<html>
	<jato:useViewBean className="com.sun.identity.authentication.UI.LoginViewBean">
		<%@ page contentType="text/html"%>
		<head>
			<title>&nbsp;</title>
			<%
				String ServiceURI = (String) viewBean.getDisplayFieldValue(viewBean.SERVICE_URI);
				String encoded = "false";
				String gotoURL = (String) viewBean.getValidatedInputURL(request.getParameter("goto"), request.getParameter("encoded"), request);
				if ((gotoURL != null) && (gotoURL.length() != 0)) {
				    encoded = "true";
				}
			%>			
			<link rel="stylesheet" href="<%= serviceURL %>/css/main.css" type="text/css" />
			<!--[if lt IE 8]>
			<link rel="stylesheet" type="text/css" href="<%=serviceURL%>/css/ielt8.css" />
			<![endif]-->
			<!--[if IE 7]>
			<link rel="stylesheet" type="text/css" href="<%=serviceURL%>/css/ie7.css" />
			<![endif]-->
			<!--[if IE 6]>
			<link rel="stylesheet" type="text/css" href="<%=serviceURL%>/css/ie6.css" />
			<![endif]-->
			<script language="JavaScript">
				writeCSS('<%= ServiceURI %>');
				<jato:content name="validContent">
				    function defaultSubmit() {
				        var hiddenFrm = document.forms['Login'];
				        if (hiddenFrm != null) {
					    hiddenFrm.elements['IDButton'].value = 'Submit';
				               hiddenFrm.submit();
				        }
				    }
				</jato:content>
			</script>
		</head>
		<body onload="defaultSubmit()" style="background-color:#0404B4">			
				<div id="outer">
					<div id="container">
						<div id="inner">
							<jato:content name="validContent">
								<auth:form name="Login" method="post" defaultCommandChild="DefaultLoginURL">
									<input name="IDButton" type="hidden">
									<input type="hidden" name="goto" value="<%= gotoURL %>">
									<input type="hidden" name="encoded" value="<%= encoded %>">
									<input type="hidden" name="realm" value="<%=request.getParameter("realm")%>">
									<input type="hidden" name="state" value="1">
								</auth:form>
							</jato:content>
						</div>
					</div>
				</div>				
		</body>
	</jato:useViewBean>
</html>