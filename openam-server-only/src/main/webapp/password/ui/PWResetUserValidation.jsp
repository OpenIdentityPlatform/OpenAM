<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: PWResetUserValidation.jsp,v 1.5 2008/08/28 06:41:11 mahesh_prasad_r Exp $

--%>
<%--
   Portions Copyrighted 2012-2013 ForgeRock AS
--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<%@include file="../ui/PWResetBase.jsp" %>
<%@page info="PWResetUserValidation" language="java" pageEncoding="UTF-8" %>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato" %>
<jato:useViewBean className="com.sun.identity.password.ui.PWResetUserValidationViewBean" fireChildDisplayEvents="true">
    <head>
        <title><jato:text name="titleHtmlPage"/></title>
        <link href="<%= ServiceURI%>/css/new_style.css" rel="stylesheet" type="text/css"/>
        <!--[if IE 9]>
        <link href="<%= ServiceURI %>/css/ie9.css" rel="stylesheet" type="text/css"> <![endif]-->
        <!--[if lte IE 7]>
        <link href="<%= ServiceURI %>/css/ie7.css" rel="stylesheet" type="text/css"> <![endif]-->
        <script language="javascript">
            <!--
            set
            the
            focus
            for a given
            field
            and
            form -->
            function setFocus(frmName, field) {
                var frm = document.forms[frmName];
                if (frm != null) {
                    var elm = frm.elements[field];
                    if (elm != null) {
                        elm.focus();
                        elm.select();
                    }
                }
            }
        </script>
    </head>
    <body class="LogBdy" onLoad="setFocus('PWResetUserValidation','PWResetUserValidation.tfUserAttr');">
    <div class="container_12">
        <div class="grid_4 suffix_8">
            <a class="logo" href="<%= ServiceURI%>"></a>
        </div>
        <div class="box box-spaced clear-float">
            <div class="grid_3">
                <div class="product-logo"></div>
            </div>
            <div class="grid_9 left-seperator">
                <div class="box-content clear-float">
                    <h1><jato:text name="userValidationTitle"/></h1>
                    <jato:content name="errorBlock">
                        <div class="message">
                            <span class="icon error"></span>

                            <h3><jato:text name="errorTitle"/></h3>
                            <h4><jato:text name="errorMsg"/></h4>
                        </div>
                    </jato:content>
                    <jato:content name="infoBlock">
                        <div class="message">
                            <span class="icon info"></span>

                            <h3><jato:text name="infoMsg"/></h3>
                        </div>
                    </jato:content>
                    <jato:content name="resetPage">
                        <jato:form name="PWResetUserValidation" method="post" defaultCommandChild="/btnNext">
                            <fieldset>
                                <jato:hidden name="fldUserAttr"/>
                                <div class="row">
                                    <label for="PWResetUserValidation.tfUserAttr">
                                        <jato:text name="lblUserAttr"/>:
                                    </label>
                                    <jato:textField name="tfUserAttr" styleClass="textbox"/>
                                </div>
                                <div class="row">
                                    <input name="PWResetUserValidation.btnNext" type="submit" class="button primary"
                                           value="<jato:text name="btnNext" />"/>
                                </div>
                            </fieldset>
                        </jato:form>
                    </jato:content>
                </div>
            </div>
        </div>
        <div class="footer alt-color">
            <div class="grid_6 suffix_3">
                <p>Copyright &copy; 2008-2013, ForgeRock AS. <br/>All Rights Reserved. Use of this software is subject to the
                    terms and conditions of the ForgeRock&trade; License and Subscription Agreement.</p>
            </div>
        </div>
    </div>
    </body>
</jato:useViewBean>
</html>
