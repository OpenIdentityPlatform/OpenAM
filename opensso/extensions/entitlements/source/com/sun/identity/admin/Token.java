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
 * $Id: Token.java,v 1.4 2009/06/09 22:40:36 farble1670 Exp $
 */

package com.sun.identity.admin;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import javax.faces.context.FacesContext;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

public class Token {
    private HttpServletRequest request;

    public Token() {
        request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }

    public Token(HttpServletRequest request) {
        this.request = request;
    }
    
    public SSOToken getSSOToken() {
        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken ssoToken = manager.createSSOToken(request);
            manager.validateToken(ssoToken);
            return ssoToken;
        } catch (SSOException ssoe) {
            throw new RuntimeException(ssoe);
        }
    }

    public SSOToken getAdminSSOToken() {
        return (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    public Subject getAdminSubject() {
        Subject adminSubject = SubjectUtils.createSubject(getAdminSSOToken());
        return adminSubject;
    }
}
