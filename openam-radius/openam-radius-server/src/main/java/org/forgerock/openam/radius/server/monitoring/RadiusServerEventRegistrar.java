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
/**
 *
 */
package org.forgerock.openam.radius.server.monitoring;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.forgerock.openam.radius.server.config.RadiusServerConstants;

import com.sun.identity.shared.debug.Debug;

/**
 * Collates information for JMX reporting and monitoring.
 */
public class RadiusServerEventRegistrar implements RadiusServerEventMonitorMXBean, RadiusServerEventRegistrator {

    // private static Logger logger = LoggerFactory.getLogger(RadiusServerConstants.RADIUS_SERVER_LOGGER);
    private static final Debug logger = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    private final ObjectName registeredName;

    private final AtomicLong noOfPacketsReceived = new AtomicLong();
    private final AtomicLong noOFPacketsAccepted = new AtomicLong();
    private final AtomicLong noOfPacketsProcessed = new AtomicLong();
    private final AtomicLong noOfAuthRequestsAccepted = new AtomicLong();
    private final AtomicLong noOfAuthRequestsRejected = new AtomicLong();

    /**
     * Constructor
     */
    public RadiusServerEventRegistrar() {
        logger.message("Entering RadiusServerEventRegistrar() constructor");
        ObjectName name = null;
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            final String procName = ManagementFactory.getRuntimeMXBean().getName();
            name = new ObjectName("OpenAM" + ":type=RadiusServer");
            if (!server.isRegistered(name)) {
                ManagementFactory.getPlatformMBeanServer().registerMBean(this, name);
                logger.message("Registered MBean with name '{}'", procName);
            } else {
                logger.message("MBean with name " + name + " is already registered.");
            }
        } catch (final Exception e) {
            logger.error("Unable to register MBean", e);
        }
        registeredName = name;
        logger.message("Leaving RadiusServiceEntryRegistrar()");
    }

    public String getRegiseredName() {
        return this.registeredName.toString();
    }

    /*
     * (non-Javadoc)
     * @see org.forgerock.openam.radius.server.monitoring.RadiusServerStateUpdator#packetReceived()
     */
    @Override
    public long packetReceived() {
        return noOfPacketsReceived.incrementAndGet();
    }

    /* (non-Javadoc)
     * @see org.forgerock.openam.radius.server.monitoring.RadiusServerMonitoring#getNumberOfPacketsRecieved()
     */
    @Override
    public long getNumberOfPacketsRecieved() {
        return noOfPacketsReceived.get();
    }

    /* (non-Javadoc)
     * @see org.forgerock.openam.radius.server.monitoring.RadiusServerMonitoring#getNumberOfAcceptedPackets()
     */
    @Override
    public long getNumberOfAcceptedPackets() {
        return noOFPacketsAccepted.get();
    }

    /*
     * (non-Javadoc)
     * @see org.forgerock.openam.radius.server.monitoring.RadiusServerStateUpdator#packetAccepted()
     */
    @Override
    public long packetAccepted() {
        return noOFPacketsAccepted.incrementAndGet();
    }

    /*
     * (non-Javadoc)
     * @see org.forgerock.openam.radius.server.monitoring.RadiusServerEventRegistrator#packetProcessed()
     */
    @Override
    public long packetProcessed() {
        return this.noOfPacketsProcessed.incrementAndGet();
    }

    /*
     * (non-Javadoc)
     * @see org.forgerock.openam.radius.server.monitoring.RadiusServerEventMonitorMXBean#getNumberOfPacketsProcessed()
     */
    @Override
    public long getNumberOfPacketsProcessed() {
        return this.noOfPacketsProcessed.get();
    }

    /*
     * (non-Javadoc)
     * @see org.forgerock.openam.radius.server.monitoring.RadiusServerEventRegistrator#authRequestAccepted()
     */
    @Override
    public long authRequestAccepted() {
        return noOfAuthRequestsAccepted.incrementAndGet();
    }

    /*
     * (non-Javadoc)
     * @see
     * org.forgerock.openam.radius.server.monitoring.RadiusServerEventMonitorMXBean#getNumberOfAuthRequestsAccepted()
     */
    @Override
    public long getNumberOfAuthRequestsAccepted() {
        return noOfAuthRequestsAccepted.get();
    }

    /*
     * (non-Javadoc)
     * @see org.forgerock.openam.radius.server.monitoring.RadiusServerEventRegistrator#authRequestRejected()
     */
    @Override
    public long authRequestRejected() {
        return noOfAuthRequestsRejected.incrementAndGet();
    }

    /*
     * (non-Javadoc)
     * @see
     * org.forgerock.openam.radius.server.monitoring.RadiusServerEventMonitorMXBean#getNumberOfAuthRequestsRejected()
     */
    @Override
    public long getNumberOfAuthRequestsRejected() {
        return noOfAuthRequestsRejected.get();
    }
}
