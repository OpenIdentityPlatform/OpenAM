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

   $Id: ImportEntity.jsp,v 1.4 2008/12/18 18:02:36 veiming Exp $
   
--%>

<%@ page info="ImportEntity" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.federation.ImportEntityViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>


<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2007" fireDisplayEvents="true" onLoad="onload()">

<link rel="stylesheet" type="text/css" href="../console/css/openam.css" />
<script language="javascript" src="../console/js/am.js"></script>
<script language="javascript" src="../com_sun_web_ui/js/dynamic.js"></script>
<cc:form name="ImportEntity" method="post" >
    
<%-- HEADER --%>
<script language="javascript">

    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
    }

    function openWindow(fieldName) {
        selectWin = window.open('../federation/FileUploader', fieldName,
            'height=300,width=650,top=' +
            ((screen.height-(screen.height/2))-(500/2)) +
            ',left=' + ((screen.width-650)/2));
        selectWin.focus();
    }

    function metaOptionSelect(radio) {
        var frm = document.forms['ImportEntity'];
        if (radio.value == 'url') {
            frm.elements['ImportEntity.tfMetadataFileURL'].style.display = '';
            frm.elements['ImportEntity.btnMetadata'].style.display = 'none';
            document.getElementById('metadatafilename').style.display = 'none';
        } else {
            frm.elements['ImportEntity.tfMetadataFileURL'].style.display = 'none';
            frm.elements['ImportEntity.btnMetadata'].style.display = '';
            document.getElementById('metadatafilename').style.display = '';
        }
    }

    function extendedOptionSelect(radio) {
        var frm = document.forms['ImportEntity'];
        if (radio.value == 'url') {
            frm.elements['ImportEntity.tfExtendeddataFileURL'].style.display = '';
            frm.elements['ImportEntity.btnExtendeddata'].style.display = 'none';
            document.getElementById('extendeddatafilename').style.display = 'none';
        } else {
            frm.elements['ImportEntity.tfExtendeddataFileURL'].style.display = 'none';
            frm.elements['ImportEntity.btnExtendeddata'].style.display = '';
            document.getElementById('extendeddatafilename').style.display = '';
        }
    }

    function onload() {
        var frm = document.forms['ImportEntity'];
        var rdo = frm.elements['ImportEntity.radioMeta'].item(0);
        if (!rdo.checked) {
            rdo = frm.elements['ImportEntity.radioMeta'].item(1);
        }
        metaOptionSelect(rdo);

        rdo = frm.elements['ImportEntity.radioExtended'].item(0);
        if (!rdo.checked) {
            rdo = frm.elements['ImportEntity.radioExtended'].item(1);
        }
        extendedOptionSelect(rdo);
    }


</script>

<cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" 
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<table border="0" cellpadding="10" cellspacing="0" width="100%">
    <tr>
	<td>
	<cc:alertinline name="ialertCommon" bundleID="amConsole" />
	</td>
    </tr>
</table>

<%-- PAGE CONTENT --%>
<cc:pagetitle name="pgtitle" 
    bundleID="amConsole" 
    pageTitleText="import.entity.title" 
    showPageTitleSeparator="true" 
    viewMenuLabel="" 
    pageTitleHelpMessage="" 
    showPageButtonsTop="true" 
    showPageButtonsBottom="true" >

<table border="0" cellpadding="0" cellspacing="0" title="">
<tr>
<td valign="top" colspan="2"><div class="ConTblCl1Div">
<cc:text name="txtInfo" defaultValue="import.entity.information.message"
    bundleID="amConsole"/>
</div></td></tr>
</table>
    
    <cc:propertysheet 
        name="propertyAttributes" 
        bundleID="amConsole" 
        showJumpLinks="false" />
    
</cc:pagetitle>

</cc:form>
<%-- END CONTENT --%>

</cc:header>

</jato:useViewBean>
