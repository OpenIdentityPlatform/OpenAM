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
import com.sun.identity.entitlement.JwtClaimSubject;
import org.forgerock.openam.entitlement.ConditionTypeRegistry;
import org.forgerock.openam.entitlement.conditions.environment.AMIdentityMembershipCondition;
import org.forgerock.openam.entitlement.conditions.environment.AuthLevelCondition;
import org.forgerock.openam.entitlement.conditions.environment.AuthSchemeCondition;
import org.forgerock.openam.entitlement.conditions.environment.AuthenticateToRealmCondition;
import org.forgerock.openam.entitlement.conditions.environment.AuthenticateToServiceCondition;
import org.forgerock.openam.entitlement.conditions.environment.IPv4Condition;
import org.forgerock.openam.entitlement.conditions.environment.IPv6Condition;
import org.forgerock.openam.entitlement.conditions.environment.LDAPFilterCondition;
import org.forgerock.openam.entitlement.conditions.environment.LEAuthLevelCondition;
import org.forgerock.openam.entitlement.conditions.environment.OAuth2ScopeCondition;
import org.forgerock.openam.entitlement.conditions.environment.ResourceEnvIPCondition;
import org.forgerock.openam.entitlement.conditions.environment.SessionCondition;
import org.forgerock.openam.entitlement.conditions.environment.SessionPropertyCondition;
import org.forgerock.openam.entitlement.conditions.environment.SimpleTimeCondition;
import org.forgerock.openam.entitlement.conditions.subject.IdentitySubject;
import org.forgerock.openam.entitlement.conditions.subject.AuthenticatedUsers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Core implementation of {@link ConditionTypeRegistry}.
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

        conditions.add(IdentitySubject.class);
        conditions.add(AuthenticatedUsers.class);
        conditions.add(JwtClaimSubject.class);

        return conditions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Class<? extends EntitlementCondition>> getEnvironmentConditions() {
        Set<Class<? extends EntitlementCondition>> conditions = new HashSet<Class<? extends EntitlementCondition>>();

        conditions.add(OAuth2ScopeCondition.class);
        conditions.add(AuthLevelCondition.class);
        conditions.add(LEAuthLevelCondition.class);
        conditions.add(AuthenticateToServiceCondition.class);
        conditions.add(AuthenticateToRealmCondition.class);
        conditions.add(AMIdentityMembershipCondition.class);
        conditions.add(SessionCondition.class);
        conditions.add(ResourceEnvIPCondition.class);
        conditions.add(SimpleTimeCondition.class);
        conditions.add(SessionPropertyCondition.class);
        conditions.add(AuthSchemeCondition.class);
        conditions.add(IPv4Condition.class);
        conditions.add(IPv6Condition.class);
        conditions.add(LDAPFilterCondition.class);

        return conditions;
    }
}
