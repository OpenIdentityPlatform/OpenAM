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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.soap;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.forgerock.openam.sts.soap.publish.web.STSPublishImpl;

import javax.servlet.ServletConfig;

/**
 * This class is the endpoint specified in web.xml as the recipient for all SOAP invocations targeting
 * STS instances. All STS instances must be published on this class' bus to be available for invocation in
 * the uri element corresponding to the web.xml entry. Decoupling Spring from CXF requires an instance of the
 * CXFNonSpringServlet to be specified in the web.xml entry. Dynamically publishing web-service endpoints which are
 * aggregated on this endpoint requires that the publish action occurs in the context of this class' bus.
 *
 * Currently, a web-service which allows the publish of STS instances is associated with this bus. This means that all
 * STS instances published via the web-service-publish web-service will also be automatically associated with bus associated
 * with instances of this class.
 *
 * Going forward, it is unlikely that users will publish STS instances by invoking a web-service. If this is the case,
 * then the association of the published STS instance with the bus associated with instances of this class must
 * be investigated and re-established.
 *
 */
public class STSBroker extends CXFNonSpringServlet {
    @Override
    public void loadBus(ServletConfig servletConfig) {
        super.loadBus(servletConfig);
        Bus bus = getBus();
        BusFactory.setDefaultBus(bus);
        /*
        WebLogic 11g requires that published endpoints have a trailing '/'. Need to determine whether this
         causes problems on other supported containers. The code below did not work either (with the trailing '/'),
         so I went back to the CXF-specific way of publishing a web-service. That is probably the best approach - provides
         more isolation from container vagaries.

        Endpoint.publish("/sts_publish/", new STSPublishImpl());
        */
        JaxWsServerFactoryBean serverFactoryBean = new JaxWsServerFactoryBean();
        serverFactoryBean.setAddress("/sts_publish/");
        serverFactoryBean.setServiceBean(new STSPublishImpl());
        serverFactoryBean.create();
    }
}
