/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at:
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 *
 * See the License for the specific language governing permission and
 * limitations under the License.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at opensso/legal/CDDLv1.0.txt. If applicable,
 * add the following below the CDDL Header, with the fields enclosed by
 * brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * $Id: NonceManager.java,v 1.3 2009/05/28 16:00:33 pbryan Exp $
 */

package com.sun.identity.oauth.filter;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Tracks the nonces for a given consumer key and/or token. Automagically
 * ensures timestamp is monotonically increasing and tracks all nonces
 * for a given timestamp.
 *
 * @author Paul C. Bryan <pbryan@sun.com>
 */
class NonceManager
{
    /** The maximum valid age of a nonce timestamp, in milliseconds. */
    private final long maxAge;

    /** Verifications to perform on average before performing garbage collection. */
    private final int gcPeriod;

    /** Counts number of verification requests performed to schedule garbage collection. */
    private int gcCounter = 0;

    /** A set of nonces for a given key and timestamp. */
    private class Nonces {
        private long timestamp = -1;
        private HashSet<String> values = new HashSet<String>();
    }

    /** Maps keys to nonces. */
    private HashMap<String, Nonces> map = new HashMap<String, Nonces>();

    /**
     * TODO: Description.
     *
     * @param maxAge the maximum valid age of a nonce timestamp, in milliseconds.
     * @param gcPeriod verifications to perform on average before performing garbage collection.
     */
    public NonceManager(long maxAge, int gcPeriod)
    {
        if (maxAge <= 0 || gcPeriod <= 0) {
            throw new IllegalArgumentException();
        }

        this.maxAge = maxAge;
        this.gcPeriod = gcPeriod;
    }

    /**
     * Evaluates the timestamp/nonce combination for validity, storing and/or
     * clearing nonces as required.
     *
     * @param timestamp the oauth_timestamp value for a given consumer request.
     * @param nonce the oauth_nonce value for a given consumer request.
     * @return true if the timestamp/nonce are valid.
     */
    public boolean verify(String key, String timestamp, String nonce)
    {
        long now = System.currentTimeMillis();

        // convert timestap to milliseconds since epoch to deal with uniformly
        long stamp = longValue(timestamp) * 1000;

        // invalid timestamp supplied; automatically invalid
        if (stamp + maxAge < now) {
            return false;
        }

        Nonces nonces = map.get(key);

        // no nonce exists for key; create a new one
        synchronized(map) {
            if (nonces == null) {
                nonces = new Nonces();
                map.put(key, nonces);
            }
        }

        // timestamp not monotonically increasing; timestamp invalid
        if (stamp < nonces.timestamp) {
            return false;
        }

        // new timestamp enountered for this key
        synchronized(nonces) {
            if (stamp > nonces.timestamp) {
                nonces.timestamp = stamp;
                nonces.values.clear();
            }
        }

        // perform garbage collection if counter is up to established number of passes
        if (++gcCounter >= gcPeriod) {
            gcCounter = 0;
            for (String k : map.keySet()) {
                Nonces n = map.get(k);
                if (n.timestamp + maxAge < now) {
                    map.remove(k);
                }
            }
        }

        // returns false if nonce already encountered for given timestamp
        return nonces.values.add(nonce);
    }

    private long longValue(String value) {
        try {
            return Long.valueOf(value);
        }
        catch (NumberFormatException nfe) {
            return -1;
        }
    }
}

