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

<%@ page info="SessionHAStatistics" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.session.SessionHAStatisticsViewBean"
    fireChildDisplayEvents="true" >

<cc:i18nbundle baseName="amConsole" id="amConsole"
    locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

    <cc:header name="hdrCommon"
    pageTitle="webconsole.title" 
    bundleID="amConsole" 
    copyrightYear="2012"
    fireDisplayEvents="true">

        <style type="text/css">
            #watermark {
                color: #d0d0d0;
                font-size: 200pt;
                -webkit-transform: rotate(-45deg);
                -moz-transform: rotate(-45deg);
                position: absolute;
                width: 100%;
                height: 100%;
                margin: 0;
                z-index: -1;
                left: -100px;
                top: -200px;
            }
        </style>


        <script language="javascript" src="../console/js/am.js"></script>

        <script language="javascript" src="/openam/js/Bluff-0.3.6.2/js-class.js" type="text/javascript"></script>
        <script language="javascript" src="/openam/js/Bluff-0.3.6.2/bluff-min.js" type="text/javascript"></script>
        <!--[if IE]><script language="javascript" src="/openam/js/Bluff-0.3.6.2/excanvas.js" type="text/javascript"></script><![endif]-->

        <!-- BLUFF -->
        <script type="text/javascript">
            var counts = {
                items: [
                    {label: 'Active Sessions', data: 1},
                    {label: 'Replicated Sessions', data: 1}]
            };

            var datapiea = {
                items: [
                    {label: 'Reads', data: 999999},
                    {label: 'Writes', data: 999999}]
            };

            // BLUFF
            window.onload = function () {

                var bluffGraph1 = new Bluff.Bar('graph1', 450);
                bluffGraph1.theme_keynote();
                bluffGraph1.title = 'Live Session Counts';

                for (i in counts.items) {
                    var item = counts.items[i];
                    //Add each data item to bar
                    bluffGraph1.data(item.label, item.data);
                }
                bluffGraph1.draw();


                var bluffGraph2 = new Bluff.Bar('graph2', 450);
                bluffGraph2.theme_keynote();
                bluffGraph2.title = 'Session Replication';

                for (i in datapiea.items) {
                    var item = datapiea.items[i];
                    //Add each data item to pie
                    bluffGraph2.data(item.label, item.data);
                }
                bluffGraph2.draw();
            }

</script>

<cc:form name="SessionHAStatistics" method="post" defaultCommandChild="/button1">
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
    pageTitleText="page.title.sessionha.statistics"
    showPageTitleSeparator="true" 
    viewMenuLabel="" 
    pageTitleHelpMessage="" 
    showPageButtonsTop="true" 
    showPageButtonsBottom="false" />

    <table>
        <tr>
            <td bgcolor='#EEE8CD' colspan='2'>
                <h3>OpenAM Session Persistence Status</h3>
                <b>Ok</b>
            </td>
        </tr>

        <tr>
            <td valign="center">
                <canvas id="graph1"></canvas>
            </td>
            <td valign="center">
                <canvas id="graph2"></canvas>
            </td>
        </tr>

    </table>


</cc:form>

</cc:header>
</jato:useViewBean>
