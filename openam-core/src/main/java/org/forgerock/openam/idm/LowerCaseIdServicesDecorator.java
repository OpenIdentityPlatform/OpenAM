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
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdServices;
import com.sun.identity.idm.IdType;

import java.util.Map;
import java.util.Set;

import static org.forgerock.openam.idm.IdServicesDecoratorUtils.toLowerCaseKeys;

/**
 * Implementation of the {@link com.sun.identity.idm.IdServices} interface that just ensures all attribute names are
 * lower case to ensure API consistency.
 *
 * @since 12.0.0
 * @see <a href="https://bugster.forgerock.org/jira/browse/OPENAM-3159">OPENAM-3159</a>
 */
public class LowerCaseIdServicesDecorator extends IdServicesDecorator {
    /**
     * Constructs the lower-case decorator using the given delegate IdServices implementation for actual work.
     *
     * @param delegate the delegate service instance to use. Cannot be null.
     * @throws NullPointerException if the delegate is null.
     */
    public LowerCaseIdServicesDecorator(IdServices delegate) {
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
