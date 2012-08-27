<!--
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2011 ForgeRock AS. All Rights Reserved
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
* "Portions Copyrighted [year] [name of copyright owner]"
*
-->




<%@ page info="SessionHAProperties" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.session.SessionHAPropertiesViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

<cc:header name="hdrCommon"
    pageTitle="webconsole.title" 
    bundleID="amConsole" 
    copyrightYear="2012"
    fireDisplayEvents="true">

<script language="javascript" src="../console/js/am.js"></script>
    
<cc:form name="SessionHAProperties" method="post" defaultCommandChild="/button1">
<cc:hidden name="tfName" />
<jato:hidden name="szCache" />

<script language="javascript">
    function confirmLogout() {
        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
    }
</script>
<cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();"
                    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
<cc:breadcrumbs name="breadCrumb" bundleID="amConsole" />
<cc:tabs name="tabCommon" bundleID="amConsole" />

<table border="0" cellpadding="10" cellspacing="0" width="100%">
    <tr>
	<td>
	<cc:alertinline name="ialertCommon" bundleID="amConsole" />
	</td>
    </tr>
</table>

<%-- PAGE CONTENT --%>
<cc:pagetitle 
    name="pgtitle" 
    bundleID="amConsole" 
    pageTitleText="page.title.sessionha.properties"
    showPageTitleSeparator="true" 
    viewMenuLabel="" 
    pageTitleHelpMessage="" 
    showPageButtonsTop="true" 
    showPageButtonsBottom="false" />

    <table>
        <thead>
            <th>Session HA Property Name</th>
            <th>Current Runtime Value</th>
        </thead>
        <tr>
            <td align="right">
                <cc:textfield name="iplanet-am-session-sfo-enabled.LABEL" readOnly="true" size="64" />
            </td>
            <td align="right">
                <cc:textfield name="iplanet-am-session-sfo-enabled"
                              readOnly="true" defaultValue="false" size="64"
                              title="iplanet-am-session-sfo-enabled"  />
            </td>
        </tr>
        <tr>
            <td align="right">
                <cc:textfield name="org.forgerock.openam.session.ha.amsessionrepository.type.LABEL"
                              readOnly="true" size="64" />
            </td>
            <td align="right">
                <cc:textfield name="org.forgerock.openam.session.ha.amsessionrepository.type"
                              readOnly="true" size="64"
                              title="org.forgerock.openam.session.ha.amsessionrepository.type" />
            </td>
        </tr>
        <tr>
            <td align="right">
                <cc:textfield name="com.sun.am.session.SessionRepositoryImpl.LABEL"
                              readOnly="true" size="64" />
            </td>
            <td align="right">
                <cc:textfield name="com.sun.am.session.SessionRepositoryImpl"
                              readOnly="true" size="64"
                              title="com.sun.am.session.SessionRepositoryImpl" />
            </td>
        </tr>
        <tr>
            <td align="right">
                <cc:textfield name="org.forgerock.openam.session.ha.amsessionrepository.rootdn.LABEL"
                              readOnly="true" size="64" />
            </td>
            <td align="right">
                <cc:textfield name="org.forgerock.openam.session.ha.amsessionrepository.rootdn"
                              readOnly="true" size="64"
                              title="org.forgerock.openam.session.ha.amsessionrepository.rootdn" />
            </td>
        </tr>

        <tr>
            <td align="right">
                <cc:textfield name="com.iplanet.am.session.failover.useRemoteSaveMethod.LABEL"
                              readOnly="true" size="64" />
            </td>
            <td align="right">
                <cc:textfield name="com.iplanet.am.session.failover.useRemoteSaveMethod"
                              readOnly="true" size="64"
                              title="com.iplanet.am.session.failover.useRemoteSaveMethod" />
            </td>
        </tr>
        <tr>
            <td align="right">
                <cc:textfield name="com.iplanet.am.session.failover.useInternalRequestRouting.LABEL"
                              readOnly="true" size="64" />
            </td>
            <td align="right">
                <cc:textfield name="com.iplanet.am.session.failover.useInternalRequestRouting"
                              readOnly="true" size="64"
                              title="com.iplanet.am.session.failover.useInternalRequestRouting" />
            </td>
        </tr>
        <tr>
            <td align="right">
                <cc:textfield name="com.iplanet.am.session.failover.cluster.stateCheck.timeout.LABEL"
                              readOnly="true" size="64" />
            </td>
            <td align="right">
                <cc:textfield name="com.iplanet.am.session.failover.cluster.stateCheck.timeout"
                              readOnly="true" size="64"
                              title="com.iplanet.am.session.failover.cluster.stateCheck.timeout" />
            </td>
        </tr>
        <tr>
            <td align="right">
                <cc:textfield name="com.iplanet.am.session.failover.cluster.stateCheck.period.LABEL"
                              readOnly="true" size="64" />
            </td>
            <td align="right">
                <cc:textfield name="com.iplanet.am.session.failover.cluster.stateCheck.period"
                              readOnly="true" size="64"
                              title="com.iplanet.am.session.failover.cluster.stateCheck.period" />
            </td>
        </tr>

        <!-- TODO Show Additional Properties -->

    </table>


</cc:form>

</cc:header>
</jato:useViewBean>
