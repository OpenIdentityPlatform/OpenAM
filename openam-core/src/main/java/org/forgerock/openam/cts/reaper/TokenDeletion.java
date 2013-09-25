/**
 * Copyright 2013 ForgeRock AS.
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
package org.forgerock.openam.cts.reaper;

import javax.inject.Inject;
import com.google.inject.name.Named;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.impl.LDAPAdapter;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.ResultHandler;
import org.forgerock.opendj.ldap.responses.Result;

import java.util.Collection;

/**
 * Responsible for signalling batches of tokens to be deleted using the asynchronous
 * deletion mechanism.
 *
 * @author robert.wapshott@forgerock.com
 */
public class TokenDeletion {
    private final LDAPAdapter adapter;
    private final ConnectionFactory factory;
    private Debug debug;

    @Inject
    public TokenDeletion(LDAPAdapter adapter, ConnectionFactory factory,
                         @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {
        this.adapter = adapter;
        this.factory = factory;
        this.debug = debug;
    }

    /**
     * Performs a delete against a batch of Token IDs in the search results.
     *
     * This operation requires that the connection factory provides a valid connection.
     * If this fails then the operation will return early.
     *
     * @param entries Non null collection of search results which contain
     *                Token ID attributes.
     * @param resultHandler The result handler which will be triggered when the deletion
     *                      has been completed.
     */
    public void deleteBatch(Collection<Entry> entries, ResultHandler<Result> resultHandler) {
        Connection connection = null;
        try {
            connection = factory.getConnection();
            for (Entry entry : entries) {
                String tokenId = entry.getAttribute(CoreTokenField.TOKEN_ID.toString()).firstValueAsString();
                adapter.deleteAsync(connection, tokenId, resultHandler);
            }
        } catch (ErrorResultException e) {
            debug.error("Failed to get a connection", e);
            return;
        } finally {
            IOUtils.closeIfNotNull(connection);
        }
    }
}
