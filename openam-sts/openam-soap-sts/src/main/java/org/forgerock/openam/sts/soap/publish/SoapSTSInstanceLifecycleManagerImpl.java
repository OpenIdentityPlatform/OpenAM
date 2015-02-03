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

package org.forgerock.openam.sts.soap.publish;

import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.ws.security.sts.provider.SecurityTokenServiceProvider;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;

import javax.xml.ws.soap.SOAPBinding;
import java.util.Map;

/**
 * @see org.forgerock.openam.sts.soap.publish.SoapSTSInstanceLifecycleManager
 */
public class SoapSTSInstanceLifecycleManagerImpl implements SoapSTSInstanceLifecycleManager {

    @Override
    public Server exposeSTSInstanceAsWebService(Map<String, Object> webServiceProperties,
                                              SecurityTokenServiceProvider securityTokenServiceProvider,
                                              SoapSTSInstanceConfig stsInstanceConfig) throws STSPublishException {
        try {
            JaxWsServerFactoryBean serverFactoryBean = new JaxWsServerFactoryBean();
        /*
        The serverFactoryBean#setBus invocation is crucial. The cxf.Bus class is ultimately the entity with which newly published web-services are
        registered. The STSBroker#loadBus method previously called BusFactory.setDefaultBus with the bus instance corresponding
        to the STSBroker, the CXFNonSpringServlet subclass which is specified in web.xml as the entry point for all incoming
        web-service invocations, and thus the entity with which all published web-service instances must be registered.
        Without the line below, web-services instances can happily be published, but unless they are associated with the
        STSBroker's cxf bus, the routing functionality in the CXFNonSpringServlet will not know of this published web-service,
        resulting in a 404 on any invocation.

        Note that the parameter to BusFactory.getDefaultBus is set to false, as BusFactory.setDefaultBus was called in
        STSBroker#loadBus, the CXFNonSpringServlet subclass, specified in web.xml, which is the entry point for all
        web-service invocations. Note that the STSBroker#loadBus method is called as part of the servlet intialization
        (it is a load-on-startup servlet), and prior to the initiation of the publish-service polling which will ultimately
        result in the invocation of this method. So the BusFactory.getDefaultBus method should always return the Bus
        instance set in the STSBroker class - thus the createIfNecessary parameter is always set to false.
         */
            final boolean createIfNecessary = false;
            serverFactoryBean.setBus(BusFactory.getDefaultBus(createIfNecessary));
            serverFactoryBean.setWsdlLocation(stsInstanceConfig.getDeploymentConfig().getWsdlLocation());
            serverFactoryBean.setAddress(normalizeDeploymentSubPath(stsInstanceConfig.getDeploymentSubPath()));
            serverFactoryBean.setServiceBean(securityTokenServiceProvider);
            serverFactoryBean.setServiceName(stsInstanceConfig.getDeploymentConfig().getService());
            serverFactoryBean.setEndpointName(stsInstanceConfig.getDeploymentConfig().getPort());
            serverFactoryBean.setBindingId(SOAPBinding.SOAP12HTTP_BINDING);  //TODO: get clear on implications of this line.
            serverFactoryBean.setProperties(webServiceProperties);
            return serverFactoryBean.create();
        } catch (RuntimeException e) {
            /*
            The CXF runtime was written at the time when checked-exceptions were passe' - thus RuntimeException subclasses
            are thrown. Catch Exception because the compiler won't tell me which exceptions are thrown.
             */
            throw new STSPublishException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
        }

    }

    @Override
    public void destroySTSInstance(Server server) throws STSPublishException {
        try {
            server.destroy();
        } catch (RuntimeException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
        }
    }

    /*
    Ultimately, when the the Server returned from JaxWsServerFactoryBean#create is destroyed, the DestinationRegistryImpl
    from which the AbstractHttpDestination should be unregistered (as part of the call-chain initiated by the Server#destroy
    method) caches the path as e.g /inst20777493845, whereas I am publishing it as inst20777493845. This results in the
    following stack trace when attempting to reference the ?wsdl associated with the destroyed endpoint:
    java.lang.NullPointerException
        at org.apache.cxf.transport.http.AbstractHTTPDestination.invoke(AbstractHTTPDestination.java:239)
        at org.apache.cxf.transport.servlet.ServletController.invokeDestination(ServletController.java:248)
        at org.apache.cxf.transport.servlet.ServletController.invoke(ServletController.java:222)
        at org.apache.cxf.transport.servlet.ServletController.invoke(ServletController.java:153)
        at org.apache.cxf.transport.servlet.CXFNonSpringServlet.invoke(CXFNonSpringServlet.java:167)
    This issue is addressed by pre-pending a '/' to the deployment path set in JaxWsServerFactoryBean#setAddress.
     */
    private String normalizeDeploymentSubPath(String deploymentSubPath) {
        if (!deploymentSubPath.startsWith("/")) {
            return "/" + deploymentSubPath;
        } else {
            return deploymentSubPath;
        }
    }
}
