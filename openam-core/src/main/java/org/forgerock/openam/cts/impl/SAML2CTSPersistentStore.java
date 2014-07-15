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

package org.forgerock.openam.cts.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.adapters.TokenAdapter;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.api.fields.SAMLTokenField;
import org.forgerock.openam.cts.api.tokens.SAMLToken;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.federation.saml2.SAML2TokenRepository;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used in SAML2 failover mode to store/recover serialized
 * state of Assertion/Response object.
 * <p/>
 * This class acts as a Proxy to perform distinct SAML2
 * operations and allow the CTSPersistentStore implementation
 * to handle the actual CRUD for Tokens.
 */
public class SAML2CTSPersistentStore implements SAML2TokenRepository {

    // Injected via Guice
    private final CTSPersistentStore persistentStore;
    private final TokenAdapter<SAMLToken> tokenAdapter;
    private final Debug debug;


    /**
     * Constructs new SAML2CTSPersistentStore,
     * @param persistentStore The CTSPersistentStore implementation to use
     * @param tokenAdapter The SAML2 TokenAdapter implementation to use
     * @param debug The Debug instance to use
     */
    @Inject
    public SAML2CTSPersistentStore(CTSPersistentStore persistentStore, TokenAdapter<SAMLToken> tokenAdapter,
                                   @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {

        this.persistentStore = persistentStore;
        this.tokenAdapter = tokenAdapter;
        this.debug = debug;
        if (debug.messageEnabled()) {
            debug.message("SAML2CTSPersistentStore instance created using persistentStore:"
                    + persistentStore.getClass().getName() + " and tokenAdapter:" + tokenAdapter.getClass().getName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object retrieveSAML2Token(String primaryKey) throws SAML2TokenRepositoryException {

        try {
            // Retrieve the SAML2 Token from the Repository using the primary key.
            Token token = persistentStore.read(primaryKey);
            if (token != null) {
                SAMLToken samlToken = tokenAdapter.fromToken(token);
                return samlToken.getToken();
            } else {
                return null;
            }
        } catch (CoreTokenException e) {
            debug.error("SAML2CTSPersistentStore.retrieveSAML2Token(): failed to retrieve SAML2 " +
                    "token using primary key:" + primaryKey, e);
            throw new SAML2TokenRepositoryException(e.getMessage(), e);
        }
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public List<Object> retrieveSAML2TokensWithSecondaryKey(String secondaryKey) throws SAML2TokenRepositoryException {

        try {
            // Perform a query against the token store with the secondary key.
            Map<CoreTokenField, Object> queryMap = new EnumMap<CoreTokenField, Object>(CoreTokenField.class);
            queryMap.put(SAMLTokenField.SECONDARY_KEY.getField(), secondaryKey);

            TokenFilter filter = new TokenFilterBuilder()
                    .withAttribute(SAMLTokenField.SECONDARY_KEY.getField(), secondaryKey)
                    .build();
            Collection<Token> tokens = persistentStore.query(filter);
            List<Object> results = new ArrayList<Object>(tokens.size());
            for (Token token : tokens) {
                SAMLToken samlToken = tokenAdapter.fromToken(token);
                results.add(samlToken.getToken());
            }

            return results;
        } catch (CoreTokenException e) {
            debug.error("SAML2CTSPersistentStore.retrieveSAML2TokensWithSecondaryKey(): failed to retrieve SAML2 " +
                    "tokens using secondary key:" + secondaryKey, e);
            throw new SAML2TokenRepositoryException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteSAML2Token(String primaryKey) throws SAML2TokenRepositoryException {

        try {
            persistentStore.delete(primaryKey);
        } catch (CoreTokenException e) {
            debug.error("SAML2CTSPersistentStore.deleteSAML2Token(): failed to delete SAML2 " +
                    "token using primary key:" + primaryKey, e);
            throw new SAML2TokenRepositoryException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSAML2Token(String primaryKey, String secondaryKey, Object samlObj, long expirationTime)
            throws SAML2TokenRepositoryException {

        // Save the SAML2 Token.
        try {
            // Perform the Save of the Token to the Token Repository.
            SAMLToken samlToken = new SAMLToken(primaryKey, secondaryKey, expirationTime, samlObj);
            Token token = tokenAdapter.toToken(samlToken);
            persistentStore.create(token);
        } catch (CoreTokenException e) {
            debug.error("SAML2CTSPersistentStore.saveSAML2Token(): failed to save SAML2 " +
                    "token using primary key:" + primaryKey, e);
            throw new SAML2TokenRepositoryException(e.getMessage(), e);
        }
    }
}