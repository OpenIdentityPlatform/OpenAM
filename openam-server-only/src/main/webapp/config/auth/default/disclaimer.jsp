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
                                                                                
   $Id: disclaimer.jsp,v 1.5 2008/08/15 01:05:28 veiming Exp $
                                                                                
--%>
<%--
   Portions Copyrighted 2012 ForgeRock Inc
--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
    <%@page info="Disclaimer Page" language="java"%>
    <%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>
    <%@taglib uri="/WEB-INF/auth.tld" prefix="auth"%>
    <jato:useViewBean className="com.sun.identity.authentication.UI.LoginViewBean">
        <%@ page contentType="text/html" %>
        <head>
            <title><jato:text name="htmlTitle_Disclaimer" /></title>
            <%
            String ServiceURI = (String) viewBean.getDisplayFieldValue(viewBean.SERVICE_URI);
            %>
            <link href="<%= ServiceURI%>/css/new_style.css" rel="stylesheet" type="text/css" />
            <!--[if IE 9]> <link href="<%= ServiceURI %>/css/ie9.css" rel="stylesheet" type="text/css"> <![endif]-->
            <!--[if lte IE 7]> <link href="<%= ServiceURI %>/css/ie7.css" rel="stylesheet" type="text/css"> <![endif]-->
            <script language="JavaScript" type="text/javascript">
                function LoginSubmit(value) {
                    var frm = document.forms[0];
                    frm.elements['IDButton'].value = value;
                    frm.submit();
                }
            </script>
        </head>
        <body>
            <div class="container_12">
                <div class="grid_4 suffix_8">
                    <a class="logo" href="<%= ServiceURI%>"></a>
                </div>
                <div class="box box-spaced clear-float">
                    <div class="grid_3">
                        <div class="product-logo"></div>
                    </div>
                    <div class="grid_9">
                        <div class="box-content clear-float">
                            <div class="message">
                                <span class="icon info"></span>
                                <h3><auth:resBundle bundleName="amAuthUI" resourceKey="disclaimer.notice" /></h3>
                                <h3><auth:resBundle bundleName="amAuthUI" resourceKey="doyou.agree" /></h3>
                            </div>
                            <auth:form name="Login" method="post" defaultCommandChild="DefaultLoginURL" >
                                <fieldset>
                                    <jato:content name="hasButton">
                                        <div class="row">
                                            <input name="IDButton" type="hidden"/>
                                            <jato:tiledView name="tiledButtons" type="com.sun.identity.authentication.UI.ButtonTiledView">
                                                <input name="Login.Submit" type="button" class="<jato:text name="txtClass" />" onclick="LoginSubmit('<jato:text name="txtButton" />'); return false;" value="<jato:text name="txtButton" />" />
                                            </jato:tiledView>
                                        </div>
                                    </jato:content>
                                </fieldset>
                            </auth:form>
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
