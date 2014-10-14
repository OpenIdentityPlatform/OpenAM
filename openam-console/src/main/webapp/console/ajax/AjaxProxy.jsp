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

   $Id: AjaxProxy.jsp,v 1.7 2009/08/04 20:50:49 asyhuang Exp $

--%>
<%--
   Portions Copyrighted 2012-2014 ForgeRock AS
   Portions Copyrighted 2012 Open Source Solution Technology Corporation
--%>

<%@page import="com.iplanet.am.util.SystemProperties"%>
<%@page import="com.iplanet.sso.SSOException"%>
<%@page import="com.iplanet.sso.SSOToken"%>
<%@page import="com.iplanet.sso.SSOTokenManager"%>
<%@page import="com.sun.identity.idm.AMIdentity"%>
<%@page import="com.sun.identity.idm.IdRepoException"%>
<%@page import="com.sun.identity.idm.IdType"%>
<%@page import="com.sun.identity.security.AdminTokenAction"%>
<%@page import="com.sun.identity.console.base.AMViewBeanBase" %>
<%@page import="com.sun.identity.workflow.ITask" %>
<%@page import="com.sun.identity.workflow.WorkflowException" %>
<%@page import="java.security.AccessController"%>
<%@page import="java.util.*" %>
<%@ page import="org.owasp.esapi.ESAPI" %>
<%@ page import="com.sun.identity.shared.debug.Debug" %>

<%
		response.setContentType("text/plain; charset=UTF-8");
        request.setCharacterEncoding("UTF-8");
        String locale = request.getParameter("locale");
        if (!ESAPI.validator().isValidInput("HTTP Parameter Value: " + locale, locale, "HTTPParameterValue", 200,
                true)) {
            locale = null;
        }
        Locale resLocale = null;
        if ((locale != null) && (!locale.isEmpty())) {
            StringTokenizer st = new StringTokenizer(locale, "|");
            int cnt = st.countTokens();
            if (cnt == 1) {
                resLocale = new Locale(st.nextToken());
            } else if (cnt == 2) {
                resLocale = new Locale(st.nextToken(), st.nextToken());
            } else {
                resLocale = new Locale(st.nextToken(), st.nextToken(),
                        st.nextToken());
            }
        } else {
            resLocale = Locale.US;
        }
        

        String amadminUUID = null;
        String adminUser = SystemProperties.get(
                "com.sun.identity.authentication.super.user");
        if (adminUser != null) {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
            AMIdentity adminUserId = new AMIdentity(adminToken, adminUser,
                    IdType.USER, "/", null);
            amadminUUID = adminUserId.getUniversalId();
        }

        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken ssoToken = manager.createSSOToken(request);

            if (!manager.isValidToken(ssoToken)) {
                String redirectUrl = request.getScheme() + "://" +
                        request.getServerName() + ":" +
                        request.getServerPort() +
                        request.getContextPath();
                response.sendRedirect(redirectUrl);
                return;
            }

            AMIdentity user = new AMIdentity(ssoToken);
            if (!user.getUniversalId().equalsIgnoreCase(amadminUUID)) {

                ResourceBundle rb = null;
                String RB_NAME = "workflowMessages";              
                com.sun.identity.shared.debug.Debug debug =
                        com.sun.identity.shared.debug.Debug.getInstance("workflowMessages");
                rb = ResourceBundle.getBundle(RB_NAME, resLocale);
                String msg = com.sun.identity.shared.locale.Locale.getString(
                        rb, "ajax.user.privilege.invalid", debug);
                throw new RuntimeException(msg);
            }

        } catch (SSOException ssoe) {
            String redirectUrl = request.getScheme() + "://" +
                    request.getServerName() + ":" +
                    request.getServerPort() +
                    request.getContextPath();
            response.sendRedirect(redirectUrl);
            return;
        } catch (IdRepoException ex) {
            String redirectUrl = request.getScheme() + "://" +
                    request.getServerName() + ":" +
                    request.getServerPort() +
                    request.getContextPath() +
                    "/base/AMUncaughtException";
            response.sendRedirect(redirectUrl);
            return;
        }

        String clazzName = request.getParameter("class");
        if (clazzName == null || !ESAPI.validator().isValidInput("HTTP Parameter Value: " + clazzName,
                clazzName, "HTTPParameterValue", 2000, false)) {
            String redirectUrl = request.getScheme() + "://" +
                    request.getServerName() + ":" +
                    request.getServerPort() +
                    request.getContextPath() +
                    "/base/AMUncaughtException";
            response.sendRedirect(redirectUrl);
            return;
        }
        try {
            Class clazz = Class.forName(clazzName);
            ITask task = (ITask) clazz.newInstance();

            Map map = new HashMap();
            for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
                String n = (String) e.nextElement();
                if (!n.equals("class") && !n.equals("locale")) {
                    map.put(n, request.getParameter(n));
                }
            }

            map.put("_servlet_context_", getServletConfig().getServletContext());
            map.put("_request_", request);
            out.println("0|" + task.execute(resLocale, map));
        } catch (WorkflowException e) {
            out.write("1|" + AMViewBeanBase.stringToHex(
                    ESAPI.encoder().encodeForHTML(e.getL10NMessage(resLocale))));
        } catch (IllegalAccessException e) {
            out.write("1|" + ESAPI.encoder().encodeForHTML(e.getMessage()));
        } catch (InstantiationException e) {
            out.write("1|" + ESAPI.encoder().encodeForHTML(e.getMessage()));
        } catch (ClassNotFoundException e) {
            out.write("1|" + ESAPI.encoder().encodeForHTML(e.getMessage()));
        } catch (ClassCastException e) {
            out.write("1|" + ESAPI.encoder().encodeForHTML(e.getMessage()));
        } catch (Exception e) {
            Debug.getInstance("workflow").error("Uncaught exception in AjaxProxy", e);
            response.sendRedirect(request.getContextPath() + "/base/AMUncaughtException");
        }

%>
