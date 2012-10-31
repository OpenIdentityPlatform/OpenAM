<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: SelectRealm.jsp,v 1.4 2009/12/08 06:19:05 si224302 Exp $

--%>




<%@ page info="SelectRealm" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.policy.SelectRealmViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2004" fireDisplayEvents="true">

<script language="javascript">
    function realmChanged(frm) {
	frm.action += '?SelectRealm.btnRealm=';
	frm.submit();
    }

    function updateValue() {
	var frm = document.forms[0];
	var realm = frm.elements[frm.name  + '.RealmNames'].value;
	var scheme = frm.elements[frm.name  + '.Schemes'].value;
        var value = '';
        var label = '';

        if (realm == '') {
            label = scheme;
            value = scheme;
        } else {
            var realms = frm.elements[frm.name + '.RealmNames'];
            for (x=0; x < realms.length; x++) {
                if (realms.options[x].selected == true) {
                    var realmLabel = realms.options[x].text;
                }
            }    
            label = realmLabel + ':' + scheme;
            value = realm + ':' + scheme;
        }

	var parentFrm = opener.document.forms[0];
	var sl = parentFrm.elements[parentFrm.name + '.AuthScheme'];
        var newOpt = window.opener.addOption(label, value);
        sl.options[sl.options.length] = newOpt; 
	top.window.close();
    }
</script>

<cc:form name="SelectRealm" method="post" defaultCommandChild="/button1">
<cc:secondarymasthead name="secMhCommon" />

<table border="0" cellpadding="10" cellspacing="0" width="100%">
    <tr>
	<td>
	<cc:alertinline name="ialertCommon" bundleID="amConsole" />
	</td>
    </tr>
</table>

<%-- PAGE CONTENT --------------------------------------------------------- --%>
<cc:pagetitle name="pgtitleTwoBtns" bundleID="amConsole" pageTitleText="policy.condition.title.selectrealm" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />

<cc:propertysheet name="propertyAttributes" bundleID="amConsole" showJumpLinks="true"/>


</cc:form>

</cc:header>
</jato:useViewBean>
