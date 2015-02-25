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

package org.forgerock.openam.sts.soap.publish;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;

import org.apache.cxf.ws.security.sts.provider.SecurityTokenServiceProvider;
import javax.xml.ws.soap.SOAPBinding;
import java.util.Map;

/**
 * @see org.forgerock.openam.sts.soap.publish.STSInstancePublisher
 *
 * The constructor may also need to take an instance of the CXF bus, so the thread-local bus variable can be set (the
 * same instance associated with the STSBroker), so that all published STS instances are known to the CXF runtime, and
 * are thus capable of being invoked by the STSBroker. Currently that is not necessary, as this class is invoked via the
 * STSPublishImpl, a web-service which is associated with the CXF bus, which results in a transitive association. But going
 * forward, this class may not be invoked via a web-service - which means that this association must be made
 * explicitly.
 *
 */
public class STSInstancePublisherImpl implements STSInstancePublisher {
    private final Map<String, Object> webServiceProperties;
    private final SecurityTokenServiceProvider securityTokenServiceProvider;
    private final SoapSTSInstanceConfig stsInstanceConfig;

    @Inject
    public STSInstancePublisherImpl(@Named(AMSTSConstants.STS_WEB_SERVICE_PROPERTIES)Map<String, Object> webServiceProperties,
                                    SecurityTokenServiceProvider securityTokenServiceProvider,
                                    SoapSTSInstanceConfig stsInstanceConfig) {
        this.webServiceProperties = webServiceProperties;
        this.securityTokenServiceProvider = securityTokenServiceProvider;
        this.stsInstanceConfig = stsInstanceConfig;
    }

    public void publishSTSInstance() {
        JaxWsServerFactoryBean serverFactoryBean = new JaxWsServerFactoryBean();
        serverFactoryBean.setWsdlLocation(stsInstanceConfig.getDeploymentConfig().getWsdlLocation());
        serverFactoryBean.setAddress(stsInstanceConfig.getDeploymentConfig().getUriElement());
        serverFactoryBean.setServiceBean(securityTokenServiceProvider);
        serverFactoryBean.setServiceName(stsInstanceConfig.getDeploymentConfig().getService());
        serverFactoryBean.setEndpointName(stsInstanceConfig.getDeploymentConfig().getPort());
        serverFactoryBean.setBindingId(SOAPBinding.SOAP12HTTP_BINDING);  //TODO: get clear on implications of this line.
        serverFactoryBean.setProperties(webServiceProperties);
        serverFactoryBean.create();
    }

}
