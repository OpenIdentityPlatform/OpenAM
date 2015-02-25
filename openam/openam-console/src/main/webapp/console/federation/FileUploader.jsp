<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

   Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved

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

   $Id: FileUploader.jsp,v 1.8 2009/08/07 23:44:08 asyhuang Exp $

--%>
<%-- Portions Copyrighted 2014 ForgeRock AS. --%>

<%@ page info="FileUploader" language="java" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<%@taglib uri="/WEB-INF/cc.tld" prefix="cc" %>
<jato:useViewBean
    className="com.sun.identity.console.federation.FileUploaderViewBean"
    fireChildDisplayEvents="true" >

    <cc:i18nbundle baseName="amConsole" id="amConsole"
                   locale="<%=((com.sun.identity.console.base.AMViewBeanBase) viewBean).getUserLocale()%>"/>

    <cc:header name="hdrCommon" pageTitle="webconsole.title" bundleID="amConsole" copyrightYear="2008" fireDisplayEvents="false">

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
                var methodName;
                var labelName;                
                if (dataDiv) {                   
                    data = dataDiv.innerHTML;
                    data = data.replace(/^\s+/, '');
                    data = data.replace(/\s+$/, '');
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
                        var idx = fldName.indexOf('_');
                        if (idx != -1) {
                            labelName = fldName.substring(idx+1);
                            fldName = fldName.substring(0, idx);
                        }
                        var idx = labelName.indexOf('_');
                        if (idx != -1) {
                            methodName = labelName.substring(idx+1);
                            labelName = labelName.substring(0, idx);
                        }
                        var parent = opener.document.forms[0];
                        var field = parent.elements[parent.name + '.' + fldName];
                        field.value = data + '<!-- ' + escapeHtml(filename) + ' -->';
                        if (labelName) {
                            var labelWidget = opener.document.getElementById(labelName);
                            // innerText is an IEism but implemented in most browsers. Firefox uses textContent instead.
                            if (labelWidget.innerText) {
                                labelWidget.innerText = filename;
                            } else {
                                labelWidget.textContent = filename;
                            }
                        }
                        if (methodName) {
                            eval("opener." + methodName + '()');
                        }
                        self.close();
                    }
                }
            }

            function escapeHtml(unsafe) {
                var tn = document.createTextNode(unsafe);
                var p = document.createElement('p');
                p.appendChild(tn);
                return p.innerHTML;
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
