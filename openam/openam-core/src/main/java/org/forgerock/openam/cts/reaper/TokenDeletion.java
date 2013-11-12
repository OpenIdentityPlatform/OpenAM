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

import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.impl.LDAPAdapter;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.ResultHandler;
import org.forgerock.opendj.ldap.responses.Result;

import javax.inject.Inject;
import java.io.Closeable;
import java.util.Collection;

/**
 * Responsible for signalling batches of tokens to be deleted using the asynchronous
 * deletion mechanism.
 *
 * This class manages the collection of a connection from the ConnectionFactory, and
 * provides a means of releasing that connection once complete.
 *
 * The caller will decide when to close the connection because we need to ensure that
 * all async deletion operations have been completed before closing the connection.
 *
 * @author robert.wapshott@forgerock.com
 */
public class TokenDeletion implements Closeable {
    // Injected
    private final LDAPAdapter adapter;
    private final ConnectionFactory factory;

    private Connection connection;

    @Inject
    public TokenDeletion(LDAPAdapter adapter, ConnectionFactory factory) {
        this.adapter = adapter;
        this.factory = factory;
    }

    /**
     * @return A connection from the ConnectionFactory if one was available.
     */
    private Connection getConnection() throws ErrorResultException {
        if (connection == null) {
            connection = factory.getConnection();
        }
        return connection;
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
    public void deleteBatch(Collection<Entry> entries, ResultHandler<Result> resultHandler) throws ErrorResultException {
        for (Entry entry : entries) {
            String tokenId = entry.getAttribute(CoreTokenField.TOKEN_ID.toString()).firstValueAsString();
            adapter.deleteAsync(getConnection(), tokenId, resultHandler);
        }
    }

    /**
     * Release the connection which was acquired by this TokenDeletion.
     */
    public void close() {
        IOUtils.closeIfNotNull(connection);
        connection = null;
    }
}
