<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
  
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
                                                                                
   $Id: register.jsp,v 1.6 2008/08/15 01:05:29 veiming Exp $
                                                                                
--%>
<%--
   Portions Copyrighted 2012 ForgeRock Inc
--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
    <%@page info="Self Registration Module" language="java"%>
    <%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>
    <%@taglib uri="/WEB-INF/auth.tld" prefix="auth"%>
    <jato:useViewBean className="com.sun.identity.authentication.UI.LoginViewBean">
        <%@ page contentType="text/html" %>
        <head>
            <title><jato:text name="htmlTitle_SelfRegModule" /></title>
            <%
            String ServiceURI = (String) viewBean.getDisplayFieldValue(viewBean.SERVICE_URI);
            %>
            <link href="<%= ServiceURI%>/css/new_style.css" rel="stylesheet" type="text/css" />
            <!--[if IE 9]> <link href="<%= ServiceURI%>/css/ie9.css" rel="stylesheet" type="text/css"> <![endif]-->
            <!--[if lte IE 7]> <link href="<%= ServiceURI%>/css/ie7.css" rel="stylesheet" type="text/css"> <![endif]-->
            <script language="JavaScript" src="<%= ServiceURI%>/js/auth.js" type="text/javascript"></script>
            <jato:content name="validContent">
                <script language="JavaScript" type="text/javascript">
                    <!--
                    var defaultBtn = 'Submit';
                    var elmCount = 0;

                    function defaultSubmit() {
                        LoginSubmit(defaultBtn);
                    }

                    function LoginSubmit(value) {
                        aggSubmit();
                        var hiddenFrm = document.forms['Login'];

                        if (hiddenFrm != null) {
                            hiddenFrm.elements['IDButton'].value = value;
                            hiddenFrm.submit();
                        }
                    }

                    function resetForms() {
                        var frms = document.forms;
                        for (var i = 0; i < elmCount; i++) {
                            var frm = frms['frm' + i];

                            if (frm != null) {
                                clearFormElms(frm);
                            }
                        }
                        placeCursorOnFirstElm();
                    }
                    -->
                </script>
            </jato:content>
        </head>
        <body onload="placeCursorOnFirstElm();">
            <div class="container_12">
                <div class="grid_4 suffix_8">
                    <a class="logo" href="<%= ServiceURI%>"></a>
                </div>
                <div class="box clear-float">
                    <div class="grid_3">
                        <div class="product-logo"></div>
                    </div>
                    <div class="grid_9 left-seperator">
                        <div class="box-content clear-float">
                            <jato:content name="ContentStaticTextHeader">
                                <h1><jato:getDisplayFieldValue name='StaticTextHeader'
                                                           defaultValue='Authentication' fireDisplayEvents='true'
                                                           escape='false'/></h1>
                                </jato:content>
                            <p class="message"><span class="icon info"></span>
                                <img src="<%= ServiceURI %>/images/required.gif" alt="Required Field"
                                     title="Required Field" width="7" height="14" />&nbsp;
                                <auth:resBundle bundleName="amAuthUI" resourceKey="reqfield.desc" /></p>
                                <jato:content name="validContent">
                                    <jato:tiledView name="tiledCallbacks"
                                                    type="com.sun.identity.authentication.UI.CallBackTiledView">
                                    <script language="javascript" type="text/javascript">
                                        <!--
                                        elmCount++;
                                        -->
                                    </script>
                                    <jato:content name="textBox">
                                        <form name="frm<jato:text name="txtIndex" />" action="blank"
                                              onsubmit="defaultSubmit(); return false;" method="post">
                                            <div class="row">
                                                <label for="IDToken<jato:text name="txtIndex" />">
                                                    <jato:text name="txtPrompt" defaultValue="User name:" escape="false" />
                                                    <jato:content name="isRequired">
                                                        <img src="<%= ServiceURI %>/images/required.gif" alt="Required Field"
                                                             title="Required Field" width="7" height="14" />
                                                    </jato:content>
                                                </label>
                                                <input class="textbox" type="text" name="IDToken<jato:text name="txtIndex" />" id="IDToken<jato:text name="txtIndex" />" value="<jato:text name="txtValue" />" />
                                                <jato:content name="hasInfoText">
                                                    <p class="message"><span class="icon error"></span><jato:text name="txtInfo" /></p>
                                                </jato:content>
                                            </div>
                                        </form>
                                    </jato:content>
                                    <jato:content name="password">
                                        <form name="frm<jato:text name="txtIndex" />" action="blank"
                                              onsubmit="defaultSubmit(); return false;" method="post">
                                            <div class="row">
                                                <label for="IDToken<jato:text name="txtIndex" />">
                                                    <jato:text name="txtPrompt" defaultValue="Password:" escape="false" />
                                                    <jato:content name="isRequired">
                                                        <img src="<%= ServiceURI %>/images/required.gif" alt="Required Field"
                                                             title="Required Field" width="7" height="14" />
                                                    </jato:content>
                                                </label>
                                                <input class="textbox" type="password" name="IDToken<jato:text name="txtIndex" />" id="IDToken<jato:text name="txtIndex" />" value="" />
                                                <jato:content name="hasInfoText">
                                                    <p class="message"><span class="icon error"></span><jato:text name="txtInfo" /></p>
                                                </jato:content>
                                            </div>
                                        </form>
                                    </jato:content>
                                    <jato:content name="choice">
                                        <form name="frm<jato:text name="txtIndex" />" action="blank"
                                              onsubmit="defaultSubmit(); return false;" method="post">
                                            <div class="row">
                                                <label for="IDToken<jato:text name="txtIndex" />">
                                                    <jato:text name="txtPrompt" defaultValue="RadioButton:" escape="false" />
                                                    <jato:content name="isRequired">
                                                        <img src="<%= ServiceURI %>/images/required.gif" alt="Required Field"
                                                             title="Required Field" width="7" height="14" />
                                                    </jato:content>
                                                </label>
                                                <div class="radios">
                                                    <jato:tiledView name="tiledChoices" type="com.sun.identity.authentication.UI.CallBackChoiceTiledView">
                                                        <jato:content name="selectedChoice">
                                                            <input type="radio" name="IDToken<jato:text name="txtParentIndex" />" id="IDToken<jato:text name="txtIndex" />" value="<jato:text name="txtIndex" />" checked="checked" />
                                                            <label for="IDToken<jato:text name="txtIndex" />">
                                                                <jato:text name="txtChoice" />
                                                            </label>
                                                        </jato:content>
                                                        <jato:content name="unselectedChoice">
                                                            <input type="radio" name="IDToken<jato:text name="txtParentIndex" />" id="IDToken<jato:text name="txtIndex" />" value="<jato:text name="txtIndex" />" />
                                                            <label for="IDToken<jato:text name="txtIndex" />">
                                                                <jato:text name="txtChoice" />
                                                            </label>
                                                        </jato:content>
                                                    </jato:tiledView>
                                                </div>

                                            </div>
                                        </form>
                                    </jato:content>
                                </jato:tiledView>
                            </jato:content>
                            <jato:content name="ContentStaticTextResult">
                                <!-- after login output message -->
                                <p><b><jato:getDisplayFieldValue name='StaticTextResult'
                                                           defaultValue='' fireDisplayEvents='true' escape='false'/></b></p>
                                    </jato:content>
                                    <jato:content name="ContentHref">
                                <!-- URL back to Login page -->
                                <p><auth:href name="LoginURL"
                                           fireDisplayEvents='true'>
                                        <jato:text name="txtGotoLoginAfterFail" /></auth:href></p>
                                </jato:content>
                                <jato:content name="ContentImage">
                                <!-- customized image defined in properties file -->
                                <p><img name="IDImage" src="<jato:getDisplayFieldValue name='Image'/>" alt=""/></p>
                            </jato:content>
                            <jato:content name="ContentButtonLogin">
                                <fieldset>
                                    <jato:content name="hasButton">
                                        <div class="row">
                                            <jato:tiledView name="tiledButtons" type="com.sun.identity.authentication.UI.ButtonTiledView">
                                                <input name="Login.Submit" type="button" onclick="LoginSubmit('<jato:text name="txtButton" />'); return false;" class="<jato:text name="txtClass" />" value="<jato:text name="txtButton" />" />
                                            </jato:tiledView>
                                            <input name="Login.Submit" type="button" onclick="resetForms(); return false;" class="button" value="<jato:text name="lblReset" />" />
                                        </div>
                                    </jato:content>
                                    <jato:content name="hasNoButton">
                                        <div class="row">
                                            <input name="Login.Submit" type="button" onclick="LoginSubmit('<jato:text name="cmdSubmit" />'); return false;" class="button primary" value="<jato:text name="lblSubmit" />" />
                                            <input name="Login.Submit" type="button" onclick="resetForms(); return false;" class="button" value="<jato:text name="lblReset" />" />
                                        </div>
                                    </jato:content>
                                </fieldset>
                            </jato:content>
                            <jato:content name="validContent">
                                <auth:form name="Login" method="post" defaultCommandChild="DefaultLoginURL">
                                    <script language="javascript" type="text/javascript">
                                        <!--
                                        if (elmCount != null) {
                                            for (var i = 0; i < elmCount; i++) {
                                                document.write(
                                                "<input name=\"IDToken" + i + "\" type=\"hidden\">");
                                            }
                                            document.write("<input name=\"IDButton"  + "\" type=\"hidden\">");
                                        }
                                        -->
                                    </script>
                                    <input type="hidden" name="page_state" value="<auth:value key='PageState' />"/>
                                </auth:form>
                            </jato:content>
                        </div>
                    </div>
                </div>
                <div class="footer alt-color">
                    <div class="grid_6 suffix_3">
                        <p><auth:resBundle bundleName="amAuthUI" resourceKey="copyright.notice" /></p>
                    </div>
                </div>
            </div>
        </body>
    </jato:useViewBean>
</html>


