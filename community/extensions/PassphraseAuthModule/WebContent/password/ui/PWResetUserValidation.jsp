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

   $Id: PWResetUserValidation.jsp,v 1.1.1.1 2008/10/29 08:04:58 svbld Exp $

   "Portions Copyrighted [2012] [Forgerock AS]"

--%>


<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">

<%@include file="../ui/PWResetBase.jsp" %>
<%@page info="PWResetUserValidation" language="java" pageEncoding="UTF-8"%>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>
<jato:useViewBean className="com.sun.identity.password.ui.PWResetUserValidationViewBean" fireChildDisplayEvents="true">

<head>
	<title>Password Reset User Validation</title>
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

<body onLoad="setFocus('PWResetUserValidation','PWResetUserValidation.tfUserAttr');">
<div id="box">
	<img src="../images/brand-logo.gif" alt="." />
	<div id="innerbox">
		<jato:content name ="resetPage">
			<jato:form name="PWResetUserValidation" method="post" defaultCommandChild="/btnNext">
				<jato:hidden name="fldUserAttr"/>
				<div class="indent">
					<span>Password Reset User Validation</span>
				</div>
				<div class="indent">
					<span><jato:content name="errorBlock"><div style="color: red;"><jato:text name="errorMsg"/></div></jato:content></span>
				</div>
				<div class="indent">
					<span><jato:content name="infoBlock"><div class="AlrtInfTxt"><jato:text name="infoMsg"/></div></jato:content></span>
				</div>
				<div class="row">
					<label for="userid">User Name:</label>
					<div><jato:textField name="tfUserAttr" size="30" /></div>
				</div>
				
				<div class="row buttons">
					<input type="submit" name="btnNext" id="btnNext" class="button" value="Next" />
				</div>
			</jato:form>
        </jato:content>
	</div>
</div>
</body>
</jato:useViewBean>
</html>
