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

/**
 * This interface defines the concerns of starting and stopping the soap-sts context. Startup will include obtaining the
 * agent config corresponding to the soap sts agent, and then initiating the polling of the sts-publish service, to
 * obtain published soap-sts instances. Shutdown involves shutting down the scheduled executor which regularly runs the
 * sts-publish polling logic. Note that startup is initiated by a call from the STSBroker class, not the SoapSTSContextListener,
 * as the act of exposing published soap-sts instances as web-services requires access to the cxf Bus, which is not
 * initialized until the STSBroker Servlet class is loaded.
 */
public interface SoapSTSLifecycle {
    /**
     * Start up the soap-sts agent context. This includes obtaining the soap-sts agent configuration state, and initiate
     * the polling of OpenAM to obtain published soap-sts instances.
     */
    void startup();

    /**
     * Shut down the soap-sts agent context. This includes shutting down the ScheduledExecutor running the publish polling
     * logic.
     */
    void shutdown();
}
