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

import com.google.inject.Inject;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import org.forgerock.openam.utils.CollectionUtils;

import java.util.Collection;
import java.util.Set;

/**
 * Validates that the given Realm is valid.
 *
 * @since 12.0.0
 */
public class RealmValidator {
    public static final String ROOT = "/";
    private final OrganizationConfigManager manager;

    @Inject
    public RealmValidator(OrganizationConfigManager manager) {
        this.manager = manager;
    }

    /**
     * @param realms Non null but possibly empty collection.
     * @return True if collection is empty, or if the Realm exists in the framework.
     */
    public void validateRealms(Collection<String> realms) throws EntitlementException {
        Collection<String> all = getAllRealmNames();
        for (String realm : realms) {
            if (!all.contains(realm)) {
                throw new EntitlementException(EntitlementException.REALM_NOT_FOUND, new Object[]{realm});
            }
        }
    }

    /**
     * Fetch all known Realm names.
     *
     * @return Non null, non empty collection.
     *
     * @throws EntitlementException If there was a problem querying the Realm names.
     */
    private Collection<String> getAllRealmNames() throws EntitlementException {
        try {
            Set<String> realms = CollectionUtils.asOrderedSet(ROOT);

            for (Object realm : manager.getSubOrganizationNames("*", true)) {
                realms.add(ROOT + realm.toString());
            }
            return realms;
        } catch (SMSException e) {
            throw new EntitlementException(EntitlementException.INTERNAL_ERROR, e);
        }
    }
}
