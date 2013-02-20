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

   $Id: PWResetQuestion.jsp,v 1.1.1.1 2008/10/29 08:04:58 svbld Exp $

   "Portions Copyrighted [2012] [Forgerock AS]"

--%>

<%@include file="../ui/PWResetBase.jsp" %>
<%@page info="PWResetQuestion" language="java" pageEncoding="UTF-8"%>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
	<jato:useViewBean className="com.sun.identity.password.ui.PWResetQuestionViewBean" fireChildDisplayEvents="true">
		<head>
			<title>. Portal Memorable Questions</title>
			<link href="../css/portal.css" rel="stylesheet" type="text/css" />
			<script language="JavaScript" src="<%= passwordUrl %>/js/browserVersion.js"></script>
			<script language="JavaScript" src="<%= passwordUrl %>/js/password.js"></script>
			<script language="javascript">
				writeCSS('<%= passwordUrl %>');
				<!-- set the focus for a given field and form -->
				function setFocus(frmName,field) {
					var frm = document.forms[frmName];
					if (frm != null) {
						var elm = frm.elements[field];
						if (elm != null) {
							elm.focus();
							elm.select();
						}
					}
				}
			</script>
		</head>
		<body onLoad="setFocus('PWResetQuestion','PWResetQuestion.passResetTileView[0].tfAnswer');">
			<div id="box">
				<img src="../images/brand-logo.gif" alt="." />
				<div id="innerbox">
					<jato:content name="errorBlock">
						<div class="logErr"><div class="AlrtMsgTxt"><jato:text name="errorMsg"/></div></div>
					</jato:content>
					<jato:content name="infoBlock">
						<div class="logErr"><div class="AlrtMsgTxt"><jato:text name="infoMsg"/></div></div>
					</jato:content>
					<div class="width20em">
						<%int index = 0;%>
						<jato:content name ="resetPage">
							<jato:form name="PWResetQuestion" method="post" defaultCommandChild="/btnOK">
								<jato:tiledView name="passResetTileView" type="com.sun.identity.password.ui.PWResetQuestionTiledView">
									<jato:hidden name="fldAttrName"/>
									<div class="row">
										<label for="PWResetQuestion.tfAnswer"><jato:text name="lblQuestion" /></label> 
										<div><input name="PWResetQuestion.passResetTileView[<%=index++%>].tfAnswer" type="text" autocomplete="off" size="30" value=""></div>
									</div>
								</jato:tiledView>
								<div class="row buttons">
									<input type="button" class="button" value="Previous" onClick="javascript: document.PWResetQuestionPrev.submit();" />
									<input type="submit" class="button" value="Ok" />
								</div>
							</jato:form>
							<jato:form name="PWResetQuestionPrev" method="post" defaultCommandChild="/btnPrevious">
							</jato:form>
						</jato:content>
						<jato:content name="infoBlock">
							<div style="margin-top: 50px; margin-left: 180px;">
								<a href="../">Return to Login page</a>
							</div>
						</jato:content>
					</div>
				</div>
			</div>
		</body>
	</jato:useViewBean>
</html>