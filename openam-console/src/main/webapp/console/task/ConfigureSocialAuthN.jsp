<%--
  ~ The contents of this file are subject to the terms of the Common Development and
  ~ Distribution License (the License). You may not use this file except in compliance with the
  ~ License.
  ~
  ~ You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
  ~ specific language governing permission and limitations under the License.
  ~
  ~ When distributing Covered Software, include this CDDL Header Notice in each file and include
  ~ the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
  ~ Header, with the fields enclosed by brackets [] replaced by your own identifying
  ~ information: "Portions copyright [year] [name of copyright owner]".
  ~
  ~ Copyright 2014 ForgeRock AS.
--%>

<%@page info="ConfigureSocialAuthN" contentType="text/html;charset=UTF-8" language="java" %>
<%@page import="org.owasp.esapi.ESAPI" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<%
    request.setCharacterEncoding("UTF-8");
%>

<jato:useViewBean
    className="com.sun.identity.console.task.ConfigureSocialAuthNViewBean"
    fireChildDisplayEvents="true" >

    <cc:i18nbundle baseName="amConsole" id="amConsole"
                   locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

    <cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2014" fireDisplayEvents="true">

        <link rel="stylesheet" type="text/css" href="../console/css/openam.css" />
        <script language="javascript" src="../console/js/am.js"></script>
        <script language="javascript" src="../com_sun_web_ui/js/dynamic.js"></script>

        <div id="main" style="position: absolute; margin: 0; border: none; padding: 0; width: 100%; height: 101%;">
            <cc:form name="ConfigureSocialAuthN" method="post" defaultCommandChild="/btnSearch">
                <jato:hidden name="szCache" />

                <%-- HEADER --%>
                <script language="javascript">
                    function confirmLogout() {
                        return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
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
                              pageTitleText="page.title.configure.social.authentication"
                              showPageTitleSeparator="false"
                              viewMenuLabel=""
                              pageTitleHelpMessage="page.desc.configure.social.authentication"
                              showPageButtonsTop="true"
                              showPageButtonsBottom="false" >

                    <cc:propertysheet
                        name="propertyAttributes"
                        bundleID="amConsole"
                        showJumpLinks="false" />
                </cc:pagetitle>

            </cc:form>
        </div>
        <div id="dlg" class="dvs"></div>
        <script type="text/javascript">
            <!--
            var ajaxObj = getXmlHttpRequestObject();
            var userLocale = "<%= viewBean.getUserLocale().toString() %>";
            var msgConfiguring = "<p><img src=\"../console/images/processing.gif\" width=\"66\" height\"66\"/></p><cc:text name="txtConfiguring" defaultValue="social.configuration.waiting" bundleID="amConsole" escape="false" />";
            var msgConfigured = "<p>&nbsp;</p><input name=\"btnOK\" type=\"submit\" class=\"Btn1\" value=\"<cc:text name="txtOKBtn" defaultValue="ajax.ok.button" bundleID="amConsole" />\" onClick=\"document.location.replace(\'../task/Home\');return false;\" /></div></p>";
            var closeBtn = "<p>&nbsp;</p><p><div class=\"TtlBtnDiv\"><input name=\"btnClose\" type=\"submit\" class=\"Btn1\" value=\"<cc:text name="txtCloseBtn" defaultValue="ajax.close.button" bundleID="amConsole" />\" onClick=\"focusMain();return false;\" /></div></p>";

            var frm = document.forms['ConfigureSocialAuthN'];
            var btn1 = frm.elements['ConfigureSocialAuthN.button1'];
            btn1.onclick = submitPage;
            var btn2 = frm.elements['ConfigureSocialAuthN.button2'];
            btn2.onclick = cancelOp;
            <%
                String type = request.getParameter("type");
                if (!ESAPI.validator().isValidInput("Social AuthN Type", type, "HTTPParameterValue", 2000, true)) {
                    type = null;
                }
                if (type != null) {
                    out.println("var type = \"" + ESAPI.encoder().encodeForHTML(type) + "\";");
                } else {
                    out.println("var type = null;");
                }
            %>

            function submitPage() {
                fade();
                document.getElementById('dlg').innerHTML = '<center>' + msgConfiguring + '</center>';
                var url = "../console/ajax/AjaxProxy.jsp";
                var params = 'locale=' + userLocale + '&class=com.sun.identity.workflow.ConfigureSocialAuthN' + getData();
                ajaxPost(ajaxObj, url, params, configured);
                return false;
            }

            function cancelOp() {
                document.location.replace("../task/Home");
                return false;
            }

            function getData() {
                var realm = frm.elements['ConfigureSocialAuthN.tfRealm'].value;
                var clientId = frm.elements['ConfigureSocialAuthN.tfClientId'].value;
                var clientSecret = frm.elements['ConfigureSocialAuthN.tfClientSecret'].value;
                var clientSecretConfirm = frm.elements['ConfigureSocialAuthN.tfConfirmSecret'].value;
                var redirectUrl = frm.elements['ConfigureSocialAuthN.tfRedirectUrl'].value;

                var params = "&realm=" + escapeEx(realm)
                    + "&type=" + escapeEx(type)
                    + "&clientId=" + escapeEx(clientId)
                    + "&clientSecret=" + escapeEx(clientSecret)
                    + "&clientSecretConfirm=" + escapeEx(clientSecretConfirm)
                    + "&redirectUrl=" + escapeEx(redirectUrl);

                if (frm.elements['ConfigureSocialAuthN.tfDiscoveryUrl']) {
                    var discoveryUrl = frm.elements['ConfigureSocialAuthN.tfDiscoveryUrl'].value;
                    var imageUrl = frm.elements['ConfigureSocialAuthN.tfImageUrl'].value;
                    var providerName = frm.elements['ConfigureSocialAuthN.tfProviderName'].value;

                    params += "&discoveryUrl=" + escapeEx(discoveryUrl)
                            + "&imageUrl=" + escapeEx(imageUrl)
                            + "&providerName=" + escapeEx(providerName);
                }

                return params;
            }

            function configured() {
                if (ajaxObj.readyState == 4) {
                    var result = hexToString(ajaxObj.responseText);
                    var status = result.substring(0, result.indexOf('|'));
                    var result = result.substring(result.indexOf('|') + 1);
                    var msg = '<center><p>' + result + '</p></center>';
                    if (status == 0) {
                        msg = msg + '<center>' + msgConfigured + '</center>';
                    } else {
                        msg = msg + '<center>' + closeBtn + '</center>';
                    }
                    document.getElementById('dlg').innerHTML = msg;
                }
            }
            -->
        </script>
        <%-- END CONTENT --%>
    </cc:header>
</jato:useViewBean>
