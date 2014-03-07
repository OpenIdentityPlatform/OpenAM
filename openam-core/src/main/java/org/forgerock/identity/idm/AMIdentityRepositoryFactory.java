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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.identity.idm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;

/**
 * AMIdentityRepositoryFactory creates a AMIdentityRepository
 */
public class AMIdentityRepositoryFactory {

    private AMIdentityRepositoryFactory() {
        //prevent instantiation
    }

    /**
     * Creates an {@link  com.sun.identity.idm.AMIdentityRepository AMIdentityRepository}
     * @param token {@link  com.iplanet.sso.SSOToken SSOToken} to create repository with.
     * @param realm Realm the repository is in.
     * @return An {@link  com.sun.identity.idm.AMIdentityRepository AMIdentityRepository} for the given realm
     * @throws IdRepoException
     * @throws SSOException
     */
    public static AMIdentityRepository createAMIdentityRepository(SSOToken token, String realm)
            throws IdRepoException, SSOException {
        return new AMIdentityRepository(token, realm);
    }
}
