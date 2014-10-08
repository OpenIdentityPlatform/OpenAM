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
 * Copyright 2014 ForgeRock AS.
 */

package com.sun.identity.entitlement.xacml3.validation;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ReferralPrivilege;

import javax.inject.Inject;
import java.util.Set;

/**
 * Validator for privileges intended for use with XACML based operations.
 *
 * @since 12.0.0
 */
public class PrivilegeValidator {
    private final RealmValidator realmValidator;


    /**
     * Create instance of this validator.
     * @param realmValidator Non null.
     */
    @Inject
    public PrivilegeValidator(RealmValidator realmValidator) {
        this.realmValidator = realmValidator;
    }

    /**
     * Apply validation logic to a Privilege.
     *
     * Note: No validation logic is performed at the moment.
     *
     * @param privilege The Privilege.
     *
     * @throws EntitlementException Not thrown.
     */
    public void validatePrivilege(Privilege privilege) throws EntitlementException {
        // Currently does nothing.
    }

    /**
     * Apply validation logic to a {@link ReferralPrivilege}.
     *
     * Ensure that the ReferralPrivilege points to an existing Realm.
     *
     * @param referralPrivilege Non null ReferralPrivilege to validate.
     *
     * @throws EntitlementException If the ReferralPrivilege points to one or more
     * non-existent realms, or this method is unable to verify that such named realms exist.
     */
    public void validateReferralPrivilege(ReferralPrivilege referralPrivilege) throws EntitlementException {
        Set<String> referralPrivilegeRealms = referralPrivilege.getRealms();
        realmValidator.validateRealms(referralPrivilegeRealms);
    }
}
