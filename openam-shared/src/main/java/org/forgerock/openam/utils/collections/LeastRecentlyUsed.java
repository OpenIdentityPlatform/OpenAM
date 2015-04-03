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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.utils.collections;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple Least Recently Used cache implementation based on the standard
 * Java SDK LinkedHashMap class.
 *
 * Specially maintains a cache up to the size limit specified. After which
 * point will start to discard entries in Least Recently Used order.
 */
public class LeastRecentlyUsed<T, V> extends LinkedHashMap<T, V> {
    private final int maxSize;

    public LeastRecentlyUsed(int maxSize) {
        // Values selected based on Java JavaDoc recommendations.
        super(maxSize*4/3, 0.75f, true);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<T, V> entry) {
        return size() > maxSize;
    }
}
