/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.uma;

import static org.forgerock.json.JsonValue.*;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sun.identity.entitlement.JwtPrincipal;
import com.sun.identity.idm.AMIdentity;

/**
 * Utility methods for UMA.
 *
 * @since 13.0.0
 */
public class UmaUtils {

    /**
     * Creates a {@code Subject} using the universal ID from the provided
     * {@code AMIdentity}.
     *
     * @param identity The {@code AMIdentity}.
     * @return A {@code Subject}.
     */
    public static Subject createSubject(AMIdentity identity) {
        JwtPrincipal principal = new JwtPrincipal(json(object(field("sub", identity.getUniversalId()))));
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(principal);
        return new Subject(false, principals, Collections.emptySet(), Collections.emptySet());
    }
}
