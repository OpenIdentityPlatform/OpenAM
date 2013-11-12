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

   $Id: PMDefaultIPConditionEdit.jsp,v 1.3 2008/06/25 05:44:43 qcheng Exp $

--%>



<%@ page info="PMDefaultIPConditionEdit" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
        className="com.sun.identity.console.policy.PMDefaultIPConditionEditViewBean"
        fireChildDisplayEvents="true" >

    <cc:i18nbundle baseName="amConsole" id="amConsole"
                   locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

    <cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2004" fireDisplayEvents="true">

        <cc:form name="PMDefaultIPConditionEdit" method="post" defaultCommandChild="/btnFilter">
            <script language="javascript">
                function confirmLogout() {
                    return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
                }
                function toggleIPs() {
                    var container = document.getElementById('field_1'),
                            rows = container.getElementsByTagName('tr'),
                            on = [(this.options.selectedIndex*2)+1, (this.options.selectedIndex*2)+2],
                            initiallySelected = this.getAttribute("initiallySelected"),
                            i, j, inputForRow, isOn;

                    for (i=1;i<rows.length;i++) {
                        isOn = on.indexOf(i) !== -1;
                        rows[i].style.display = isOn ? 'table-row' : 'none';
                        inputForRow = rows[i].getElementsByTagName("input");
                        for (j=0;j<inputForRow.length;j++) {
                            if (isOn) {
                                if (parseInt(initiallySelected) !== this.options.selectedIndex) {
                                    inputForRow[j].value = '';
                                }
                                inputForRow[j].removeAttribute("disabled");
                            } else {
                                inputForRow[j].setAttribute("disabled", true);
                            }
                        }
                        inputForRow = rows[i].getElementsByTagName("select");
                        for (j=0;j<inputForRow.length;j++) {
                            if (isOn) {
                                inputForRow[j].removeAttribute("disabled");
                            } else {
                                inputForRow[j].setAttribute("disabled", true);
                            }
                        }
                    }
                }

                document.body.onload = function () {
                    var fields = document.getElementsByClassName("ConFldSetDiv"),
                            idx = document.getElementById("psLbl2").options.selectedIndex;
                    for (i=0;i<fields.length;i++) {
                        fields[i].setAttribute("id", "field_" + i);
                    }
                    document.getElementById("psLbl2").setAttribute("onChange", "toggleIPs.call(this)");
                    document.getElementById("psLbl2").setAttribute("initiallySelected", idx);
                    toggleIPs.call(document.getElementById("psLbl2"));
                }
            </script>
            <cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
            <cc:breadcrumbs name="breadCrumb" bundleID="amConsole" />

            <table border="0" cellpadding="10" cellspacing="0" width="100%">
                <tr>
                    <td>
                        <cc:alertinline name="ialertCommon" bundleID="amConsole" />
                    </td>
                </tr>
            </table>

            <%-- PAGE CONTENT --------------------------------------------------------- --%>
            <cc:pagetitle name="pgtitleTwoBtns" bundleID="amConsole" pageTitleText="page.title.policy.condition.edit" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />

            <cc:propertysheet name="propertyAttributes" bundleID="amConsole" showJumpLinks="false"/>

        </cc:form>

    </cc:header>
</jato:useViewBean>
