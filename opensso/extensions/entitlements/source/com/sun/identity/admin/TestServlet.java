/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: TestServlet.java,v 1.6 2009/06/24 19:23:46 farble1670 Exp $
 */
package com.sun.identity.admin;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ReferralPrivilegeManager;
import com.sun.identity.entitlement.opensso.OpenSSOPrivilege;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import java.io.IOException;
import java.io.PrintWriter;
import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String action = request.getParameter("action");

            if (action == null) {
                throw new ServletException("no action specified");
            }
            if (action.equals("privilege.create")) {
                privilegeCreateAction(request, out);
            } else if (action.equals("referral.create")) {
                referralCreateAction(request, out);
            }
        } catch (EntitlementException ee) {
            throw new ServletException(ee);
        } finally {
            out.close();
        }
    }

    private void privilegeCreateAction(HttpServletRequest request, PrintWriter out) throws ServletException, EntitlementException {
        int n = 1;
        if (request.getParameter("n") != null) {
            n = Integer.parseInt(request.getParameter("n"));
        }
        String template = request.getParameter("template");
        if (template == null) {
            throw new ServletException("no privilege template specified");
        }
        String realm = request.getParameter("realm");
        if (realm == null) {
            realm = "/";
        }

        PrivilegeManager pm = getPrivilegeManager(request, realm);
        Privilege p = pm.getPrivilege(template);
        if (p == null) {
            throw new ServletException("template privilege did not exist");
        }
        for (int i = 0; i < n; i++) {
            String name = "policy" + System.currentTimeMillis();
            OpenSSOPrivilege op = new OpenSSOPrivilege();
            op.setName(name);
            op.setEntitlement(p.getEntitlement());
            op.setSubject(p.getSubject());
            op.setCondition(p.getCondition());
            op.setResourceAttributes(p.getResourceAttributes());
            op.setDescription("created by test servlet");
            out.print("creating privilege: " + name + " ... ");
            pm.addPrivilege(op);
            out.println("done");
        }
    }

    private void referralCreateAction(HttpServletRequest request, PrintWriter out) throws ServletException, EntitlementException {
        int n = 1;
        if (request.getParameter("n") != null) {
            n = Integer.parseInt(request.getParameter("n"));
        }
        String template = request.getParameter("template");
        if (template == null) {
            throw new ServletException("no referral template specified");
        }
        String realm = request.getParameter("realm");
        if (realm == null) {
            realm = "/";
        }

        ReferralPrivilegeManager rpm = getReferralPrivilegeManager(request, realm);
        ReferralPrivilege rp = rpm.getReferral(template);
        if (rp == null) {
            throw new ServletException("template referral did not exist");
        }
        for (int i = 0; i < n; i++) {
            String name = "referral" + System.currentTimeMillis();
            ReferralPrivilege newRp = new ReferralPrivilege(
                    name, rp.getMapApplNameToResources(), rp.getRealms());
            newRp.setDescription("created by test servlet");
            out.print("creating referral: " + name + " ... ");
            rpm.add(newRp);
            out.println("done");
        }
    }

    private PrivilegeManager getPrivilegeManager(HttpServletRequest request, String realm) throws ServletException {
        try {
            SSOToken t = SSOTokenManager.getInstance().createSSOToken(request);
            Subject s = SubjectUtils.createSubject(t);
            PrivilegeManager pm = PrivilegeManager.getInstance(realm, s);

            return pm;
        } catch (SSOException ssoe) {
            throw new ServletException(ssoe);
        }
    }

    private ReferralPrivilegeManager getReferralPrivilegeManager(HttpServletRequest request, String realm) throws ServletException {
        try {
            SSOToken t = SSOTokenManager.getInstance().createSSOToken(request);
            Subject s = SubjectUtils.createSubject(t);
            ReferralPrivilegeManager rpm = new ReferralPrivilegeManager(realm, s);

            return rpm;
        } catch (SSOException ssoe) {
            throw new ServletException(ssoe);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
