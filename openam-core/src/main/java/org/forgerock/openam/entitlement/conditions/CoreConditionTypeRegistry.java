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

package org.forgerock.openam.entitlement.conditions;

import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementSubject;
import org.forgerock.openam.entitlement.ConditionTypeRegistry;
import org.forgerock.openam.entitlement.conditions.environment.OAuth2ScopeCondition;
import org.forgerock.openam.entitlement.conditions.subject.AMIdentitySubject;
import org.forgerock.openam.entitlement.conditions.subject.AuthenticatedUsers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Core implementation of the {@link ConditionTypeRegistry}.
 *
 * @since 12.0.0
 */
public class CoreConditionTypeRegistry implements ConditionTypeRegistry {

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Class<? extends EntitlementSubject>> getSubjectConditions() {
        Set<Class<? extends EntitlementSubject>> conditions = new HashSet<Class<? extends EntitlementSubject>>();

        conditions.add(AMIdentitySubject.class);
        conditions.add(AuthenticatedUsers.class);

        return conditions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Class<? extends EntitlementCondition>> getEnvironmentConditions() {

        Set<Class<? extends EntitlementCondition>> conditions = new HashSet<Class<? extends EntitlementCondition>>();

        conditions.add(OAuth2ScopeCondition.class);

        return conditions;
    }
}
