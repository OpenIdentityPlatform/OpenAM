<%--
    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved

    The contents of this file are subject to the terms
    of the Common Development and Distribution License
    (the License). You may not use this file except in
    compliance with the License.

    You can obtain a copy of the License at
    http://forgerock.org/license/CDDLv1.0.html
    See the License for the specific language governing
    permission and limitations under the License.

    When distributing Covered Code, include this CDDL
    Header Notice in each file and include the License file
    at http://forgerock.org/license/CDDLv1.0.html
    If applicable, add the following below the CDDL Header,
    with the fields enclosed by brackets [] replaced by
    your own identifying information:
    "Portions Copyrighted [year] [name of copyright owner]"

--%>

<%@ page language="java" %>
<%@ page import="com.iplanet.sso.SSOToken" %>
<%@ page import="com.sun.identity.sm.*" %>
<%@ page import="java.util.*" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>OpenAM</title>
    <link rel="stylesheet" type="text/css" href="com_sun_web_ui/css/css_ns6up.css"/>
    <link rel="shortcut icon" href="com_sun_web_ui/images/favicon/favicon.ico" type="image/x-icon"/>

    <style type="text/css">

        .attribute {
            color: red;
            width: 550px;
            padding: 0px;
            /*background-color: #eeeeee;*/
        }

        .attributeRowA {
            width: 100%;
            background-color: #eeeeee;
            padding: 6px;
        }

        .attributeRowB {
            width: 100%;
            background-color: #dddddd;
            padding: 6px;
        }

        .attribute .name {
            color: blue;
            font-size: 12px;
            font-family: courier;
            /*background-color: #00008b;*/
        }

        .attribute .description {
            color: black;
            font-size: 15px;
            /*background-color: #b8860b;*/
        }

        .attribute .descriptionMissing {
            color: #666666;
            font-size: 15px;
        }

        .attribute .help {
            color: green;
            font-size: 12px;
            /*background-color: #006400;*/
        }

        .schemaType {
            position: relative;
            float: left;
            clear: left;
            padding: 2px;
            width: 750px;
            font-family: arial, helvetica;
        }

        .schemaBlock {
            position: relative;
            float: left;
            width: 630px;
            display: block;
            font-family: arial, helvetica;
        }

        .schemaTitle {
            width: 120px;
            float: left;
            font-size: 12px;
        }

        .service {
            position: relative;
            float: left;
            padding-top: 10px;
            width: 800px;
            font-family: arial, helvetica;
        }

        .serviceName {
            position: relative;
            float: left;
            font-weight: bold;
            font-size: 16px;
            /*background-color: #bbbbcc;*/
            padding: 2px;
            width: 796px;
        }

        .serviceBlock {
            position: relative;
            float: left;
            clear: left;
            left: 10px;
            width: 790px;
            display: block;
        }


    </style>

    <script src="js/jquery.js"></script>

    <script type="text/javascript">

        $(document).ready(function () {
            $(".schemaTitle").click(function () {
                $(this).parent().children(".schemaBlock").toggle("fast");
            });
            $(".serviceName").click(function () {
                $(this).parent().children(".serviceBlock").toggle("fast");
            });
        });

    </script>

</head>


<body class="DefBdy">
<div class="SkpMedGry1"><a href="#SkipAnchor3860"><img src="com_sun_web_ui/images/other/dot.gif" alt="Jump to End of Masthead" border="0" height="1" width="1"/></a></div>
<div class="MstDiv">
    <table class="MstTblBot" title="" border="0" cellpadding="0" cellspacing="0" width="100%">
        <tr>
            <td class="MstTdTtl">
                <div class="MstDivTtl"><img name="AMConfig.configurator.ProdName" src="com_sun_web_ui/images/PrimaryProductName.png" alt="OpenAM" border="0"/></div>
            </td>
        </tr>
    </table>
</div>
<table class="SkpMedGry1" border="0" cellpadding="5" cellspacing="0" width="100%">
    <tr>
        <td><img src="com_sun_web_ui/images/other/dot.gif" alt="Jump to End of Masthead" border="0" height="1" width="1"/></a></td>
    </tr>
</table>
<table border="0" cellpadding="10" cellspacing="0" width="100%">
    <tr>
        <td></td>
    </tr>
</table>
<%@ include file="/WEB-INF/jsp/admincheck.jsp" %>
<%

    SSOToken ssoToken = requireAdminSSOToken(request, response, out, "showServerConfig.jsp");
    if (ssoToken == null) {
%>
</body></html>
<%
        return;
    }

%>
    <table cellpadding=15>
    <tr>
        <td>


            <div id="doc" style="max-width: 800px; display: block;">

                <p><strong>OpenAM Service Attributes</strong></p>

                <p>In order to translate configuration changes made in OpenAM console to
                    <code>ssoadm</code> commands, you must first match the GUI settings to
                    service attributes used by <code>ssoadm</code>. This page lists
                    available service attributes, including their labels and online help from
                    OpenAM console where available. Service attributes apply according to their type:</p>

                <dl>
                    <dt>Dynamic</dt>
                    <dd>Applies to a role or a realm</dd>

                    <dt>Global</dt>
                    <dd>Applies to the entire OpenAM server</dd>

                    <dt>Organization</dt>
                    <dd>Applies to a realm</dd>
                </dl>

                <p>To find the service attribute that corresponds to a particular GUI
                    setting, click Expand below, and then search for the label of the GUI
                    setting within this page.</p>

                <p>For example, suppose you changed <code>Maximum Session Time</code> in
                    the OpenAM console to 240 seconds. You search this page for
                    <code>Maximum Session Time</code> and find the dynamic attribute
                    <code>iplanet-am-session-max-session-time</code> on the
                    <code>iPlanetAMSessionService</code> service. To set maximum session
                    time to 240 seconds by script, you use the following command.</p>

<pre>ssoadm set-attr-defs -u amadmin -f /tmp/pwd.txt -s iPlanetAMSessionService \
 -t dynamic -a "iplanet-am-session-max-session-time=240"</pre>

                <hr>

            </div>

            <br/>
            <div>
                <div style="font-size: 12px; display: inline; text-color: #666666; text-decoration: underline;"
                     onclick="$(document).find('.serviceBlock').show(); $(document).find('.schemaBlock').show();">Expand all
                </div>
                    &nbsp;|&nbsp;
                <div style="font-size: 12px; display: inline; text-color: #666666; text-decoration: underline;"
                     onclick="$(document).find('.serviceBlock').hide(); $(document).find('.schemaBlock').hide();">Collapse all
                </div>
            </div>
            <br/>

            <%

                ServiceManager sm = new ServiceManager(ssoToken);
                Set serviceNames = sm.getServiceNames();

                for (Object o : serviceNames) {
                    String serviceName = o.toString();

                    out.println("<div class='service' name='" + o + "'>");
                    out.println("<div class='serviceName'>" + serviceName + "</div>");
                    out.println("<div class='serviceBlock'>");

                    // Assume version 1.0 (as of 2012-06 all services are version 1.0)
                    ServiceConfigManager scm = sm.getConfigManager(serviceName, "1.0");
                    Set<String> instances = scm.getInstanceNames();

                    // To list instances
//                    for (String instance : instances) {
//                        out.println("Instance: " + instance + "");
//                    }


                    ServiceSchemaManager ssm = sm.getSchemaManager(serviceName, "1.0");

                    if (ssm != null) {

                        Map<String, ServiceSchema> serviceSchemaList = new HashMap<String, ServiceSchema>();
                        ServiceSchema dynamic = ssm.getDynamicSchema();
                        if (dynamic != null) {
                            serviceSchemaList.put("Dynamic", dynamic);
                        }
                        ServiceSchema global = ssm.getGlobalSchema();
                        if (global != null) {
                            serviceSchemaList.put("Global", global);
                        }
                        ServiceSchema org = ssm.getOrganizationSchema();
                        if (org != null) {
                            serviceSchemaList.put("Organization", org);
                        }

                        for (String schemaType : serviceSchemaList.keySet()) {

                            ServiceSchema ss = serviceSchemaList.get(schemaType);
                            String i18nFilename = ss.getI18NFileName();
                            ResourceBundle bundle = ResourceBundle.getBundle(i18nFilename);

                            if (ss != null) {
                                Set<String> serviceAttributeNames = ss.getServiceAttributeNames();

                                // Ignore this schema if it is empty
                                if (serviceAttributeNames == null || serviceAttributeNames.isEmpty()) {
                                    continue;
                                }

                                out.println("<div class='schemaType'><div class='schemaTitle'>" + schemaType + "</div>");
                                out.println("<div class='schemaBlock'>");

                                boolean rowAlternator = true;
                                for (String s : serviceAttributeNames) {
                                    // Alternating row backgrounds
                                    rowAlternator = !rowAlternator;

                                    AttributeSchema as = ss.getAttributeSchema(s);
                                    out.println("<div class='attribute'>");

                                    if (rowAlternator)
                                        out.println("<div class='attributeRowA'>");
                                    else
                                        out.println("<div class='attributeRowB'>");

                                    out.println("<div class='name'>" + s + "</div>");
                                    try {
                                        if (as.getI18NKey() != null)
                                            out.println("<div class='description'>" + bundle.getString(as.getI18NKey()) + "</div>");
                                    } catch (MissingResourceException mre) {
                                        out.println("<div class='descriptionMissing'>" + s + " (missing description)</div>");
                                    }
                                    try {
                                        if (as.getI18NKey() != null)
                                            out.println("<div class='help'>" + bundle.getString(as.getI18NKey().concat(".help")) + "</div>");
                                    } catch (MissingResourceException mre) {
                                    }
                                    out.println("</div>");
                                    out.println("</div>");
                                }
                                out.println("</div>");
                                out.println("</div>");
                            }
                        }
                    }

                    out.println("</div></div>");
                }

            %>


        </td>
    </tr>
</table>
</body>
</html>

