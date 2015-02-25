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
 * $Id: SubjectUtils.java,v 1.1 2009/08/19 05:40:36 veiming Exp $
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.internal.server.AuthSPrincipal;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import java.security.AccessController;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;

public class SubjectUtils {
    private SubjectUtils() {
    }

    public static Subject createSuperAdminSubject() {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        return createSubject(adminToken);
    }

    public static Subject createSubject(SSOToken token) {
        try {
            Set<Principal> userPrincipals = new HashSet<Principal>(2);
            String uuid = token.getProperty(Constants.UNIVERSAL_IDENTIFIER);
            userPrincipals.add(new AuthSPrincipal(uuid));

            Set privateCred = new HashSet();
            privateCred.add(token);
            return new Subject(false, userPrincipals, new HashSet(),
                privateCred);
        } catch (SSOException ex) {
            PrivilegeManager.debug.error("SubjectUtils.createSubject", ex);
            return null;
        }
    }

    public static SSOToken getSSOToken(Subject subject) {
        Set set = subject.getPrivateCredentials();
        if ((set == null) || set.isEmpty()) {
            return null;
        }
        for (Object o : set) {
            if (o instanceof SSOToken) {
                return (SSOToken)o;
            }
        }
        return null;
    }

    public static String getPrincipalId(Subject subject) {
        Set<Principal> userPrincipals = subject.getPrincipals();
        return ((userPrincipals != null) && !userPrincipals.isEmpty()) ?
            userPrincipals.iterator().next().getName() : null;
    }
}
