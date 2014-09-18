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
 * $Id: SSOTokenPrincipal.java,v 1.2 2009/11/12 18:37:35 veiming Exp $
 */

package com.sun.identity.rest;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.internal.server.AuthSPrincipal;
import com.sun.identity.shared.Constants;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;

public class SSOTokenPrincipal implements Principal, ISubjectable {
    private SSOToken ssoToken;

    public SSOTokenPrincipal(SSOToken ssoToken) {
        this.ssoToken = ssoToken;
    }

    public String getName() {
        return ssoToken.getTokenID().toString();
    }

    public Subject createSubject() 
        throws Exception {
        Set<Principal> userPrincipals = new HashSet<Principal>(2);
        String uuid = ssoToken.getProperty(Constants.UNIVERSAL_IDENTIFIER);
        userPrincipals.add(new AuthSPrincipal(uuid));

        Set privateCred = new HashSet();
        privateCred.add(ssoToken);
        return new Subject(false, userPrincipals, new HashSet(),
            privateCred);
    }
}
