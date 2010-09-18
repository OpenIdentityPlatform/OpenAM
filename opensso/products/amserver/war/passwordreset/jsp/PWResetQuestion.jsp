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

   $Id: PWResetQuestion.jsp,v 1.5 2008/08/28 06:41:11 mahesh_prasad_r Exp $

--%>


<html>
<%@include file="../ui/PWResetBase.jsp" %>
<%@page info="PWResetQuestion" language="java" pageEncoding="UTF-8"%>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>
<jato:useViewBean className="com.sun.identity.password.ui.PWResetQuestionViewBean" fireChildDisplayEvents="true">

<head>
<title><jato:text name="titleHtmlPage" /></title>
<link rel="stylesheet" href="<%= passwordUrl %>/css/adminstyle.css" />
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

<body class="LogBdy" 
    onLoad="setFocus('PWResetQuestion','PWResetQuestion.passResetTileView[0].tfAnswer');">

  <table border="0" cellpadding="0" cellspacing="0" align="center" title="">
    <tr>
      <td width="50%"><img src="<%= passwordUrl %>/images/dot.gif" width="1" height="1" alt="" /></td>
      <td><img src="<%= passwordUrl %>/images/dot.gif" width="728" height="1" alt="" /></td>
      <td width="50%"><img src="<%= passwordUrl %>/images/dot.gif" width="1" height="1" alt="" /></td>
    </tr>
    <tr class="LogTopBnd" style="background-image: url(<%= passwordUrl %>/images/gradlogtop.jpg); 
    background-repeat: repeat-x; background-position: left top;">
      <td>&nbsp;</td>
      <td><img src="<%= passwordUrl %>/images/dot.gif" width="1" height="30" alt="" /></td>
      <td>&nbsp;</td>
    </tr>
    <tr>
      <td class="LogMidBnd" style="background-image: url(<%= passwordUrl %>/images/gradlogsides.jpg);
        background-repeat:repeat-x;background-position:left top;">&nbsp;</td>
      <td class="LogCntTd" style="background-image: url(<%= passwordUrl %>/images/login-backimage.jpg);
        background-repeat:no-repeat;background-position:left top;" height="435" align="center" valign="middle">
        <table border="0" background="<%= passwordUrl %>/images/dot.gif" cellpadding="0" cellspacing="0" 
        width="100%" title="">
          <tr>
            <td width="260"><img src="<%= passwordUrl %>/images/dot.gif" width="260" height="245" alt="" /></td>
            <td width="415" bgcolor="#ffffff" valign="top"><img name="Login.productLogo" 
            src="<%= passwordUrl %>/images/PrimaryProductName.png" alt="<%= productName %>" 
            border="0" />
              <table border="0" cellspacing="0" cellpadding="0">
                <tr>
                  <td colspan="2">
		      <img src="<%= passwordUrl %>/images/dot.gif" width="1" height="25" alt="" />		    	    
		  </td>
                </tr>            

                <jato:content name="errorBlock">
                <tr>
                  <td>&nbsp;</td>
                  <td><div class="logErr"><table align="center" border="0" cellpadding="0" cellspacing="0" 
                    class="AlrtTbl" title="">
                <tr>
                <td valign="middle">
                <div class="AlrtErrTxt"> 
                <img name="Login.AlertImage" src="<%= passwordUrl %>/images/error_large.gif" alt="Error" 
                height="21" width="21" />
                <jato:text name="errorTitle"/></div>
                <div class="AlrtMsgTxt">
                <jato:text name="errorMsg"/></div>
                </td></tr></table></div></td>
                </tr>
                
                <tr>
                <td>&nbsp;</td>
                </tr>
                </jato:content>

                <jato:content name="infoBlock">
                <tr>
                  <td>&nbsp;</td>
                  <td><div class="logErr"><table align="center" border="0" cellpadding="0" cellspacing="0" 
                    class="AlrtTbl" title="">
                <tr>
                <td valign="middle">
                <div class="AlrtInfTxt"> 
                <img name="Login.AlertImage" src="<%= passwordUrl %>/images/info_large.gif" alt="Info" 
                height="21" width="21" />
                <jato:text name="infoMsg"/></div>
                <div class="AlrtMsgTxt">&nbsp;</div>
                </td></tr></table></div></td>
                </tr>
                
                <tr>
                <td>&nbsp;</td>
                </tr>
                </jato:content>

                <jato:content name ="resetPage">
                <tr>
                <td nowrap="nowrap"></td>
                <td><div class="logTxtSvrNam">                    
                <jato:text name="pwQuestionTitle"/>
                </div></td>
                </tr>

            <jato:form name="PWResetQuestion" method="post" defaultCommandChild="/btnOK">
            <jato:tiledView name="passResetTileView"
                type="com.sun.identity.password.ui.PWResetQuestionTiledView">
            <jato:hidden name="fldAttrName"/>
            <tr>
                <td nowrap="nowrap"><div class="logLbl">
                <span class="LblLev2Txt">
                <label for="PWResetQuestion.tfAnswer">
                        <jato:text name="lblQuestion" />
                </label> 
                </span></div>
                </td>
                <td><div class="logInp">
                <jato:textField name="tfAnswer" size="30" /></div>
                </td>
            </tr>        
            </jato:tiledView>
    
            <tr>
                <td><img src="<%= passwordUrl %>/images/dot.gif" 
                width="1" height="15" alt="" /></td>
                <td>
                <table border=0 cellpadding=0 cellspacing=0>
                <tr>
                <script language="javascript">
		markupButton('<jato:text name="btnPrevious" />',"PWResetQuestion.btnPrevious");		    
                </script> 
                <script language="javascript">
		markupButton('<jato:text name="btnOK" />',"PWResetQuestion.btnOK");		    
                </script>             
                </tr>
                </table>
                </td>
            </tr>

            </jato:form>
            </jato:content>

            <tr>
                <td>&nbsp;</td>
                </tr>
            <tr>
            <td><img src="<%= passwordUrl %>/images/dot.gif" 
            width="1" height="33" alt="" /></td>
	    <td>&nbsp;</td>
            </tr>
        </table>
      </td>
      <td width="45"><img src="<%= passwordUrl %>/images/dot.gif" 
      width="45" height="245" alt="" /></td>
    </tr>
    </table>
    </td>
    <td class="LogMidBnd" style="background-image: url(<%= passwordUrl %>/images/gradlogsides.jpg);
    background-repeat:repeat-x;background-position:left top;">&nbsp;</td>
    </tr>
    <tr class="LogBotBnd" style="background-image: url(<%= passwordUrl %>/images/gradlogbot.jpg);
    background-repeat:repeat-x;background-position:left top;">
      <td>&nbsp;</td>
      <td><div class="logCpy"><span class="logTxtCpy">
        <jato:text name="copyrightText" escape="false"/></span></div>
      </td>
      <td>&nbsp;</td>
    </tr>
  </table>
</body>
</jato:useViewBean>
</html>
