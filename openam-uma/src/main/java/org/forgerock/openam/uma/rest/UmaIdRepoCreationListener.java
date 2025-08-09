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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.uma.rest;

import java.util.Set;
import java.util.TreeSet;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.forgerock.openam.core.CoreWrapper;

import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoCreationListener;

/**
 * An implementation of a {@code IdRepoCreationListener} which adds a
 * {@code UmaPolicyApplicationListener} to each {@code AMIdentityRepository} for a realm.
 *
 * @since 13.0.0
 */
@Singleton
public class UmaIdRepoCreationListener implements IdRepoCreationListener {

    private final UmaPolicyApplicationListener policyApplicationListener;
    private final Set<String> registeredRealms = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final CoreWrapper coreWrapper;

    /**
     * Creates a new UmaIdRepoCreationListener instance.
     *
     * @param policyApplicationListener An instance of the {@code UmaPolicyApplicationListener}
     */
    @Inject
    public UmaIdRepoCreationListener(UmaPolicyApplicationListener policyApplicationListener, CoreWrapper coreWrapper) {
        this.policyApplicationListener = policyApplicationListener;
        this.coreWrapper = coreWrapper;
    }

    /**
     * Ensures that a {@link UmaPolicyApplicationListener} is attached to the
     * {@code AMIdentityRepository} for each realm.
     *
     * @param idRepo {@inheritDoc}
     * @param realm {@inheritDoc}
     */
    @Override
    public synchronized void notify(AMIdentityRepository idRepo, String realm) {
        String normalizedRealm = coreWrapper.convertRealmNameToOrgName(realm);
        if (!registeredRealms.contains(normalizedRealm)) {
            idRepo.addEventListener(policyApplicationListener);
            registeredRealms.add(normalizedRealm);
        }
    }
}
