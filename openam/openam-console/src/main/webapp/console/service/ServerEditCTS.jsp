<%--
    The contents of this file are subject to the terms of the Common Development and
    Distribution License (the License). You may not use this file except in compliance with the
    License.

    You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
    specific language governing permission and limitations under the License.

    When distributing Covered Software, include this CDDL Header Notice in each file and include
    the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
    Header, with the fields enclosed by brackets [] replaced by your own identifying
    information: "Portions copyright [year] [name of copyright owner]".

    Copyright 2013 ForgeRock AS.
--%>

<%@ page info="ServerEditCTS" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean className="com.sun.identity.console.service.ServerEditCTSViewBean" fireChildDisplayEvents="true" >
    <cc:i18nbundle baseName="amConsole" id="amConsole" locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
    <cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2007" fireDisplayEvents="true">
        <script language="javascript">
            // Field names.
            var storeLocationRadioButtonName = 'ServerEditCTS.cscorg-forgerock-services-cts-store-location';
            var sslEnableCheckBoxName = 'ServerEditCTS.cscorg-forgerock-services-cts-store-ssl-enabled';
            var directoryNameFieldName = 'ServerEditCTS.cscorg-forgerock-services-cts-store-directory-name';
            var portFieldName = 'ServerEditCTS.cscorg-forgerock-services-cts-store-port';
            var loginIdFieldName = 'ServerEditCTS.cscorg-forgerock-services-cts-store-loginid';
            var passwordFieldName = 'ServerEditCTS.cscorg-forgerock-services-cts-store-password';
            var maxConnectionsFieldName = 'ServerEditCTS.cscorg-forgerock-services-cts-store-max-connections';
            var heartbeat = 'ServerEditCTS.cscorg-forgerock-services-cts-store-heartbeat';

            window.onload = function() {
                // Set the initial state of the fields.
                var radioBtns = document.getElementsByName(storeLocationRadioButtonName);

                if (radioBtns.length != 2) {
                    // Do nothing, there must be two radio buttons.
                    return;
                }

                toggleExternalConfig((radioBtns[0].checked) ? radioBtns[0] : radioBtns[1]);
            }

            // Retrieves the first element of the given name.
            function getFirstElementByName(name) {
                var elements = document.getElementsByName(name);
                return (elements.length > 0) ? elements[0] : null;
            }

            // Toggles the status of the external configuration fields.
            function toggleExternalConfig(storeLocationRadioButton) {
                var readonly = storeLocationRadioButton.value == 'default';
                toggleField(sslEnableCheckBoxName, readonly);
                toggleField(directoryNameFieldName, readonly);
                toggleField(portFieldName, readonly);
                toggleField(loginIdFieldName, readonly);
                toggleField(passwordFieldName, readonly);
                toggleField(maxConnectionsFieldName, readonly);
                toggleField(heartbeat, readonly);
            }

            // Toggles the status of a given field.
            function toggleField(fieldName, readonly) {
                var field = getFirstElementByName(fieldName);

                if (field != null) {
                    if (readonly) {
                        field.setAttribute('readonly', 'readonly');
                        field.className = 'TxtFldDis';
                    } else {
                        field.removeAttribute('readonly');
                        field.className = 'TxtFld';
                    }
                }
            }
        </script>

        <cc:form name="ServerEditCTS" method="post" defaultCommandChild="/button1">
            <script language="javascript">
                function confirmLogout() {
                    return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
                }
            </script>
            <cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
            <cc:breadcrumbs name="breadCrumb" bundleID="amConsole" />
            <cc:tabs name="tabCommon" bundleID="amConsole" />

            <table border="0" cellpadding="10" cellspacing="0" width="100%">
                <tr>
                    <td>
                        <cc:alertinline name="ialertCommon" bundleID="amConsole" />
                    </td>
                </tr>
            </table>

            <%-- PAGE CONTENT --------------------------------------------------------- --%>
            <cc:pagetitle name="pgtitleThreeBtns" bundleID="amConsole" pageTitleText="page.title.server.edit" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />

            <table border="0" cellpadding="10" cellspacing="0" width="100%">
                <tr><td>
                    <cc:button name="btnInherit" bundleID="amConsole" defaultValue="serverconfig.button.inherit" type="primary" />
                </td></tr>
            </table>

            <cc:propertysheet name="propertyAttributes" bundleID="amConsole" showJumpLinks="true"/>

        </cc:form>

    </cc:header>
</jato:useViewBean>
