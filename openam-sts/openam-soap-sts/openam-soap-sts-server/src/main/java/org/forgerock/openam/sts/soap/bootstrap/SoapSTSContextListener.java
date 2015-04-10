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

import com.google.inject.Key;
import org.forgerock.openam.sts.soap.config.SoapSTSInjectorHolder;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * This class will shutdown the soap-sts state related to polling the sts-publish service in order to
 * expose soap-sts instances published on the OpenAM home server.
 * The contextInitialized method will not be leveraged for initializing this process because
 * contextInitialized is called prior to the Servlet is actually initialized, and thus referencing the SoapSTSInjectorHolder,
 * and thereby triggering the global bindings, kicks-off the process of polling the sts-publish service, as part of exposing
 * published soap-sts instances. Part of this exposure involves referencing the cxf Bus set in the STSBroker#loadBus method,
 * this process cannot be initialized until after the STSBroker servlet has been initialized. Thus this initialization will
 * occur in the STSBroker#loadBus method, and the STSBroker servlet will be a load-on-startup servlet.
 */
public class SoapSTSContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        SoapSTSInjectorHolder.getInstance(Key.get(SoapSTSLifecycle.class)).shutdown();
    }
}
