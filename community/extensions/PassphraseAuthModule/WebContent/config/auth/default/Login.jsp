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
<%@page info="Login" language="java"%>
<%@ page contentType="text/html"%>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>
<%@taglib uri="/WEB-INF/auth.tld" prefix="auth"%>
<%@include file="init.jsp"%>
<%@ page import="java.text.SimpleDateFormat"%>

<html xmlns="http://www.w3.org/1999/xhtml">
	<jato:useViewBean className="com.sun.identity.authentication.UI.LoginViewBean">
		<head>
			<title>Login - . Group</title>
			<%
				String ServiceURI = (String) viewBean.getDisplayFieldValue(viewBean.SERVICE_URI);
				String encoded = "false";
				String gotoURL = (String) viewBean.getValidatedInputURL(request.getParameter("goto"), request.getParameter("encoded"), request);
				if ((gotoURL != null) && (gotoURL.length() != 0)) {
					encoded = "true";
				}
				String replaygotoURL = "";
				String goToURL = request.getParameter("goto");
				if (gotoURL != null && !gotoURL.equals("null") && (gotoURL.length() > 0)) {
					replaygotoURL = "&goto=" + goToURL;
				}
				String realm = request.getParameter("realm");
				String queryString = (org.apache.commons.lang.StringUtils.isNotBlank(realm)? "&org="+realm:"");
				
				SimpleDateFormat formatter = new SimpleDateFormat("k");
				int hour = Integer.parseInt(formatter.format(new java.util.Date()));
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
			
			<script language="JavaScript" src="<%=ServiceURI%>/js/browserVersion.js"></script>
			<script language="JavaScript" src="<%=ServiceURI%>/js/auth.js"></script>
			<script language="JavaScript" src="<%=ServiceURI%>/js/cookies.js"></script>
			<script language="JavaScript">
			    writeCSS('<%=ServiceURI%>');
				<jato:content name="validContent">
					//checkCookie();
					var defaultBtn = 'Submit';
					var elmCount = 0;
				
					function defaultSubmit() {
						LoginSubmit(defaultBtn);
					}
				
					function LoginSubmit(value) {
						aggSubmit();
						var hiddenFrm = document.forms['Login'];
				
						if (hiddenFrm != null) {
						hiddenFrm.elements['IDButton'].value = value;
							if (this.submitted) {
								alert("The request is currently being processed");
							}
							else {
								this.submitted = true;
								hiddenFrm.submit();
							}
						}
					}
				
					function ForgotPassword() {
						window.location = "<%=ServiceURI%>/UI/Login?module=PasswordResetModule<%=queryString%>";
					}
					
					function ForgotPassphrase() {
						window.location = "<%=ServiceURI%>/UI/Login?module=PassphraseResetModule<%=queryString%>";
					}
					
				</jato:content>
			</script>
			<script type="text/javascript"><!--// Empty script so IE5.0 Windows will draw table and button borders //-->
			</script>
		</head>
		
		<body onload="placeCursorOnFirstElm();">
			<div id="outer">				
				<div id="container">
					<div id="logininner">					
						<%if(hour >= 0 && hour<=12){%>
						<p class="indented">Good morning, and welcome to .<br/>
						Stage 1 of 2: Please enter your username and password below.</p> 
						<%}else if(hour >=13 && hour<=14){%>
						<p class="indented">Good afternoon, and welcome to .<br/>
						Stage 1 of 2: Please enter your username and password below.</p>
						<%}else if(hour >=15 && hour<=24){%>
						<p class="indented">Good evening, and welcome to .<br/>
						Stage 1 of 2: Please enter your username and password below.</p>
						<%}%>
						<div id="logo"><a href="/"><img src="<%=serviceURL%>/images/logo150.gif" alt=". Logo" /></a></div>																			
							<table border="0" cellspacing="0" cellpadding="0">									
									<jato:content name="validContent">									
										<jato:tiledView name="tiledCallbacks" type="com.sun.identity.authentication.UI.CallBackTiledView">
											<script language="javascript">
												elmCount++;
											</script>																					
											<jato:content name="textBox">											
													<form name="frm<jato:text name="txtIndex" />" action="blank" onSubmit="defaultSubmit(); return false;" method="post">																																																																		
														<input class="indented1" type="text" autocomplete="off" name="IDToken<jato:text name="txtIndex" />" id="IDToken<jato:text name="txtIndex" />" value="" class="textbox">																							
													</form>
											</jato:content>
											<jato:content name="password">														
													<form name="frm<jato:text name="txtIndex" />" action="blank" onSubmit="defaultSubmit(); return false;" method="post">																																																
														<input class="indented2" type="password" name="IDToken<jato:text name="txtIndex" />" id="IDToken<jato:text name="txtIndex" />" value="" class="textbox">																																												
													</form>											
											</jato:content>																					
										</jato:tiledView>
									</jato:content>								
					
									<jato:content name="ContentStaticTextResult">
										<p><b><jato:getDisplayFieldValue name='StaticTextResult' defaultValue='' fireDisplayEvents='true' escape='false' /></b></p>
									</jato:content>
						
									<jato:content name="ContentHref">
										<p><auth:href name="LoginURL" fireDisplayEvents='true'>
											<jato:text name="txtGotoLoginAfterFail" />
										</auth:href></p>
									</jato:content>
						
									<jato:content name="ContentImage">
										<p><img name="IDImage" src="<jato:getDisplayFieldValue name='Image'/>" alt=""></p>
									</jato:content>									
									
									<jato:content name="ContentButtonLogin">
										<jato:content name="hasNoButton">											
											<input class="indented3" type="submit" class="submit" value="Login" onClick="javascript:LoginSubmit('<jato:text name="lblSubmit" />')"/>																														
										</jato:content>
									</jato:content>
							</table>							
							<jato:content name="validContent">
								<auth:form name="Login" method="post" defaultCommandChild="DefaultLoginURL">
									<script language="javascript">
										if (elmCount != null) {
											for (var i = 0; i < elmCount; i++) {
												document.write("<input name=\"IDToken" + i + "\" type=\"hidden\">");
											}
											document.write("<input name=\"IDButton"  + "\" type=\"hidden\">");
										}
										createCookie('isAgreed','false',1);
									</script>
									<input type="hidden" name="encoded" value="<%=encoded%>">
									<%-- commented out to make the portal landin page as success url in all scenarios
									<input type="hidden" name="goto" value="<%=gotoURL%>">
									<input type="hidden" name="plaingoto" value="<%=request.getParameter("goto")%>">
									--%>
									<input type="hidden" name="goto" value="">
									<input type="hidden" name="plaingoto" value="">
									<% if (request.getParameter("realm") != null) { %>
										<input type="hidden" name="realm" value="<%=request.getParameter("realm")%>">
									<% } %>
								</auth:form>
							</jato:content>
							<hr class="loginclear" />
						</div>						
						<p class="loginindented4">
							<a href="javascript:ForgotPassword()">Forgotten Password?</a> <a href="javascript:ForgotPassphrase()">Forgotten Passphrase?</a>
						</p>
					<div id="loginfooter"></div>
				</div>
			</div>
		</body>
	</jato:useViewBean>
</html>
