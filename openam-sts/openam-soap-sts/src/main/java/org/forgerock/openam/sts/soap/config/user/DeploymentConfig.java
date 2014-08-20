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

package org.forgerock.openam.sts.soap.config.user;

import org.forgerock.openam.sts.config.user.AuthTargetMapping;

import javax.xml.namespace.QName;

import org.forgerock.util.Reject;

/**
 * This class represents the deployment configuration for an STS instance. This includes:
 * 1. the wsdl location (which will determine the SecurityPolicy bindings)
 * 2. The service and port QNames
 * 3. The uri element (e.g. /realm1/accounting/bobo) at which the STS instance will be deployed
 * 4. The realm within which the STS is deployed.
 *
 */
public class DeploymentConfig {
    public static class DeploymentConfigBuilder {
        private QName service;
        private QName port;
        private String uriElement;
        private String wsdlLocation;
        private String realm = "/"; //default value
        private AuthTargetMapping authTargetMapping;
        private String amDeploymentUrl;

        public DeploymentConfigBuilder serviceQName(QName service)  {
            this.service = service;
            return this;
        }

        public DeploymentConfigBuilder portQName(QName port)  {
            this.port = port;
            return this;
        }

        public DeploymentConfigBuilder amDeploymentUrl(String url) {
            this.amDeploymentUrl = url;
            return this;
        }

        public DeploymentConfigBuilder uriElement(String uriElement)  {
            this.uriElement = uriElement;
            return this;
        }

        public DeploymentConfigBuilder wsdlLocation(String wsdlLocation)  {
            this.wsdlLocation = wsdlLocation;
            return this;
        }

        public DeploymentConfigBuilder realm(String realm)  {
            this.realm = realm;
            return this;
        }

        public DeploymentConfigBuilder authTargetMapping(AuthTargetMapping authTargetMapping)  {
            this.authTargetMapping = authTargetMapping;
            return this;
        }

        public DeploymentConfig build() {
            return new DeploymentConfig(this);
        }
    }

    private final QName service;
    private final QName port;
    private final String uriElement;
    private final String wsdlLocation;
    private final String realm;
    private final AuthTargetMapping authTargetMapping;
    private final String amDeploymentUrl;

    private DeploymentConfig(DeploymentConfigBuilder builder) {
        this.service = builder.service;
        this.port = builder.port;
        this.uriElement = builder.uriElement;
        this.wsdlLocation = builder.wsdlLocation;
        this.realm = builder.realm;
        this.authTargetMapping = builder.authTargetMapping;
        this.amDeploymentUrl = builder.amDeploymentUrl;
        Reject.ifNull(service, "Service QName cannot be null");
        Reject.ifNull(port, "Port QName cannot be null");
        Reject.ifNull(uriElement, "UriElement String cannot be null");
        Reject.ifNull(wsdlLocation, "wsdlLocation String cannot be null");
        Reject.ifNull(realm, "Realm String cannot be null");
        Reject.ifNull(authTargetMapping, "AuthTargetMapping cannot be null");
        Reject.ifNull(amDeploymentUrl, "AM Deployment URL cannot be null");
    }

    public static DeploymentConfigBuilder builder() {
        return new DeploymentConfigBuilder();
    }

    public QName getService() {
        return service;
    }

    public QName getPort() {
        return port;
    }

    public String getUriElement() {
        return uriElement;
    }

    public String getWsdlLocation() {
        return wsdlLocation;
    }

    public String getRealm() {
        return realm;
    }

    public AuthTargetMapping getAuthTargetMapping() {
        return authTargetMapping;
    }

    public String getAMDeploymentUrl() {
        return amDeploymentUrl;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DeploymentConfig instance: ").append('\n');
        sb.append('\t').append("Service QName: ").append(service).append('\n');
        sb.append('\t').append("Port QName: ").append(port).append('\n');
        sb.append('\t').append("Deployment uriElement: ").append(uriElement).append('\n');
        sb.append('\t').append("wsdlLocation: ").append(wsdlLocation).append('\n');
        sb.append('\t').append("realm: ").append(realm).append('\n');
        sb.append('\t').append("authTargetMapping: ").append(authTargetMapping).append('\n');
        sb.append('\t').append("AM Deployment URL: ").append(amDeploymentUrl).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DeploymentConfig) {
            DeploymentConfig otherConfig = (DeploymentConfig)other;
            return service.equals(otherConfig.getService()) &&
                    port.equals(otherConfig.getPort()) &&
                    uriElement.equals(otherConfig.getUriElement()) &&
                    wsdlLocation.equals(otherConfig.getWsdlLocation()) &&
                    realm.equals(otherConfig.getRealm()) &&
                    amDeploymentUrl.equals(otherConfig.getAMDeploymentUrl()) &&
                    authTargetMapping.equals(otherConfig.authTargetMapping);

        }
        return false;
    }

    @Override
    public int hashCode() {
        /*
        not including the AuthTargetMapping as the uriElement should be unique per STS deployment.
         */
        return (service.toString() + port.toString() + uriElement + wsdlLocation + realm).hashCode();

    }
}
