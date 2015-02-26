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

   $Id: Login.jsp,v 1.11 2009/01/09 07:13:21 bhavnab Exp $

--%>
<%--
   Portions Copyrighted 2013 Syntegrity
   Portions Copyrighted 2013 ForgeRock Inc
--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
    <%@page info="Login" language="java"%>
    <%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>
    <%@taglib uri="/WEB-INF/auth.tld" prefix="auth"%>
    <jato:useViewBean className="com.sun.identity.authentication.UI.LoginViewBean">
        <%@page contentType="text/html" %>
        <head>
            <title><jato:text name="htmlTitle_Login" /></title>
            <%
                String ServiceURI = (String) viewBean.getDisplayFieldValue(viewBean.SERVICE_URI);
                String encoded = "false";
                String gotoURL = (String) viewBean.getValidatedInputURL(
                        request.getParameter("goto"), request.getParameter("encoded"), request);
                String gotoOnFailURL = (String) viewBean.getValidatedInputURL(
                        request.getParameter("gotoOnFail"), request.getParameter("encoded"), request);
                String encodedQueryParams = (String) viewBean.getEncodedQueryParams(request);
                if ((gotoURL != null) && (gotoURL.length() != 0)) {
                    encoded = "true";
                }
            %>
            <script type="text/javascript">
                if (typeof console == "undefined") {
                    this.console = {
                        log: function() {},
                        info: function() {},
                        debug: function() {},
                        warn: function() {},
                        error: function() {}
                    };
                }
                if (typeof JSON == "undefined") {
                    var ss = document.createElement('script');
                    ss.src = "<%= ServiceURI %>/js/json2/json2/1.0/json2-1.0.js";
                    var hh = document.getElementsByTagName('head')[0];
                    hh.appendChild(ss);
                }
            </script>
            <script language="JavaScript" src="<%= ServiceURI %>/js/browserVersion.js"></script>
            <script language="JavaScript" src="<%= ServiceURI%>/js/auth.js" type="text/javascript"></script>
            <script language="JavaScript" data-main="<%= ServiceURI %>/js/openam-authnmodule-adaptive-deviceprint-scripts-min.js" src="<%= ServiceURI %>/js/require-jquery.js"></script>
            <jato:content name="validContent">
                <script language="JavaScript" type="text/javascript">
                    <!--
                    var defaultBtn = 'Submit';
                    var elmCount = 0;

                    /**
                     * submit form with default command button
                     */
                    function defaultSubmit() {
                        LoginSubmit(defaultBtn);
                    }

                    /**
                     * submit form with given button value
                     *
                     * @param value of button
                     */
                    function LoginSubmit(value) {
                        aggSubmit();
                        var hiddenFrm = document.forms['Login'];

                        if (hiddenFrm != null) {
                            hiddenFrm.elements['IDButton'].value = value;
                            if (this.submitted) {
                                alert("The request is currently being processed");
                            } else {
                                this.submitted = true;
                                hiddenFrm.submit();
                            }
                        }
                    }
                    -->
                </script>
            </jato:content>
        </head>
        <body>
            <div>
                <jato:content name="validContent">
                    <auth:form name="Login" method="post" defaultCommandChild="DefaultLoginURL">
                        <input type="button" name="Login.Submit" onclick="defaultSubmit(); return false;" value="Proceed" />
                        <input type="hidden" name="IDButton" />
                        <input type="hidden" name="IDToken0" id="IDToken0" />
                        <input type="hidden" name="goto" value="<%= gotoURL%>" />
                        <input type="hidden" name="gotoOnFail" value="<%= gotoOnFailURL%>"/>
                        <input type="hidden" name="SunQueryParamsString" value="<%= encodedQueryParams%>" />
                        <input type="hidden" name="encoded" value="<%= encoded%>" />
                    </auth:form>
                </jato:content>
            </div>
            <script type="text/javascript">
                window.onload = function() {
                    document.forms['Login'].submit();
                }
            </script>
        </body>
    </jato:useViewBean>
</html>

