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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.identity.idm;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.Set;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoCreationListener;
import com.sun.identity.log.spi.Debug;
import org.forgerock.guice.core.InjectorHolder;

/**
 * Using Guice adds all bound {@link IdRepoCreationListener}s to the {@link AMIdentityRepository}.
 *
 * @since 13.0.0
 */
public class AMIdentityRepositoryListenerInitializer implements ServletContextListener {

    /**
     * Adds all bound {@link IdRepoCreationListener}s to the {@link AMIdentityRepository}.
     *
     * @param event {@inheritDoc}
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {
    		try {
	        Set<IdRepoCreationListener> listeners = InjectorHolder.getInstance(
	                Key.get(new TypeLiteral<Set<IdRepoCreationListener>>() {}));
	        for (IdRepoCreationListener listener : listeners) {
	            AMIdentityRepository.addCreationListener(listener);
	        }
    		}catch (Throwable e) {
    			Debug.error("AMIdentityRepositoryListenerInitializer contextInitialized", e);
    			throw e;
    		}
    }

    /**
     * Removes all bound {@link IdRepoCreationListener}s from the {@link AMIdentityRepository}.
     *
     * @param event {@inheritDoc}
     */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
    		try {
	        Set<IdRepoCreationListener> listeners = InjectorHolder.getInstance(
	                Key.get(new TypeLiteral<Set<IdRepoCreationListener>>() {}));
	        for (IdRepoCreationListener listener : listeners) {
	            AMIdentityRepository.removeCreationListener(listener);
	        }
    		}catch (Throwable e) {
    			Debug.error("AMIdentityRepositoryListenerInitializer contextDestroyed", e);
    			throw e;
    		}
    }
}
