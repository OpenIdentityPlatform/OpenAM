/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock, Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.agents.jboss;

import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.filter.AmFilterManager;
import com.sun.identity.agents.filter.IJ2EEAuthenticationHandler;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Initiates the login process in J2EE_POLICY/ALL mode using the Java EE 6 provided request#login method.
 *
 * @author Peter Major
 */
public class JBossJ2EEAuthHandler implements IJ2EEAuthenticationHandler {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean authenticate(String userName, String password, HttpServletRequest request, HttpServletResponse response, Object extraData) {
        HttpSession session = request.getSession(true);
        IModuleAccess modAccess = AmFilterManager.getModuleAccess();
        try {
            request.login(userName, password);
            if (modAccess.isLogMessageEnabled()) {
                modAccess.logMessage("JBossJ2EEAuthHandler: Successful JAAS login using Servlet API");
            }
            return true;
        } catch (ServletException se) {
            if (modAccess.isLogMessageEnabled()) {
                modAccess.logMessage("JBossJ2EEAuthHandler: Failed to log in to JAAS realm", se);
            }
            session.invalidate();
        }
        return false;
    }
}
