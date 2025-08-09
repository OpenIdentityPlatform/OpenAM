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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.session.service.access.persistence.watchers;

import static org.forgerock.util.query.QueryFilter.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.continuous.ChangeType;
import org.forgerock.openam.cts.continuous.ContinuousQueryListener;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.opendj.ldap.Attribute;

import com.iplanet.dpro.session.SessionID;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.setup.SetupListener;
import com.sun.identity.shared.debug.Debug;

/**
 * Listens for changes to sessions, and triggers attached listeners.
 */
public class SessionModificationWatcher implements SetupListener {

    private SessionModificationContinuousQueryListener queryListener;

    public SessionModificationWatcher() {
        queryListener = InjectorHolder.getInstance(SessionModificationContinuousQueryListener.class);
    }

    @Override
    public void setupComplete() {
        queryListener.setupContinuousQuery();
    }

    /**
     * Add a listener which will be triggered by this watcher.
     *
     * @param listener The listener to add.
     */
    public void addListener(SessionModificationListener listener) {
        queryListener.addListener(listener);
    }

    @Singleton
    private static class SessionModificationContinuousQueryListener implements ContinuousQueryListener<Attribute> {

        private final List<SessionModificationListener> listeners;
        private final Debug debug;
        private final Provider<CTSPersistentStore> store;

        @Inject
        public SessionModificationContinuousQueryListener(@Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
                                                          Provider<CTSPersistentStore> store) {
            this.debug = sessionDebug;
            this.store = store;
            this.listeners = new ArrayList<>();
        }

        private void addListener(SessionModificationListener listener) {
            listeners.add(listener);
        }

        @Override
        public void objectChanged(String tokenId, Map<String, Attribute> changeSet, ChangeType changeType) {
            if (changeType == ChangeType.DELETE) {
                final SessionID sessionID = new SessionID(SessionUtils.getDecrypted(changeSet.get(SessionTokenField.SESSION_ID.getField().toString()).firstValue().toString()));
                for (SessionModificationListener listener : listeners) {
                    listener.sessionChanged(sessionID);
                }
            }
        }

        @Override
        public void objectsChanged(Set<String> tokenIds) {
            // This section intentionally left blank
        }

        @Override
        public void connectionLost() {
            debug.error("Continuous query listener has lost its connection");
        }

        @Override
        public void processError(DataLayerException dlE) {
            debug.error("SessionModificationWatcher error", dlE);
        }

        private void setupContinuousQuery() {
            try {
                store.get().addContinuousQueryListener(this, getTokenFilter());
            } catch (CoreTokenException ctE) {
                throw new RuntimeException("Unable to register continuous query for session cache invalidation", ctE);
            }
        }

        private static TokenFilter getTokenFilter() {
            return new TokenFilterBuilder()
                    .returnAttribute(SessionTokenField.SESSION_ID.getField())
                    .withQuery(equalTo(CoreTokenField.TOKEN_TYPE, TokenType.SESSION))
                    .build();
        }
    }
}
