/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.idm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdCachedServices;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;

import java.util.Map;
import java.util.Set;

import static org.forgerock.openam.idm.IdServicesDecoratorUtils.toLowerCaseKeys;

/**
 * Version of {@link LowerCaseIdServicesDecorator} that also implements the {@link com.sun.identity.idm.IdCachedServices}
 * interface.
 *
 * @since 12.0.0
 */
public class LowerCaseIdCachedServicesDecorator extends IdCachedServicesDecorator {
    /**
     * Constructs the decorator using the given delegate implementation.
     *
     * @param delegate a non-null IdServices implementation to delegate calls to.
     * @throws NullPointerException if the delegate is null.
     */
    public LowerCaseIdCachedServicesDecorator(IdCachedServices delegate) {
        super(delegate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map getAttributes(SSOToken token, IdType type, String name, Set attrNames, String amOrgName, String amsdkDN,
                             boolean isString) throws IdRepoException, SSOException {
        return toLowerCaseKeys(super.getAttributes(token, type, name, attrNames, amOrgName, amsdkDN, isString));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map getAttributes(SSOToken token, IdType type, String name, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException {
        return toLowerCaseKeys(super.getAttributes(token, type, name, amOrgName, amsdkDN));
    }
}
