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
 * $Id: AdminTokenIdImpl.java,v 1.1 2009/11/20 19:50:43 huacui Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.security;

import com.iplanet.sso.SSOToken;
import java.security.AccessController;

/**
 * This class implements interface <code>AdminTokenId</code>
 */
public class AdminTokenIdImpl implements AdminTokenId {

    /**
     * Returns an admin token Id
     *
     * @return admin token Id
     */
    public String getAdminTokenId() {
        String tokenId = null;
        SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
                                  AdminTokenAction.getInstance());
        if (adminToken != null) {
            tokenId = adminToken.getTokenID().toString();
        }
        return tokenId; 
    }
}
