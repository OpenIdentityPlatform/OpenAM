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

import com.sun.identity.entitlement.PrivilegeManager;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryAttribute;
import org.forgerock.openam.rest.resource.RealmContext;
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
    public PrivilegePolicyStoreProvider(PrivilegeManagerFactory factory,
                                        Map<String, QueryAttribute> queryAttributes) {
        Reject.ifNull(factory, queryAttributes);
        this.factory = factory;
        this.queryAttributes = queryAttributes;
    }

    /**
     * Constructs a policy store provider that looks up privilege managers using the standard
     * {@link PrivilegeManager#getInstance(String, javax.security.auth.Subject)} method. Uses the given query
     * attribute definitions.
     */
    @Inject
    public PrivilegePolicyStoreProvider(@Named(POLICY_QUERY_ATTRIBUTES) Map<String, QueryAttribute> queryAttributes) {
        this(new DefaultPrivilegeManagerFactory(), queryAttributes);
    }

    @Override
    public PolicyStore getPolicyStore(ServerContext context) {
        Subject adminSubject = context.asContext(SubjectContext.class).getCallerSubject();
        String realm = context.asContext(RealmContext.class).getResolvedRealm();

        return getPolicyStore(adminSubject, realm);
    }

    public PolicyStore getPolicyStore(Subject adminSubject, String realm) {
        if (realm.isEmpty()) {
            realm = "/";
        }

        return new PrivilegePolicyStore(factory.getPrivilegeManager(realm, adminSubject), queryAttributes);
    }

    /**
     * Abstract factory for getting hold of actual privilege manager instances.
     */
    public interface PrivilegeManagerFactory {
        /**
         * Gets a privilege manager for the given realm and admin subject.
         *
         * @param realm the realm to manage privileges for.
         * @param adminSubject the subject to use to perform management actions.
         * @return an appropriate privilege manager.
         */
        PrivilegeManager getPrivilegeManager(String realm, Subject adminSubject);
    }

    private static class DefaultPrivilegeManagerFactory implements PrivilegeManagerFactory {
        @Override
        public PrivilegeManager getPrivilegeManager(String realm, Subject adminSubject) {
            return PrivilegeManager.getInstance(realm, adminSubject);
        }
    }
}
