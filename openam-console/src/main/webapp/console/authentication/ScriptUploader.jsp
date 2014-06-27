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

<%-- Portions copyright 2008 Sun Microsystems Inc. --%>
<%-- Based on/simplified from federation/FileUploader.jsp --%>

<%@ page info="ScriptUploader" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>


<jato:useViewBean
        className="com.sun.identity.console.authentication.ScriptUploaderViewBean"
        fireChildDisplayEvents="true" >

    <cc:i18nbundle baseName="amConsole" id="amConsole"
                   locale="<%=((com.sun.identity.console.base.AMViewBeanBase) viewBean).getUserLocale()%>"/>

    <cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2014" fireDisplayEvents="false">

        <link rel="stylesheet" type="text/css" href="../console/css/openam.css" />
        <script language="javascript" src="../console/js/am.js">
        </script>

        <script language="javascript">
            var filename = '';
            var failUpload = "<cc:text name="txtConfigured" defaultValue="ajax.upload.file.failed" bundleID="amConsole" escape="false" />";
            var errorUpload = "<cc:text name="txtConfigured" defaultValue="{0}" escape="false" />";
            var closeBtn = '<p>&nbsp;</p><p><div class="TtlBtnDiv"><input name="btnClose" type="submit" class="Btn1" value="<cc:text name="txtCloseBtn" defaultValue="ajax.close.button" bundleID="amConsole" />" onClick="focusMain();return false;" /></div></p>';
            function selectFile(data) {
                var idoc = document.getElementById('fileupload');
                var dataDiv = idoc.contentWindow.document.getElementById('data');
                if (dataDiv) {
                    data = dataDiv.innerHTML;
                    data = data.replace(/^\s+/, '');
                    data = data.replace(/\s+$/, '');
                    data = data.replace(/&amp;/g, '&');
                    data = data.replace(/&lt;/g, '<');
                    data = data.replace(/&gt;/g, '>');
                    if (data.length == 0) {
                        fade();
                        document.getElementById('dlg').innerHTML = '<center>' +
                                failUpload + '<p>&nbsp;</p>' +  closeBtn + '</center>';
                    } else if(data.search("Error:") == 0){
                        var msg = data.substring(7);
                        msg = errorUpload.replace("{0}", msg);
                        fade();
                        document.getElementById('dlg').innerHTML = '<center>' +
                                msg + '<p>&nbsp;</p>' +  closeBtn + '</center>';
                    } else {
                        var fldName = window.name;
                        var parent = opener.document.forms[0];
                        var field = parent.elements[parent.name + '.' + fldName];
                        field.value = data;
                        self.close();
                    }
                }
            }
        </script>

        <div id="main" style="position: absolute; margin: 0; border: none; padding: 0; width:auto; height:101%;">
        <cc:secondarymasthead name="secondaryMasthead" />

        <form name="FileUploader" action="../console/ajax/FileUpload.jsp"
              enctype="multipart/form-data" method="post" target="fileupload" onSubmit="filename=this.elements['fileX'].value;">

            <cc:pagetitle name="pgtitle"
                          bundleID="amConsole"
                          pageTitleText="file.uploader.title"
                          showPageTitleSeparator="true"
                          viewMenuLabel=""
                          pageTitleHelpMessage=""
                          showPageButtonsTop="false"
                          showPageButtonsBottom="true" >
            </cc:pagetitle>

            <p>&nbsp;</p>
            <table border=0 cellpadding=10>
                <tr><td>
                    <input type="file" name="fileX" />
                </td></tr>
            </table>
        </form>

        <iframe style="display:none" src="about:blank" id="fileupload" name="fileupload" onload="selectFile(this.contentWindow.document);"></iframe>
    </cc:header>
    </div>
    <div id="dlg" class="dvs"></div>
</jato:useViewBean>
