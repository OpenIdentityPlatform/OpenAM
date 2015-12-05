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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.server;

import java.security.AccessController;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.guava.common.eventbus.EventBus;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.radius.server.audit.RadiusAuditLoggerEventBus;
import org.forgerock.openam.radius.server.audit.RadiusAuditor;
import org.forgerock.openam.radius.server.config.RadiusServer;
import org.forgerock.openam.radius.server.config.RadiusServerConstants;
import org.forgerock.openam.radius.server.config.RadiusServerManager;
import org.forgerock.openam.radius.server.monitoring.RadiusServerEventRegistrar;
import org.forgerock.openam.radius.server.monitoring.RadiusServerEventRegistrator;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * This module is loaded by the java service loader mechanism due, initiated by the entry in META-INF/services.
 */
@GuiceModule
public class RadiusServerGuiceModule extends AbstractModule {

    private static final Debug LOG = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    /**
     * EventBus used by the RADIUS server to notify both the Common Audit Logging framework and the
     * RadiusServiceEventRegistrar that totals number of packets, accepted/rejected requests etc.
     */
    private static EventBus radiusEventBus = new EventBus();

    /**
     * The event registrar that totals number of packets, accepted/rejected requests etc.
     */
    private static RadiusServerEventRegistrar eventRegistrar = new RadiusServerEventRegistrar(radiusEventBus);

    @Override
    protected void configure() {
        LOG.message("RadiusServerGuiceModule - Entering configure.");

        // This should not be Eager Singleton as it relies on being able to obtain config, which
        // relies on this being called from the AMSetupServlet. Eager Singletons get created before
        // that.
        bind(RadiusServer.class).to(RadiusServerManager.class).in(Singleton.class);
        bind(RadiusAuditor.class).to(RadiusAuditLoggerEventBus.class).in(Singleton.class);
        LOG.message("RadiusServerGuiceModule - Leaving configure.");
    }

    /**
     * Guice provider for the ServiceConfigManager.
     *
     * @return a ServiceConfigurationManager that can be used
     * @throws RadiusLifecycleException - when the service config manager can not be obtained.
     */
    @Provides
    @Named("RadiusServer")
    protected ServiceConfigManager getRadiusServiceConfigManger() throws RadiusLifecycleException {
        ServiceConfigManager mgr = null;
        // get a ServiceConfigManager for our service
        try {
            final SSOToken admTk = AccessController.doPrivileged(AdminTokenAction.getInstance());
            mgr = new ServiceConfigManager(RadiusServerConstants.RADIUS_SERVICE_NAME, admTk);
        } catch (final Exception e) {
            throw new RadiusLifecycleException("Could not obtain ServiceConfigManger for the RADIUS service.", e);
        }
        return mgr;
    }

    /**
     * Get the radius eventBus.
     *
     * @return the event bus used by radius components to publish and subscribe for events.
     */
    @Provides
    @Named("RadiusEventBus")
    protected EventBus getRadiusEventBus() {
        return radiusEventBus;
    }

    /**
     * Get the RadiusServerEventRegistrator.
     *
     * @return the RadiusServerEventRegistrator.
     */
    @Provides
    @Singleton
    @Inject
    public RadiusServerEventRegistrator getRadiusServerEventRegistrator() {
        return eventRegistrar;
    }
}
