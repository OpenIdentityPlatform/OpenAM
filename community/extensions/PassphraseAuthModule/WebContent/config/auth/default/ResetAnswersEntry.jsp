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
<%@page info="Password/Passsphrase Reset Options" language="java"%>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>
<%@taglib uri="/WEB-INF/auth.tld" prefix="auth"%>
<%@include file="init.jsp"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
	<jato:useViewBean className="com.sun.identity.authentication.UI.LoginViewBean">
		<%@ page contentType="text/html"%>
		<head>
			<title>. Portal Memorable Questions</title>
			<%
				String ServiceURI = (String) viewBean.getDisplayFieldValue(viewBean.SERVICE_URI);
				String encoded = "false";
				String gotoURL = (String) viewBean.getValidatedInputURL(request.getParameter("goto"), request.getParameter("encoded"), request);
				if ((gotoURL != null) && (gotoURL.length() != 0)) {
				    encoded = "true";
				}
				int index = 1;
			%>
		
			<link rel="stylesheet" href="<%= serviceURL %>/css/portal.css" type="text/css" />
			<script language="JavaScript" src="<%= ServiceURI %>/js/browserVersion.js"></script>
			<script language="JavaScript" src="<%= ServiceURI %>/js/auth.js"></script>
		
			<script language="JavaScript">
				writeCSS('<%= ServiceURI %>');
				<jato:content name="validContent">
				    var defaultBtn = 'Submit';
				    var elmCount = 0;
				
				    /** submit form with default command button */
				    function defaultSubmit() {
				        LoginSubmit(defaultBtn);
				    }
				
				    /**
				     * submit form with given button value
				     *
				     * @param value of button
				     */
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
				</jato:content>
			</script>
			<script type="text/javascript"><!--// Empty script so IE5.0 Windows will draw table and button borders //--></script>
		</head>
	
		<body onload="placeCursorOnFirstElm();">
		<div id="box">
			<img src="<%=serviceURL%>/images/brand-logo.gif" alt="." />
			<div id="innerbox">
		        <!-- display error message -->
				<jato:content name="ContentStaticTextHeader">
					<div style="color:red;text-align:center">
						<jato:getDisplayFieldValue name='StaticTextHeader' defaultValue='Authentication' fireDisplayEvents='true' escape='false' />
						<br/><br/>
					</div>
				</jato:content>
				<jato:content name="validContent">
					<jato:tiledView name="tiledCallbacks" type="com.sun.identity.authentication.UI.CallBackTiledView">
						<script language="javascript">
							elmCount++;
						</script>
						<jato:content name="textBox">
							<form name="frm<jato:text name="txtIndex" />" action="blank" onSubmit="defaultSubmit(); return false;" method="post" class="width20em">
								<div class="row">
									<label for="writer"><jato:text name="txtPrompt" defaultValue="User name:" escape="false" /></label>
									<% String param = "6".equals(request.getParameter("state"))? request.getParameter("IDToken"+index++):""; %>
									<input type="text" autocomplete="off" name="IDToken<jato:text name="txtIndex" />" id="IDToken<jato:text name="txtIndex" />" class="textbox" value="<%=param%>">
								</div>
							</form>
						</jato:content>
						<jato:content name="choice">
							<form name="frm<jato:text name="txtIndex" />" action="blank" onSubmit="defaultSubmit(); return false;" method="post">
								<jato:tiledView name="tiledChoices" type="com.sun.identity.authentication.UI.CallBackChoiceTiledView">
									<jato:content name="selectedChoice">
										<input type="hidden" name="IDToken<jato:text name="txtParentIndex" />" id="IDToken<jato:text name="txtParentIndex" />" value="5">
									</jato:content>
								</jato:tiledView>
							</form>
						</jato:content>
					</jato:tiledView>
				</jato:content>
				
				<jato:content name="ContentStaticTextResult">
					<!-- after login output message -->
					<p><b><jato:getDisplayFieldValue name='StaticTextResult' defaultValue='' fireDisplayEvents='true' escape='false' /></b></p>
				</jato:content>

				<jato:content name="ContentHref">
					<!-- URL back to Login page -->
					<p>
						<auth:href name="LoginURL" fireDisplayEvents='true'>
							<jato:text name="txtGotoLoginAfterFail" />
						</auth:href>
					</p>
				</jato:content>

				<jato:content name="ContentImage">
					<!-- customized image defined in properties file -->
					<p><img name="IDImage" src="<jato:getDisplayFieldValue name='Image'/>" alt=""></p>
				</jato:content>

				<jato:content name="ContentButtonLogin">
					<jato:content name="hasButton">
						<script language="javascript">
							defaultBtn = '<jato:text name="defaultBtn" />';
						</script>
						<tr>
							<td><img src="<%= serviceURL %>/images/dot.gif" width="1" height="15" alt="" /></td>
							<td>
								<table border=0 cellpadding=0 cellspacing=0>
									<tr>
										<jato:tiledView name="tiledButtons" type="com.sun.identity.authentication.UI.ButtonTiledView">
											<script language="javascript">
												markupButton(
													'<jato:text name="txtButton" />',
													"javascript:LoginSubmit('<jato:text name="txtButton" />')");
											</script>
										</jato:tiledView>
									</tr>
								</table>
							</td>
						</tr>
					</jato:content>

					<jato:content name="hasNoButton">
						<div class="width20em">
							<div class="row buttons">
								<input type="submit" class="button" value="Submit" onClick="javascript:LoginSubmit('<jato:text name="lblSubmit" />')"/>
							</div>
						</div>
						<!-- end of hasNoButton -->
					</jato:content>
				</jato:content>
											
				<jato:content name="validContent">
					<auth:form name="Login" method="post" defaultCommandChild="DefaultLoginURL">
			
						<script language="javascript">
							if (elmCount != null) {
								for (var i = 0; i < elmCount; i++) {
									document.write("<input name=\"IDToken" + i + "\" type=\"hidden\">");
								}
								document.write("<input name=\"IDButton"  + "\" type=\"hidden\">");
							}
						</script>
						<input type="hidden" name="IDToken4" value="15">
						<input type="hidden" name="goto" value="<%= gotoURL %>">
						<input type="hidden" name="encoded" value="<%= encoded %>">
						<input type="hidden" name="state" value="6">
					</auth:form>
				</jato:content>
				</br>
				<div>
					Note:</br>
					Please provide answers to the security challenge questions.</br>
					These will be used to verify your identity in the event of you needing to reset your password or passphrase
					</br>
				</div>
			</div>
		</body>
	</jato:useViewBean>
</html>