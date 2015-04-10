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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap;

import com.google.inject.Key;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSLifecycle;
import org.forgerock.openam.sts.soap.config.SoapSTSInjectorHolder;

import javax.servlet.ServletConfig;

/**
 * This class is the endpoint specified in web.xml as the recipient for all SOAP invocations targeting
 * STS instances. All STS instances must be published on this class' bus to be available for invocation in
 * the uri element corresponding to the web.xml entry. Decoupling Spring from CXF requires an instance of the
 * CXFNonSpringServlet to be specified in the web.xml entry. Dynamically publishing web-service endpoints which are
 * aggregated on this endpoint requires that the publish action occurs in the context of this class' bus.
 */
public class STSBroker extends CXFNonSpringServlet {
    @Override
    public void loadBus(ServletConfig servletConfig) {
        super.loadBus(servletConfig);
        /*
        The SoapSTSInstancePublisher will initialize the bus associated with the JaxWsServerFactoryBean used to publish
        each soap-sts instance via the call to BusFactory#getDefaultBus. Note that this must occur prior to the call
        to SoapSTSInstancePublisher#initiatePublishPolling, as the context associated with this method will ultimately
        have to reference the BusFactory's default Bus instance so that published web-services are exposed via this
        class, which is the entry point for all web-service invocations.
         */
        BusFactory.setDefaultBus(getBus());
        /*
         kick off the soap-sts bootstrap functionality, which includes obtaining the sts agent config, and kicking off
         the publish polling logic.
          */
        SoapSTSInjectorHolder.getInstance(Key.get(SoapSTSLifecycle.class)).startup();
    }
}
