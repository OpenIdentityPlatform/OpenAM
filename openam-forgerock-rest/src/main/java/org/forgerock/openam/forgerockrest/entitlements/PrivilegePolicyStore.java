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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;

/**
 * Stores entitlements policies using the {@link PrivilegeManager}.
 *
 * @since 12.0.0
 */
final class PrivilegePolicyStore implements PolicyStore {

    private final PrivilegeManager privilegeManager;

    PrivilegePolicyStore(final PrivilegeManager privilegeManager) {
        this.privilegeManager = privilegeManager;
    }

    @Override
    public Privilege read(String policyName) throws EntitlementException {
        return privilegeManager.getPrivilege(policyName);
    }

    @Override
    public Privilege create(Privilege policy) throws EntitlementException {
        try {
            privilegeManager.addPrivilege(policy);
        } catch (ClassCastException ex) {
            throw new UnsupportedOperationException("Only concrete Privilege instances are supported");
        }
        return policy;
    }

    @Override
    public Privilege update(Privilege policy) throws EntitlementException {
        privilegeManager.modifyPrivilege(policy);
        return policy;
    }

    @Override
    public void delete(String policyName) throws EntitlementException {
        privilegeManager.removePrivilege(policyName);
    }
}
