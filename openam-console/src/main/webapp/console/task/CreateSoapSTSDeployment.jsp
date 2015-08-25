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
  ~ Copyright 2015 ForgeRock AS.
--%>

<%@ page info="CreateSoapSTSDeployment" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<%@taglib tagdir="/WEB-INF/tags" prefix="console"%>
<%
  request.setCharacterEncoding("UTF-8");
%>
<jato:useViewBean
        className="com.sun.identity.console.task.CreateSoapSTSDeploymentViewBean"
        fireChildDisplayEvents="true" >

  <cc:i18nbundle baseName="amConsole" id="amConsole"
                 locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>

  <cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2015" fireDisplayEvents="true">

    <link rel="stylesheet" type="text/css" href="../console/css/openam.css" />
    <script language="javascript" src="../console/js/am.js"></script>
    <script language="javascript" src="../console/js/tasksPage.js"></script>
    <script language="javascript" src="../com_sun_web_ui/js/dynamic.js"></script>

    <div id="main" style="position: absolute; margin: 0; border: none; padding: 0; width:auto; height:101%;">
    <div id="divhelp" style="display: none; position:absolute; margin: 0; border: 1px solid #AABCC8; padding: 0; width:400px; height:200px; background:#FCFCFC">
      <table border=0 cellpadding=2 cellspacing=0 width="100%">
        <tr><td width=99%><span id="divHelpmsg" /></td>
          <td width="1%" valign="top">
            <img src="../console/images/tasks/close.gif" width="16" height="16" onClick="hideHelp()" />
          </td>
        </tr>
      </table>
    </div>

    <cc:form name="CreateSoapSTSDeployment" method="post">
      <jato:hidden name="szCache" />
      <script language="javascript">
        function confirmLogout() {
          return confirm("<cc:text name="txtLogout" defaultValue="masthead.logoutMessage" bundleID="amConsole"/>");
        }

        function cancelOp() {
          redirectToXui();
          return false;
        }

      </script>

      <cc:primarymasthead name="mhCommon" bundleID="amConsole"  logoutOnClick="return confirmLogout();" locale="<%=((com.sun.identity.console.base.AMViewBeanBase)viewBean).getUserLocale()%>"/>
      <table border="0" cellpadding="10" cellspacing="0" width="100%">
        <tr>
          <td>
            <cc:alertinline name="ialertCommon" bundleID="amConsole" />
          </td>
        </tr>
      </table>

      <%-- PAGE CONTENT --------------------------------------------------------- --%>
      <cc:pagetitle name="pgtitle" bundleID="amConsole" pageTitleText="page.title.configure.soapstsdeployment" showPageTitleSeparator="true" viewMenuLabel="" pageTitleHelpMessage="" showPageButtonsTop="true" showPageButtonsBottom="false" />

      <table border="0" cellpadding="20" cellspacing="0">
        <tr><td>
          <cc:text name="txtDesc" defaultValue="page.desc.configure.soapstsdeployment" bundleID="amConsole" />
        </td></tr>
      </table>


      <cc:propertysheet name="propertyAttributes" bundleID="amConsole" showJumpLinks="false"/>

    </cc:form>
  </cc:header>
  </div>
  <div id="dlg" class="dvs"></div>

  <console:redirectToXui realm="encodeURIComponent(frm.elements['CreateSoapSTSDeployment.tfRealm'].value)"
                         xuiPath="../XUI#realms/{realm}/dashboard"/>

  <script language="javascript">
    var msgCreating = "<p><img src=\"../console/images/processing.gif\" width=\"66\" height\"66\"/></p><cc:text name="txtConfiguring" defaultValue="configuring.soapstsdeployment.waiting" bundleID="amConsole" escape="false" />";
    var ttlCreated = "<h3><cc:text name="txtTtlCreated" defaultValue="soapstsdeployment.configured.title" escape="false" bundleID="amConsole" /></h3>";
    var msgCreated = "<p>&nbsp;</p><input name=\"btnOK\" type=\"submit\" class=\"Btn1\" value=\"<cc:text name="txtOKBtn" defaultValue="ajax.ok.button" bundleID="amConsole" />\" onClick=\"redirectToXui();return false;\" /></div></p>";
    var closeBtn = "<p>&nbsp;</p><p><div class=\"TtlBtnDiv\"><input name=\"btnClose\" type=\"submit\" class=\"Btn1\" value=\"<cc:text name="txtCloseBtn" defaultValue="ajax.close.button" bundleID="amConsole" />\" onClick=\"focusMain();return false;\" /></div></p>";

    var frm = document.forms['CreateSoapSTSDeployment'];
    var btn1 = frm.elements['CreateSoapSTSDeployment.button1'];
    btn1.onclick = submitPage;
    var btn2 = frm.elements['CreateSoapSTSDeployment.button2'];
    btn2.onclick = cancelOp;
    var ajaxObj = getXmlHttpRequestObject();
    var userLocale = "<%= viewBean.getUserLocale().toString() %>";

    function getData() {
      var realm = frm.elements['CreateSoapSTSDeployment.tfRealm'].value;
      var openAMUrl = frm.elements['CreateSoapSTSDeployment.tfOpenAMUrl'].value;
      var soapAgentName = frm.elements['CreateSoapSTSDeployment.tfSoapAgentName'].value;
      var soapAgentPassword = frm.elements['CreateSoapSTSDeployment.tfSoapAgentPassword'].value;

      var keystoreFileNamesSize = frm.elements['CreateSoapSTSDeployment.elKeystoreFileNames.listbox'].length;
      var i=0;
      var keystoreFileNames="";
      for (i=0;i<keystoreFileNamesSize-1;i++)
      {
        keystoreFileNames+=frm.elements['CreateSoapSTSDeployment.elKeystoreFileNames.listbox'].options[i].value + ",";
      }

      var wsdlFileNamesSize = frm.elements['CreateSoapSTSDeployment.elCustomWsdlFileNames.listbox'].length;
      var wsdlFileNames="";
      for (i=0;i<wsdlFileNamesSize-1;i++)
      {
        wsdlFileNames+=frm.elements['CreateSoapSTSDeployment.elCustomWsdlFileNames.listbox'].options[i].value + ",";
      }

      var params = "&realm=" + escapeEx(realm)
              + "&openAMUrl=" + escapeEx(openAMUrl)
              + "&soapAgentName=" + escapeEx(soapAgentName)
              + "&soapAgentPassword=" + escapeEx(soapAgentPassword)
              + "&keystoreFileNames=" + escapeEx(keystoreFileNames)
              + "&wsdlFileNames=" + escapeEx(wsdlFileNames);

      return params;
    }

    function submitPage() {
      document.getElementById('dlg').style.top = '300px';
      fade();
      document.getElementById('dlg').innerHTML = '<center>' +
              msgCreating + '</center>';
      var url = "../console/ajax/AjaxProxy.jsp";
      var params = 'locale=' + userLocale +
              '&class=com.sun.identity.workflow.CreateSoapSTSDeployment' + getData();
      ajaxPost(ajaxObj, url, params, configured);
      return false;
    }

    function configured() {
      if (ajaxObj.readyState == 4) {
        var result = hexToString(ajaxObj.responseText);
        var status = result.substring(0, result.indexOf('|'));
        var result = result.substring(result.indexOf('|') +1);
        var msg = '<center><p>' + result + '</p></center>';
        if (status == 0) {
          msg = '<center>' + ttlCreated + msg + msgCreated + '</center>';
        } else {
          msg = msg + '<center>' +  closeBtn + '</center>';
        }
        document.getElementById('dlg').innerHTML = msg;
      }
    }

  </script>
</jato:useViewBean>