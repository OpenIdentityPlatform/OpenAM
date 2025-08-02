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

package org.forgerock.openam.scripting.service;

import static org.forgerock.openam.scripting.ScriptConstants.SERVICE_NAME;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.utils.RealmNormaliser;
import org.slf4j.Logger;

import com.google.inject.name.Named;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * A factory for providing new scripting service instances.
 */
@Singleton
public class ScriptingServiceFactory {

    private final Logger logger;
    private final Map<String, ScriptingService> services = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
    private final CoreWrapper coreWrapper;
    private final RealmNormaliser realmNormaliser;
    private final ServiceConfigManager scm;

    /**
     * Construct a new factory.
     * @param logger The scripting logger.
     */
    @Inject
    public ScriptingServiceFactory(@Named("ScriptLogger") Logger logger, CoreWrapper coreWrapper,
            RealmNormaliser realmNormaliser) {
        this.logger = logger;
        this.coreWrapper = coreWrapper;
        this.realmNormaliser = realmNormaliser;
        try {
            this.scm = coreWrapper.getServiceConfigManager(SERVICE_NAME, coreWrapper.getAdminToken());
        } catch (SSOException | SMSException e) {
            throw new IllegalStateException();
        }
    }

    /**
     * Creates a new scripting service instance based on the calling subject for the passed realm.
     * @param realm the realm
     * @return a scripting service instance
     */
    public ScriptingService create(String realm) {
        try {
            realm = realmNormaliser.normalise(realm);
        } catch (NotFoundException e) {
            throw new IllegalArgumentException("Cannot find realm " + realm, e);
        }
        ScriptingService service = services.get(realm);
        if (service == null) {
            service = forRealm(realm);
        }
        return service;
    }

    private synchronized ScriptingService forRealm(String realm) {
        if (!services.containsKey(realm)) {
            services.put(realm, new ScriptConfigurationService(logger, realm, coreWrapper, scm));
        }
        return services.get(realm);
    }

}
