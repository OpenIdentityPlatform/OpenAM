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

   $Id: FileUpload.jsp,v 1.4 2009/08/07 23:39:08 asyhuang Exp $

--%>
<%--
   Portions Copyrighted 2012 ForgeRock Inc
   Portions Copyrighted 2012 Open Source Solution Technology Corporation
--%>

<%@page import="com.iplanet.sso.SSOException"%>
<%@page import="com.iplanet.sso.SSOToken"%>
<%@page import="com.iplanet.sso.SSOTokenManager"%>
<%@page import="java.io.*" %>
<%@page import="java.net.*" %>
<%@page import="java.util.*" %>

<%
    request.setCharacterEncoding("UTF-8");
    String locale = request.getParameter("locale");
    Locale resLocale = null;
    if ((locale != null) && (locale.length() > 0)) {
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

    try {
        SSOTokenManager manager = SSOTokenManager.getInstance();
        SSOToken ssoToken = manager.createSSOToken(request);

        if (!manager.isValidToken(ssoToken)) {
            return;
        }
    } catch (SSOException ssoe) {
        String redirectUrl = request.getScheme() + "://" +
                request.getServerName() + ":" +
                request.getServerPort() +
                request.getContextPath();
        response.sendRedirect(redirectUrl);
        return;
    }


    InputStream is = null;

    try {
        boolean limitExceeded = false;
        StringBuffer buff = new StringBuffer();
        is = request.getInputStream();
        BufferedReader bos = new BufferedReader(new InputStreamReader(is));
        String line = bos.readLine();
        while (line != null) {
            buff.append(line).append("\n");
            line = bos.readLine();
            if (buff.length() > (1024 * 50)) {
                limitExceeded = true;
                break;
            }

        }

        if (limitExceeded) {
            ResourceBundle rb = null;
            String RB_NAME = "workflowMessages";
            com.sun.identity.shared.debug.Debug debug =
                    com.sun.identity.shared.debug.Debug.getInstance("workflowMessages");
            rb = ResourceBundle.getBundle(RB_NAME, resLocale);
            String data = com.sun.identity.shared.locale.Locale.getString(
                    rb, "file.upload.size.limit.exceeded", debug);
            out.println("<div id=\"data\">" + "Error: " + data + "</div>");
        } else {
            // Parses a content-type String for the boundary.
            String contentType = request.getContentType();
            if (contentType == null) {
                contentType = request.getHeader("Content-Type");
            }
            String boundary = "";
            if (contentType != null && contentType.lastIndexOf("boundary=") != -1) {
                boundary = contentType.substring(contentType.lastIndexOf("boundary=") + 9);
                if (boundary.endsWith("\n")) {
                    boundary = boundary.substring(0, boundary.length()-1);
                }
            }


            String data = buff.toString();
            int idx = data.indexOf("filename=\"");
            idx = data.indexOf("\n\n", idx);
            data = data.substring(idx + 2);
            idx = data.lastIndexOf("\n--" + boundary);
            data = data.substring(0, idx);
            data = data.replace("<", "&lt;");
            data = data.replace(">", "&gt;");
            out.println("<div id=\"data\">" + data + "</div>");
        }
    } catch (IOException e) {
    } finally {
        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            //ignore
        }
    }
%>
