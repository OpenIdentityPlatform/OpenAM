/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SessionService.java,v 1.37 2010/02/03 03:52:54 bina Exp $
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */

package com.iplanet.dpro.session.service;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.security.AdminDNAction;
import com.sun.identity.security.AdminPasswordAction;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.AccessController;

/**
 * Responsible for providing a cached SSOToken for the DSAME admin user.
 *
 * Contains logic extracted from SessionService class as part of first-pass
 * refactoring to improve SessionService adherence to SRP.
 *
 * @since 13.0.0
 */
/*
 * Further refactoring is warranted.
 */
@Singleton
public class DsameAdminTokenProvider {

    private final SSOTokenManager ssoTokenManager;
    private volatile SSOToken adminToken = null;

    private final String dsameAdminDN;
    private final String dsameAdminPassword;

    @Inject
    DsameAdminTokenProvider(SSOTokenManager ssoTokenManager) {
        this.ssoTokenManager = ssoTokenManager;
        dsameAdminDN = (String) AccessController.doPrivileged(new AdminDNAction());
        dsameAdminPassword = (String) AccessController.doPrivileged(new AdminPasswordAction());
    }

    String getDsameAdminDN() {
        return dsameAdminDN;
    }

    SSOToken getAdminToken() throws SSOException {
        if (adminToken == null) {
            adminToken = ssoTokenManager.createSSOToken(new AuthPrincipal(dsameAdminDN), dsameAdminPassword);
        }
        return adminToken;
    }
}
