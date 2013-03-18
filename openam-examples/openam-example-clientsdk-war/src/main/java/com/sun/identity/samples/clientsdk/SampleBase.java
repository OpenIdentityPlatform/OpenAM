/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SampleBase.java,v 1.2 2008/06/25 05:41:09 qcheng Exp $
 *
 */

package com.sun.identity.samples.clientsdk;

import com.sun.identity.authentication.AuthContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.io.PrintWriter;
import javax.servlet.http.HttpServlet;

public abstract class SampleBase extends HttpServlet {

    protected AuthContext authenticate(
        String orgname,
        String username,
        String password,
        PrintWriter out
    ) throws Exception
    {
        // Authenticate the user and obtain SSO Token
        AuthContext lc = new AuthContext(orgname);
        lc.login();
        while (lc.hasMoreRequirements()) {
            Callback[] callbacks = lc.getRequirements();
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    NameCallback nc = (NameCallback) callbacks[i];
                    nc.setName(username);
                } else if (callbacks[i] instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) callbacks[i];
                    pc.setPassword(password.toCharArray());
                } else {
                    out.println("Unknow Callback: " + callbacks[i]);
                    out.println("</body></html>");
                    return null;
                }
            }
            lc.submitRequirements(callbacks);
        }

        if (lc.getStatus() != AuthContext.Status.SUCCESS) {
            out.println("Invalid credentials");
            out.println("</body></html>");
            return null;
        }

        return lc;
    }
}
