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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.rest;

import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.providers.dpro.SSOPrincipal;
import org.forgerock.json.resource.AbstractConnectionWrapper;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.util.promise.Promise;

import javax.inject.Inject;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

/**
 * CREST connection factory wrapper that elevates in coming contexts
 * with an admin user for the duration of the connection.
 *
 * @since 13.0.0
 */
public final class ElevatedConnectionFactoryWrapper implements ConnectionFactory {

    private final ConnectionFactory connectionFactory;
    private final PrivilegedAction<SSOToken> ssoTokenPrivilegedAction;
    private final SSOTokenContext.Factory ssoTokenContextFactory;

    @Inject
    public ElevatedConnectionFactoryWrapper(ConnectionFactory connectionFactory,
            PrivilegedAction<SSOToken> ssoTokenPrivilegedAction, SSOTokenContext.Factory ssoTokenContextFactory) {
        this.connectionFactory = connectionFactory;
        this.ssoTokenPrivilegedAction = ssoTokenPrivilegedAction;
        this.ssoTokenContextFactory = ssoTokenContextFactory;
    }

    @Override
    public Connection getConnection() throws ResourceException {
        Connection connection = connectionFactory.getConnection();
        SSOToken ssoToken = ssoTokenPrivilegedAction.run();
        return new ElevatedConnection(connection, ssoToken);
    }

    @Override
    public Promise<Connection, ResourceException> getConnectionAsync() {
        try {
            return newResultPromise(getConnection());
        } catch (ResourceException rE) {
            return newExceptionPromise(rE);
        }
    }

    @Override
    public void close() {
        connectionFactory.close();
    }

    private final class ElevatedConnection extends AbstractConnectionWrapper<Connection> {

        private final String authenticationId;
        private final Map<String, Object> authorisation;

        ElevatedConnection(Connection connection, SSOToken adminToken) {
            super(connection);

            try {
                SSOPrincipal ssoPrincipal = (SSOPrincipal) adminToken.getPrincipal();
                authenticationId = ssoPrincipal.getName();
                authorisation = new HashMap<>();
                authorisation.put("authLevel", adminToken.getAuthLevel());
                authorisation.put("tokenId", adminToken.getTokenID().toString());
            } catch (SSOException ssoE) {
                throw new SecurityException("Unable to create security context", ssoE);
            }
        }

        @Override
        protected Context transform(Context context) {
            return ssoTokenContextFactory.create(new SecurityContext(context, authenticationId, authorisation));
        }

    }

}
