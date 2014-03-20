/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.idm;

import com.sun.identity.idm.IdCachedServices;

import java.util.Set;

/**
 * Decorator pattern base class for {@link IdCachedServices} implementations.
 *
 * @since 12.0.0
 * @see IdServicesDecorator
 */
public class IdCachedServicesDecorator extends IdServicesDecorator implements IdCachedServices {
    /**
     * Constructs the decorator using the given delegate implementation.
     *
     * @param delegate a non-null IdServices implementation to delegate calls to.
     * @throws NullPointerException if the delegate is null.
     */
    protected IdCachedServicesDecorator(IdCachedServices delegate) {
        super(delegate);
    }

    @Override
    protected IdCachedServices getDelegate() {
        return (IdCachedServices) super.getDelegate();
    }

    @Override
    public int getSize() {
        return getDelegate().getSize();
    }

    @Override
    public void clearCache() {
        getDelegate().clearCache();
    }

    @Override
    public void dirtyCache(String changedId, int eventType, boolean cosType, boolean aciChange, Set attrNames) {
        getDelegate().dirtyCache(changedId, eventType, cosType, aciChange, attrNames);
    }
}
