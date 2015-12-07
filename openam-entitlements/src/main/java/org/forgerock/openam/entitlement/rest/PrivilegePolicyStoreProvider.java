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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.rest;

import org.forgerock.openam.entitlement.service.PrivilegeManagerFactory;
import org.forgerock.services.context.Context;
import org.forgerock.openam.entitlement.rest.query.QueryAttribute;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;
import java.util.Map;

/**
 * A policy store provider that returns {@link PrivilegePolicyStore} instances.
 *
 * @since 12.0.0
 */
public final class PrivilegePolicyStoreProvider implements PolicyStoreProvider {
    public static final String POLICY_QUERY_ATTRIBUTES = "PolicyQueryAttributes";

    private final PrivilegeManagerFactory factory;
    private final Map<String, QueryAttribute> queryAttributes;

    /**
     * Constructs a policy store provider that looks up privilege managers using the given abstract factory.
     *
     * @param factory a non-null privilege manager factory.
     * @param queryAttributes the set of query attributes to allow in queries.
     */
    @Inject
    public PrivilegePolicyStoreProvider(PrivilegeManagerFactory factory,
            @Named(POLICY_QUERY_ATTRIBUTES) Map<String, QueryAttribute> queryAttributes) {
        Reject.ifNull(factory, queryAttributes);
        this.factory = factory;
        this.queryAttributes = queryAttributes;
    }

    @Override
    public PolicyStore getPolicyStore(Context context) {
        Subject adminSubject = context.asContext(SubjectContext.class).getCallerSubject();
        String realm = context.asContext(RealmContext.class).getResolvedRealm();

        return getPolicyStore(adminSubject, realm);
    }

    public PolicyStore getPolicyStore(Subject adminSubject, String realm) {
        if (realm.isEmpty()) {
            realm = "/";
        }

        return new PrivilegePolicyStore(factory.get(realm, adminSubject), queryAttributes);
    }

}
