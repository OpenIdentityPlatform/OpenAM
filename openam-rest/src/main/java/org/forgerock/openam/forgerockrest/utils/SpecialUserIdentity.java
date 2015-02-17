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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.utils;

import com.iplanet.sso.SSOToken;

/**
 * Interface representing concerns of determining if token corresponds to the 'special' user. Encapsulates
 * consumption of AuthD.isSpecialUser to aid in unit-tests. Note that the SSOToken returned from
 * AccessController.doPrivileged(AdminTokenAction.getInstance()) corresponds to the 'special' user.
 */
public interface SpecialUserIdentity {
    /**
     * @param token corresponding to authenticated user
     * @return true if the user is a special user; false is returned if it is not the case, or if an exception occurred
     * making this determination.
     */
    boolean isSpecialUser(SSOToken token);
}
