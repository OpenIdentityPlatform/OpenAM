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
 */

package org.forgerock.openam.session.service.access.persistence.caching;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicStampedReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.cache.Weigher;
import com.google.common.collect.ImmutableMap;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.service.access.persistence.InternalSessionStore;
import org.forgerock.openam.session.service.access.persistence.InternalSessionStoreStep;
import org.forgerock.openam.session.service.access.persistence.SessionPersistenceException;
import org.forgerock.openam.session.service.access.persistence.watchers.SessionModificationListener;
import org.forgerock.openam.session.service.access.persistence.watchers.SessionModificationWatcher;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.annotations.VisibleForTesting;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.sun.identity.shared.debug.Debug;

/**
 * Session cache implemented using a simple in-memory cache data structure.
 */
public class InMemoryInternalSessionCacheStep implements InternalSessionStoreStep {

    /**
     * Atomic stamped reference to the cache. The stamp is the maximum size of the cache. Permits atomic updates of
     * the cache.
     */
    private final AtomicStampedReference<Cache<String, InternalSession>> cache;
    private final SessionServiceConfig sessionConfig;
    private final Debug debug;

    @Inject
    @VisibleForTesting
    InMemoryInternalSessionCacheStep(SessionServiceConfig sessionConfig,
                                     @Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
                                     SessionModificationWatcher watcher) {
        final int maxCacheSize = sessionConfig.getMaxSessionCacheSize();
        this.sessionConfig = sessionConfig;
        this.cache = new AtomicStampedReference<>(buildCache(maxCacheSize), maxCacheSize);
        this.debug = sessionDebug;

        watcher.addListener(new SessionModificationListener() {
            @Override
            public void sessionChanged(SessionID sessionID) {
                invalidateCache(sessionID);
            }
        });
    }

    @Override
    public InternalSession getBySessionID(final SessionID sessionID, final InternalSessionStore next)
            throws SessionPersistenceException {
        return getFromCacheOrFind(sessionID.toString(), new Callable<InternalSession>() {
            @Override
            public InternalSession call() throws SessionPersistenceException {
                return next.getBySessionID(sessionID);
            }
        });
    }

    @Override
    public InternalSession getByHandle(final String sessionHandle, final InternalSessionStore next)
            throws SessionPersistenceException {
        return getFromCacheOrFind(sessionHandle, new Callable<InternalSession>() {
            @Override
            public InternalSession call() throws SessionPersistenceException {
                return next.getByHandle(sessionHandle);
            }
        });
    }

    @Override
    public InternalSession getByRestrictedID(final SessionID sessionID, final InternalSessionStore next)
            throws SessionPersistenceException {
        return getFromCacheOrFind(sessionID.toString(), new Callable<InternalSession>() {
            @Override
            public InternalSession call() throws SessionPersistenceException {
                return next.getByRestrictedID(sessionID);
            }
        });
    }

    @Override
    public void store(final InternalSession session, final InternalSessionStore next)
            throws SessionPersistenceException {

        // First, pass down to the underlying persistence to store it - if this throws an exception then we shouldn't
        // cache the session.
        next.store(session);

        // Collect all references to the session into a map
        final Map<String, InternalSession> toAdd = new TreeMap<>();
        toAdd.put(session.getID().toString(), session);

        final String sessionHandle = session.getSessionHandle();
        if (sessionHandle != null) {
            toAdd.put(sessionHandle, session);
        }

        for (SessionID restrictedToken : session.getRestrictedTokens()) {
            toAdd.put(restrictedToken.toString(), session);
        }

        // Add all references in a single go. While this is not atomic in the current Guava implementation (as far as
        // I can tell), it provides the opportunity for more sophisticated implementations to optimise the insert.
        getCache().putAll(toAdd);
    }

    @Override
    public void remove(final InternalSession session, final InternalSessionStore next) throws SessionPersistenceException {
        invalidateCache(session.getID());

        // Always ask the lower layers to remove the session even if we did not have it cached
        next.remove(session);
    }

    private void invalidateCache(final SessionID sessionID) {
        InternalSession session = getCache().getIfPresent(sessionID.toString());
        if (session != null) {
            // Remove all references to this session
            List<String> references = new ArrayList<>();
            references.add(session.getID().toString());

            if (session.getSessionHandle() != null) {
                references.add(session.getSessionHandle());
            }

            for (SessionID restrictedToken : session.getRestrictedTokens()) {
                references.add(restrictedToken.toString());
            }

            getCache().invalidateAll(references);
        }
    }

    @VisibleForTesting
    long size() {
        return getCache().size();
    }

    /**
     * The Guava {@link Cache#get(Object, Callable)} API is unnecessarily complicated, and so we try to hide the
     * details here. Firstly, the interface does not allow the callable task to return null, and so we have to check
     * for null from the lower layers and turn that into a {@link NullResultException} that we then turn back into a
     * null in the outer catch block. Secondly, Guava insists on turning all exceptions into
     * {@link ExecutionException}s, which is just a really awkward design decision. Here we unwrap that into
     * something more sensible.
     *
     * @param key the id/restricted id/handle of the session to lookup.
     * @param sessionFinder a task to find the session if it is not already present in the cache.
     * @return the matching internal session object, or {@code null} if not present.
     * @throws SessionPersistenceException if an error occurs.
     */
    private InternalSession getFromCacheOrFind(final String key, final Callable<InternalSession> sessionFinder)
            throws SessionPersistenceException {
        try {
            return getCache().get(key, new Callable<InternalSession>() {
                @Override
                public InternalSession call() throws Exception {
                    InternalSession result = sessionFinder.call();
                    if (result == null) {
                        throw NullResultException.INSTANCE;
                    }
                    return result;
                }
            });
        } catch (ExecutionException e) {
            if (e.getCause() instanceof NullResultException) {
                // There was no result - return back into a null here
                return null;
            }
            // Rethrow any Error/RuntimeException/SessionPersistenceException
            Throwables.propagateIfPossible(e.getCause(), SessionPersistenceException.class);
            // Wrap anything else as a new SessionPersistenceException
            throw new SessionPersistenceException(e.getMessage(), e.getCause());
        }
    }

    private Cache<String, InternalSession> getCache() {
        // Check to see if the cache needs to be hot-swapped
        final int[] stampHolder = new int[1];
        Cache<String, InternalSession> currentCache = cache.get(stampHolder);
        final int oldCacheSize = stampHolder[0];
        final int newCacheSize = sessionConfig.getMaxSessionCacheSize();

        // Use an atomic CAS instruction so that only one thread "wins" and actually updates the cache
        if (oldCacheSize != newCacheSize) {
            debug.message("InMemoryInternalSessionCacheStep: Detected change in cache size configuration (old: {}, " +
                    "new: {}). Rebuilding cache", oldCacheSize, newCacheSize);
            final Cache<String, InternalSession> newCache = buildCache(newCacheSize);
            if (this.cache.compareAndSet(currentCache, newCache, oldCacheSize, newCacheSize)) {
                newCache.putAll(currentCache.asMap());
                currentCache.invalidateAll();
                currentCache.cleanUp();
                currentCache = newCache;
                debug.message("InMemoryInternalSessionCacheStep: Finished replacing cache (new size: {})", newCacheSize);
                if (newCacheSize <= 0) {
                    debug.warning("InMemoryInternalSessionCacheStep: Session caching has been completely disabled!");
                }
            }
        }

        return currentCache;
    }

    private static Cache<String, InternalSession> buildCache(final int maxCacheSize) {
        if (maxCacheSize <= 0) {
            return EmptyCache.INSTANCE;
        }
        return CacheBuilder.newBuilder()
                    .concurrencyLevel(16)
                    .maximumWeight(maxCacheSize)
                    .weigher(new SessionIDWeigher())
                    .softValues()
                    .build();
    }

    /**
     * A custom {@link Weigher} for the cache that only counts the master session ID as having weight, and the
     * session handle and restricted tokens as being "free". This ensures that we only count each session object once
     * and that the cache is sized by the maximum number of session objects rather than references to those sessions.
     */
    private static class SessionIDWeigher implements Weigher<String, InternalSession> {
        @Override
        public int weigh(final @Nonnull String cacheKey, final @Nonnull InternalSession session) {
            // cacheKey could be the master session id, restricted token or session handle: only count the master id
            return StringUtils.isEqualTo(cacheKey, session.getID().toString()) ? 1 : 0;
        }
    }

    /**
     * Local marker exception for transporting null values through Guava's null-hostile Cache API. While it is "bad"
     * to use exceptions for flow-control rather than genuine exceptional cases, the API in this case provides no
     * other mechanism to indicate "no value exists for this key at all" vs "no value currently exists in the cache
     * for this key".
     */
    private static class NullResultException extends Exception {
        private static final NullResultException INSTANCE = new NullResultException();

        private NullResultException() {
            // Private constructor
        }

        /**
         * Does not fill in the stack trace. This is a common performance optimisation for exceptions that are not
         * indicating errors (i.e., only used for flow control) and should never be seen in any log file.
         *
         * @return the same instance of the exception.
         * @see <a href="http://blogs.atlassian
         * .com/2011/05/if_you_use_exceptions_for_path_control_dont_fill_in_the_stac/">Article about avoiding stack
         * trace when using exceptions for flow control</a>
         */
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    /**
     * Null-object pattern for when caching has been completely disabled. Immediately returns null or empty values or
     * delegates directly to the supplied Callable.
     */
    private enum EmptyCache implements Cache<String, InternalSession> {
        INSTANCE;

        @Nullable
        @Override
        public InternalSession getIfPresent(final Object o) {
            return null;
        }

        @Override
        public InternalSession get(final @Nonnull String key,
                final @Nonnull Callable<? extends InternalSession> callable) throws ExecutionException {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        }

        @Override
        public ImmutableMap<String, InternalSession> getAllPresent(final @Nonnull Iterable<?> iterable) {
            return ImmutableMap.of();
        }

        @Override
        public void put(final @Nonnull String key, final @Nonnull InternalSession session) {
            // Ignore
        }

        @Override
        public void putAll(final @Nonnull Map<? extends String, ? extends InternalSession> map) {
            // Ignore
        }

        @Override
        public void invalidate(final @Nonnull Object key) {
            // Ignore
        }

        @Override
        public void invalidateAll(final @Nonnull Iterable<?> iterable) {
            // Ignore
        }

        @Override
        public void invalidateAll() {
            // Ignore
        }

        @Override
        public long size() {
            return 0;
        }

        @Override
        public CacheStats stats() {
            return null;
        }

        @Override
        public ConcurrentMap<String, InternalSession> asMap() {
            return new ConcurrentHashMap<>();
        }

        @Override
        public void cleanUp() {
            // Ignore
        }
    }
}
