/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS. All rights reserved.
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

package org.forgerock.openam.authentication.modules.common.mapping;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import java.util.Map;
import java.util.Set;

/**
 * Implementations of this interface provide the means to search for and create users given a map of attributes.
 * @see org.forgerock.openam.authentication.modules.common.mapping.DefaultAccountProvider
 *
 * @supported.all.api
 */
public interface AccountProvider {

    /**
     * Search for a user given a map of attributes.
     * @param idrepo The identity repository.
     * @param attr The set of attributes, which should be treated as 'or' statements.
     * @return The first matching user found.
     */
    AMIdentity searchUser(AMIdentityRepository idrepo, Map<String, Set<String>> attr);

    /**
     * Provisions a user with the specified attributes.
     * @param idrepo The identity repository in which the user will be created.
     * @param attributes The user attributes.
     * @return The created user identity.
     * @throws AuthLoginException Thrown if user creation fails.
     */
    AMIdentity provisionUser(AMIdentityRepository idrepo, Map<String, Set<String>> attributes) throws AuthLoginException;
    
}
