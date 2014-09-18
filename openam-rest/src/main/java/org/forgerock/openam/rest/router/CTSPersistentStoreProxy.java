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
package org.forgerock.openam.rest.router;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.PartialToken;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.Map;

/**
 * A proxy implementation of the CTSPersistentStore, which delegates all its calls to the "real" implementation.
 * <br/>
 * Using this proxy instead of the "real" implementation prevents singletons in the core of OpenAM to be initialised
 * prior to OpenAM being configured. As if this happens then some functions in OpenAM will be broken and a restart will
 * be required for them to be restored.
 *
 * @since 12.0.0
 */
@Singleton
public class CTSPersistentStoreProxy implements CTSPersistentStore {

    /**
     * Enum to lazy init the CTSPersistentStore variable in a thread safe manner.
     */
    private enum CTSHolder {
        INSTANCE;

        private final CTSPersistentStore cts;

        private CTSHolder() {
            cts = InjectorHolder.getInstance(CTSPersistentStore.class);
        }

        static CTSPersistentStore get() {
            return INSTANCE.cts;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(Token token) throws CoreTokenException {
        CTSHolder.get().create(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Token read(String tokenId) throws CoreTokenException {
        return CTSHolder.get().read(tokenId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Token token) throws CoreTokenException {
        CTSHolder.get().update(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Token token) throws CoreTokenException {
        CTSHolder.get().delete(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String tokenId) throws CoreTokenException {
        CTSHolder.get().delete(tokenId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(Map<CoreTokenField, Object> query) throws CoreTokenException {
        return CTSHolder.get().delete(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Token> query(TokenFilter filter) throws CoreTokenException {
        return CTSHolder.get().query(filter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<PartialToken> attributeQuery(TokenFilter tokenFilter) throws CoreTokenException {
        return CTSHolder.get().attributeQuery(tokenFilter);
    }

    @Override
    public void deleteOnQuery(TokenFilter tokenFilter) throws CoreTokenException {
        CTSHolder.get().deleteOnQuery(tokenFilter);
    }
}
