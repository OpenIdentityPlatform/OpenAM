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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.rest.router;

import java.util.Collection;
import java.util.Map;

import jakarta.inject.Singleton;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.continuous.ContinuousQueryListener;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.util.Options;

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

    @Override
    public void create(Token token) throws CoreTokenException {
        CTSHolder.get().create(token);
    }

    @Override
    public void create(Token token, Options options) throws CoreTokenException {
        CTSHolder.get().create(token, options);
    }

    @Override
    public void createAsync(Token token) throws CoreTokenException {
        CTSHolder.get().createAsync(token);
    }

    @Override
    public void createAsync(Token token, Options options) throws CoreTokenException {
        CTSHolder.get().createAsync(token, options);
    }

    @Override
    public Token read(String tokenId) throws CoreTokenException {
        return CTSHolder.get().read(tokenId);
    }

    @Override
    public Token read(String tokenId, Options options) throws CoreTokenException {
        return CTSHolder.get().read(tokenId, options);
    }

    @Override
    public void update(Token token) throws CoreTokenException {
        CTSHolder.get().update(token);
    }

    @Override
    public void update(Token token, Options options) throws CoreTokenException {
        CTSHolder.get().update(token, options);
    }

    @Override
    public void updateAsync(Token token) throws CoreTokenException {
        CTSHolder.get().updateAsync(token);
    }

    @Override
    public void updateAsync(Token token, Options options) throws CoreTokenException {
        CTSHolder.get().updateAsync(token, options);
    }

    @Override
    public void delete(Token token) throws CoreTokenException {
        CTSHolder.get().delete(token);
    }

    @Override
    public void deleteAsync(Token token) throws CoreTokenException {
        CTSHolder.get().deleteAsync(token);
    }

    @Override
    public void delete(String tokenId) throws CoreTokenException {
        CTSHolder.get().delete(tokenId);
    }

    @Override
    public void delete(String tokenId, Options options) throws CoreTokenException {
        CTSHolder.get().delete(tokenId, options);
    }

    @Override
    public void deleteAsync(String tokenId) throws CoreTokenException {
        CTSHolder.get().deleteAsync(tokenId);
    }

    @Override
    public void deleteAsync(String tokenId, Options options) throws CoreTokenException {
        CTSHolder.get().deleteAsync(tokenId, options);
    }

    @Override
    public int delete(Map<CoreTokenField, Object> query) throws CoreTokenException {
        return CTSHolder.get().delete(query);
    }

    @Override
    public void addContinuousQueryListener(ContinuousQueryListener listener, TokenFilter filter)
            throws CoreTokenException {
        CTSHolder.get().addContinuousQueryListener(listener, filter);
    }

    @Override
    public void removeContinuousQueryListener(ContinuousQueryListener listener, TokenFilter filter)
            throws CoreTokenException {
        CTSHolder.get().removeContinuousQueryListener(listener, filter);
    }

    @Override
    public void stopContinuousQuery(TokenFilter filter) {
        CTSHolder.get().stopContinuousQuery(filter);
    }

    @Override
    public Collection<Token> query(TokenFilter filter) throws CoreTokenException {
        return CTSHolder.get().query(filter);
    }

    @Override
    public Collection<PartialToken> attributeQuery(TokenFilter tokenFilter) throws CoreTokenException {
        return CTSHolder.get().attributeQuery(tokenFilter);
    }

    @Override
    public void deleteOnQueryAsync(TokenFilter tokenFilter) throws CoreTokenException {
        CTSHolder.get().deleteOnQueryAsync(tokenFilter);
    }
}
