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

   $Id: logout.jsp,v 1.9 2009/07/02 22:00:17 exu Exp $

--%>

<%@page
    import="com.sun.identity.wsfederation.common.WSFederationConstants"
    import="java.util.Map"
    import="com.sun.identity.plugin.session.SessionManager"
    import="com.sun.identity.shared.encode.URLEncDec"
    import="com.sun.identity.multiprotocol.MultiProtocolUtils"
    import="com.sun.identity.multiprotocol.SingleLogoutManager"
    import="org.owasp.esapi.ESAPI"
%>
<%
    String displayName = 
        (String)request.getAttribute(WSFederationConstants.LOGOUT_DISPLAY_NAME);
    String wreply = 
        (String)request.getAttribute(WSFederationConstants.LOGOUT_WREPLY);
    Map<String, String> providerList = 
        (Map<String, String>)request.getAttribute(
        WSFederationConstants.LOGOUT_PROVIDER_LIST);
    String uri = request.getRequestURI();
    String deploymentURI = uri;
    int firstSlashIndex = uri.indexOf("/");
    int secondSlashIndex = uri.indexOf("/", firstSlashIndex+1);
    if (secondSlashIndex != -1) {
        deploymentURI = uri.substring(0, secondSlashIndex);
    }
%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <title>Signing Out</title>
        <script language="JavaScript">
        <%
        // handle multi-federation protocol case
        Object uSession = null;
        try {
            uSession = SessionManager.getProvider().getSession(request);
        } catch (Exception e) {
            // ignore
        }
        if ((providerList != null) && !providerList.isEmpty() 
             && (uSession != null) && 
            SessionManager.getProvider().isValid(uSession) &&
            MultiProtocolUtils.isMultipleProtocolSession(uSession, 
                SingleLogoutManager.WS_FED)) 
        {
            StringBuffer redirectUrl = new StringBuffer();
            redirectUrl.append(deploymentURI).
                append("/wsfederation/jsp/multi.jsp");
            String realm = (String)
                request.getAttribute(WSFederationConstants.REALM_PARAM);
            String idpEntityId = (String)
                request.getAttribute(WSFederationConstants.ENTITYID_PARAM);
            if ((realm != null)  && (realm.length() != 0)) {
                redirectUrl.append("?").
                    append(WSFederationConstants.REALM_PARAM).
                    append("=").append(realm);
            }
            if ((idpEntityId != null) && (idpEntityId.length() != 0)) {
                if (redirectUrl.toString().indexOf("?") == -1) {
                    redirectUrl.append("?");
                } else {
                    redirectUrl.append("&");
                }
                redirectUrl.append(WSFederationConstants.ENTITYID_PARAM).
                    append("=").append(URLEncDec.encode(idpEntityId));
            }

            if ((wreply != null) && (wreply.length() != 0)) {
                if (redirectUrl.toString().indexOf("?") == -1) {
                    redirectUrl.append("?");
                } else {
                    redirectUrl.append("&");
                }
                redirectUrl.append(WSFederationConstants.LOGOUT_WREPLY).
                    append("=").append(URLEncDec.encode(wreply));
            }
            wreply = redirectUrl.toString();
        }
        if ((wreply != null) && (wreply.length() != 0)) {
        %>
            function startTimer() {
                if (window.self == window.top) {
                    setTimeout(redirectToWReply,6000);
                } else {
                    setTimeout(redirectToWReply, 3000);
                }
            }
            function redirectToWReply () {
                document.location.href="<%=wreply%>";
            } 
        <%
        } else {
        %>
            function startTimer() {
            }
        <%
        }
        %>
        </script>
        <link rel="stylesheet" type="text/css" href="<%= deploymentURI %>/com_sun_web_ui/css/css_ns6up.css" />
        <link rel="shortcut icon" href="<%= deploymentURI %>/com_sun_web_ui/images/favicon/favicon.ico" type="image/x-icon" />
    </head>
    <body class="DefBdy" onload="startTimer();">
        <div class="SkpMedGry1"><a href="#SkipAnchor3860"><img src="<%= deploymentURI %>/com_sun_web_ui/images/other/dot.gif" alt="Jump to End of Masthead" border="0" height="1" width="1"></a></div><div class="MstDiv">
            <table class="MstTblBot" title="" border="0" cellpadding="0" cellspacing="0" width="100%">
                <tr>
                    <td class="MstTdTtl" width="99%">
                        <div class="MstDivTtl"><img name="AMConfig.configurator.ProdName" src="<%= deploymentURI %>/console/images/PrimaryProductName.png" alt="OpenAM" border="0"></div>
                    </td>
                    <td class="MstTdLogo" width="1%"><img name="AMConfig.configurator.BrandLogo" src="<%= deploymentURI %>/com_sun_web_ui/images/other/javalogo.gif" alt="Java(TM) Logo" border="0" height="55" width="31"></td>
                </tr>
            </table>
            <table class="MstTblEnd" border="0" cellpadding="0" cellspacing="0" width="100%"><tr><td><img name="RMRealm.mhCommon.EndorserLogo" src="<%= deploymentURI %>/com_sun_web_ui/images/masthead/masthead-sunname.gif" alt="Sun(TM) Microsystems, Inc." align="right" border="0" height="10" width="108" /></td></tr></table>
        </div>
        <table class="SkpMedGry1" border="0" cellpadding="5" cellspacing="0" width="100%"><tr><td><img src="<%= deploymentURI %>/com_sun_web_ui/images/other/dot.gif" alt="Jump to End of Masthead" border="0" height="1" width="1"></td></tr></table>
        <table border="0" cellpadding="10" cellspacing="0" width="100%"><tr><td></td></tr></table>
        <table cellpadding=5>
            <tr>
                <td>
                  <%
                    if ( wreply!=null && wreply.length()>0 )
                    {
                  %>
                        <script>
                            document.write("<p id=\"logoutPrompt\">Signing out of <%=ESAPI.encoder().encodeForHTML(displayName)%></p>");
                        </script>
                        <noscript>
                            <p><a href="<%=wreply%>">Click here</a> to continue</p>
                        </noscript>
                  <%
                    }
                  %>
                  <%
                    for ( String url : providerList.keySet() )
                    {
                  %>
                        <p>Signing out from <%=ESAPI.encoder().encodeForHTML(providerList.get(url))%></p>
                        <iframe width="500" src="<%=url%>"></iframe>
                  <%
                    }
                  %>
                </td>
            </tr>
        </table>
    </body>
</html>
