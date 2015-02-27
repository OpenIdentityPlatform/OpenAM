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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap.bootstrap;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.soap.publish.SoapSTSInstancePublisher;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @see org.forgerock.openam.sts.soap.bootstrap.SoapSTSLifecycle
 */
public class SoapSTSLifecycleImpl implements SoapSTSLifecycle {
    /*
    This value corresponds to an AttributeSchema entry in AgentsService.xml for the SoapSTSAgent.
     */
    private static final String POLL_INTERVAL_KEY = "publish-service-poll-interval";
    private static final int POLL_INTERVAL_DEFAULT = 300;
    private static final int AGENT_CONFIG_POLL_INITIAL_WAIT = 10;
    private static final int AGENT_CONFIG_POLL_INTERVAL = 600;
    private final ScheduledExecutorService scheduledExecutorService;
    private final SoapSTSAgentConfigAccess soapSTSAgentConfigAccess;
    private final SoapSTSInstancePublisher soapSTSInstancePublisher;
    private ScheduledFuture<?> agentConfigAccessFuture;
    private volatile boolean configObtained;
    private final Logger logger;

    @Inject
    SoapSTSLifecycleImpl(ScheduledExecutorService scheduledExecutorService,
                         SoapSTSAgentConfigAccess soapSTSAgentConfigAccess,
                         SoapSTSInstancePublisher soapSTSInstancePublisher,
                         Logger logger) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.soapSTSAgentConfigAccess = soapSTSAgentConfigAccess;
        this.soapSTSInstancePublisher = soapSTSInstancePublisher;
        this.logger = logger;
    }

    @Override
    public void startup() {
        agentConfigAccessFuture =
                scheduledExecutorService.scheduleAtFixedRate(
                        new AgentConfigAccessRunnable(), AGENT_CONFIG_POLL_INITIAL_WAIT, AGENT_CONFIG_POLL_INTERVAL, TimeUnit.SECONDS);

    }

    @Override
    public void shutdown() {
        scheduledExecutorService.shutdownNow();
    }

    /*
    Called by the scheduled AgentConfigAccessRunnable when agent config has been successfully obtained. Now schedule
    the publish polling logic, and cancel the Runnable which obtains the soap sts agent config.
     */
    private void agentConfigObtained(JsonValue agentConfig) {
        if (!configObtained) {
            final int pollInterval = getPublishPollInterval(agentConfig);
            logger.debug("Scheduling the publish service poller at an interval of " + pollInterval + " seconds.");
            scheduledExecutorService.scheduleAtFixedRate(soapSTSInstancePublisher, pollInterval, pollInterval,
                    TimeUnit.SECONDS);
            if (!agentConfigAccessFuture.cancel(true)) {
                logger.error("Could not cancel the AgentConfigAccessRunnable!");
            }
            /*
            Toggling the configObtained boolean is likely overkill, given the cancel on the agentConfigAccessFuture above,
            but I want to avoid re-scheduling the polling of the publish service in case the cancel of the agentConfigAccessFuture
            does not succeed. Note that this cancellation will occur in the thread running in the (single threaded)
            ScheduledExecutor, so it seems very unlikely that this cancellation may fail, but I want to insure that, if
            this occurs, only this one task will continue to run every 10 minutes, rather than triggering another task
            every time it runs.
             */
            configObtained = true;
        }
    }

    private int getPublishPollInterval(JsonValue agentConfig) {
        try {
            return Integer.valueOf(agentConfig.get(POLL_INTERVAL_KEY).asList(String.class).get(0));
        } catch (JsonValueException e) {
            logger.error("Exception caught obtaining the publish service poll interval from the agent config: " + e +
                    "\nUsing default poll interval of " + POLL_INTERVAL_KEY + " seconds.", e);
            return POLL_INTERVAL_DEFAULT;
        } catch (NumberFormatException e) {
            logger.error("Exception caught obtaining the publish service poll interval from the agent config: " + e +
                    "\nUsing default poll interval of " + POLL_INTERVAL_KEY + " seconds.", e);
            return POLL_INTERVAL_DEFAULT;
        }
    }

    private class AgentConfigAccessRunnable implements Runnable {
        @Override
        public void run() {
            try {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                final JsonValue configState = soapSTSAgentConfigAccess.getConfigurationState();
                agentConfigObtained(configState);
            } catch (ResourceException e) {
                logger.warn("Exception caught obtaining soap sts agent config state: " + e + "; Will retry.");
            } catch (Exception e) {
                //need to catch all exceptions as exception encountered in ScheduledExecutor Runnable will terminate subsequent executions
                logger.error("Unexpected exception caught obtaining soap sts agent config state: " + e + "; Will retry.");
            }
        }
    }
}
