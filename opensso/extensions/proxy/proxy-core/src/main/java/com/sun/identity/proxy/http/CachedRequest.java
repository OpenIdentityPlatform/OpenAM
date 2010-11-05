/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: CachedRequest.java,v 1.3 2009/10/22 01:18:22 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.http;

import com.sun.identity.proxy.io.CachedInputStream;
import com.sun.identity.proxy.io.TemporaryStorage;
import java.io.IOException;

/**
 * Wraps a request, caching all content and allows rewinding to the
 * request's original state.
 *
 * @author Paul C. Bryan
 */
public class CachedRequest extends Request
{
    /** Contains the entity cached stream to support replay. */
    private CachedInputStream cachedStream = null;

    /** The request to wrap and cache. */
    private Request original;

    /**
     * Creates a new cached request, wrapping the specified request.
     *
     * @param original the request to wrap and cache.
     * @param storage allocates temporary records for caching incoming request entities.
     * @throws IOException if an I/O exception occurs.
     */
    public CachedRequest(Request original, TemporaryStorage storage) throws IOException {
        this.original = original; // FIXME: should really be cloned to avoid anything else writing after the fact?
        if (original.entity != null) {
            cachedStream = new CachedInputStream(original.entity, storage.open(storage.create()));
        }
        rewind();
    }

    /**
     * Restores the request to its original state.
     *
     * @return a reference to this object.
     * @throws IOException if an I/O exception occurs.
     */
    public CachedRequest rewind() throws IOException {
        version = original.version;
        method = original.method;
        uri = original.uri;
        headers.clear();
        headers.putAll(original.headers);
        if (cachedStream != null) {
            entity = cachedStream.rewind();
        }
        principal = original.principal;
        session = original.session;
        attributes.clear();
        attributes.putAll(original.attributes);
        return this;
    }
}

