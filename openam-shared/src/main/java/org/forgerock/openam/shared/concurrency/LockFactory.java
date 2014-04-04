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
package org.forgerock.openam.shared.concurrency;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Lock factory is responsible for delivering up locks that are shared for a given key. This allows locking to be more
 * fine grained as opposed to blocking all threads for all circumstances. K represents the key type used to index the
 * lock instance. Given that K is used to indirectly index the lock, it must satisfy the hash code and equals methods.
 * <p/>
 * To alleviate the responsibility of the developer and avoid potential memory leaks, the factory manages the internal
 * clean up of unreferenced locks. Locks are cached within a {@link WeakHashMap} against a key. Each lock has a strong
 * reference to its key, so that once all consumers of a lock give up their strong references to the lock, the single
 * strong reference to the key from within the lock is freed. This allows the behaviour of the cache to now clear out
 * the cached entry.
 *
 * @param <K> The type of the key used to retrieve the lock.
 */
public class LockFactory<K> {

    private final Map<Key<K>, Reference<Lock>> lockCache;

    public LockFactory() {
        lockCache = new WeakHashMap<Key<K>, Reference<Lock>>();
    }

    /**
     * Given a key, lookup/create the associated lock instance.
     *
     * @param key
     *         the key used to index a give lock instance.
     * @return a lock instance.
     * @throws NullPointerException
     *         if key is null.
     */
    public Lock acquireLock(K key) {
        if (key == null) {
            throw new NullPointerException("Key must not be null");
        }

        final Key<K> internalKey = new Key<K>(key);
        Lock lock;

        synchronized (lockCache) {
            final Reference<Lock> holder = lockCache.get(internalKey);

            if (holder == null || (lock = holder.get()) == null) {
                lock = new KeyReferenceLock<K>(internalKey);
                // Weak reference is used to ensure consumers
                // maintain the only strong references to the lock.
                lockCache.put(internalKey, new WeakReference<Lock>(lock));
            }
        }

        return lock;
    }

    /**
     * The keys within a weak hash map are held by weak references and this factory relies on the design that once all
     * strong references to a given lock are cleaned up, so the lock's strong reference to its associated key is cleared
     * and the cache can be updated.
     * <p/>
     * It is therefore essential that the factory knows about all strong references to the key and this is the purpose
     * of this class; there is no awareness of what strong references there may be present on the key value passed from
     * the consumer.
     */
    private static final class Key<K> {

        private final K value;

        public Key(K value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }

            if (!(o instanceof Key)) {
                return false;
            }

            final Key<?> key = (Key<?>)o;
            return value == null ? key.value == null : value.equals(key.value);
        }

        @Override
        public String toString() {
            return "Key[value=" + value + ']';
        }

    }

    /**
     * The key reference lock is an re-entrant lock that maintains the only strong reference to its associated key.
     * <p/>
     * Once all consumers give up their strong reference to the lock, no strong references remain on the lock
     * and the lock is now garbage collected. After this no strong references remain on the associated key and
     * so the lock cache can now clear out its cached entry
     */
    private static final class KeyReferenceLock<K> extends ReentrantLock {

        // Single strong reference to the locks associated key.
        private final Key<K> keyReference;

        public KeyReferenceLock(Key<K> keyReference) {
            this.keyReference = keyReference;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("KeyReferenceLock[keyReference=");
            builder.append(keyReference);
            builder.append(',');
            builder.append(super.toString());
            builder.append(']');
            return builder.toString();
        }
    }
}
