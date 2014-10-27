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


   Portions copyright 2014 ForgeRock AS.
--%>

<%@ page import="com.iplanet.am.util.SystemProperties" %>
<%@ page import="com.iplanet.sso.SSOException" %>
<%@ page import="com.iplanet.sso.SSOToken" %>
<%@ page import="com.iplanet.sso.SSOTokenManager" %>
<%@ page import="com.sun.identity.common.DNUtils" %>
<%@ page import="com.sun.identity.idm.AMIdentity" %>
<%@ page import="com.sun.identity.idm.IdRepoException" %>
<%@ page import="com.sun.identity.idm.IdType" %>
<%@ page import="com.sun.identity.idm.IdUtils" %>
<%@ page import="com.sun.identity.shared.debug.Debug" %>
<%@ page import="com.sun.identity.shared.encode.Hash" %>
<%@ page import="com.sun.identity.shared.ldap.util.DN" %>
<%@ page import="com.sun.identity.sm.SMSEntry" %>
<%@ page import="javax.servlet.http.HttpServletRequest" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="java.text.MessageFormat" %>

<%!
    /**
     * Ensures that the provided request contains an SSOToken with super user privileges.
     *
     * If the request contains an SSOToken with super user privileges, the SSOToken is
     * returned.
     *
     * If the request contains an SSOToken without super user privileges, the HTTP client
     * is informed that they are not authoriszed to access this page and null is returned.
     *
     * If the request does not contain an SSOToken, the HTTP client is redirected to the
     * login page with a follow on redirect back to the current page.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @param out The JspWriter used to inform the HTTP client that they are unauthorized to view this page.
     * @param currentPageUrl The path of the JSP page in which this file has been included, relative to AM root.
     * @return The SSOToken of the current user if they have one with super user privileges.
     * @throws IOException If attempting to write to out parameter fails.
     */
    public SSOToken requireAdminSSOToken(HttpServletRequest request,
                                         HttpServletResponse response,
                                         JspWriter out,
                                         String currentPageUrl) throws IOException {

        SSOToken ssoToken;

        try {

            // Obtain current user identity from ssoToken
            SSOTokenManager manager = SSOTokenManager.getInstance();
            ssoToken = manager.createSSOToken(request);
            manager.validateToken(ssoToken);
            AMIdentity user = new AMIdentity(ssoToken);

            // Obtain DN and identity for super user
            String adminUserDN = "";
            AMIdentity adminUserId = null;
            String adminUser = SystemProperties.get("com.sun.identity.authentication.super.user");
            if (adminUser != null) {
                adminUserDN = DNUtils.normalizeDN(adminUser);
                adminUserId = new AMIdentity(ssoToken, adminUser, IdType.USER, "/", null);
            }

            // Check if current user is super user
            if ((!adminUserDN.equals(DNUtils.normalizeDN(ssoToken.getPrincipal().getName()))) && (!user.equals(adminUserId))) {
                out.println(ResourceBundle.getBundle("encode", request.getLocale()).getString("no.permission"));
                ssoToken = null;
            }

        } catch (SSOException e) {
            // If the user has does not have a session force them to authenticate then redirect back here
            response.sendRedirect("UI/Login?goto=../" + currentPageUrl);
            ssoToken = null;

        } catch (IdRepoException e) {
            // If the SSOToken's universal identifier is invalid
            String errorMsgTemplate = ResourceBundle.getBundle("encode", request.getLocale()).getString("invalid.uid");
            out.println(MessageFormat.format(errorMsgTemplate, "UI/Logout?goto=../" + currentPageUrl));
            ssoToken = null;
        }

        return ssoToken;
    }

%>

