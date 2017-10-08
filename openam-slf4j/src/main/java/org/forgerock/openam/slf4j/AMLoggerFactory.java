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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.slf4j;

import com.sun.identity.shared.debug.Debug;

import org.forgerock.guava.common.cache.CacheBuilder;
import org.forgerock.guava.common.cache.CacheLoader;
import org.forgerock.guava.common.cache.LoadingCache;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class AMLoggerFactory implements ILoggerFactory {

    private static final LoadingCache<String, AMDebugLogger> amLoggerCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, AMDebugLogger>() {
                @Override
                public AMDebugLogger load(String s) {
                    return new AMDebugLogger(Debug.getInstance(s));
                }
            });

    private static final LoadingCache<String, OpenDJLoggerAdapter> djLoggerCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, OpenDJLoggerAdapter>() {
                @Override
                public OpenDJLoggerAdapter load(String s) {
                    return new OpenDJLoggerAdapter(s);
                }
            });

    public Logger getLogger(String s) {
        // For the case when OpenAM is running with OpenDJ embedded
        if (isEmbeddedDJLogger(s)) {
            return djLoggerCache.getUnchecked(s);
        } else {
            return amLoggerCache.getUnchecked(s);
        }
    }

    private boolean isEmbeddedDJLogger(String s) {
        boolean isOpenDJSDK =
                (s.startsWith("com.forgerock.opendj.ldap.") && !s.startsWith("com.forgerock.opendj.ldap.config")) ||
                s.startsWith("com.forgerock.opendj.util.") ||
                s.startsWith("org.forgerock.opendj.asn1.") ||
                s.startsWith("org.forgerock.opendj.ldap.") ||
                s.startsWith("org.forgerock.opendj.ldif.") ||
                s.startsWith("org.forgerock.opendj.util.");

        boolean isOpenDJ = s.contains(".opendj") || s.contains(".opends");

        return isOpenDJ && !isOpenDJSDK;
    }
}
