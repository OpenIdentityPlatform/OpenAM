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

import org.forgerock.json.fluent.JsonValue;

import javax.xml.namespace.QName;

import org.forgerock.openam.sts.config.user.DeploymentConfig;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.Reject;

import java.util.Map;
import java.util.Set;

/**
 * This class represents the deployment configuration for an STS instance. This includes:
 * 1. the wsdl location (which will determine the SecurityPolicy bindings)
 * 2. The service and port QNames
 * 3. The uri element (e.g. /realm1/accounting/bobo) at which the STS instance will be deployed
 * 4. The realm within which the STS is deployed.
 *
 * This class extends RestDeploymentConfig as it will share all of the same configuration parameters. Regarding the
 * configuration of the x509 header and offload hosts for the x509 transform case, it should be possible to access and
 * validate this state in a WS-Trust compliant manner by pulling the x509 cert directly out of the header, and by confirming
 * the caller is among the tls-offload-host-set by looking at the HttpServletRequest.
 *
 */
public class SoapDeploymentConfig extends DeploymentConfig {
    /*
    Statics used to marshal to and from json and the Map<String, Set<String>> required by the SMS. Must maintain correspondence
    to values defined in soapSTS.xml
     */
    static final String SERVICE_QNAME = "deployment-service-name";
    static final String SERVICE_PORT = "deployment-service-port";
    static final String WSDL_LOCATION = "deployment-wsdl-location";
    static final String AM_DEPLOYMENT_URL = "deployment-am-url";
    abstract static class SoapDeploymentConfigBuilderBase<T extends SoapDeploymentConfigBuilderBase<T>>
            extends DeploymentConfig.DeploymentConfigBuilderBase<T> {
        private QName service;
        private QName port;
        private String wsdlLocation;
        private String amDeploymentUrl;

        protected abstract T self();

        public T serviceQName(QName service)  {
            this.service = service;
            return self();
        }

        public T portQName(QName port)  {
            this.port = port;
            return self();
        }

        public T wsdlLocation(String wsdlLocation)  {
            this.wsdlLocation = wsdlLocation;
            return self();
        }

        public T amDeploymentUrl(String amDeploymentUrl)  {
            this.amDeploymentUrl = amDeploymentUrl;
            return self();
        }

        public SoapDeploymentConfig build() {
            return new SoapDeploymentConfig(this);
        }
    }

    public static class SoapDeploymentConfigBuilder extends SoapDeploymentConfigBuilderBase<SoapDeploymentConfigBuilder> {
        @Override
        protected SoapDeploymentConfigBuilder self() {
            return this;
        }
    }

    private final QName service;
    private final QName port;
    private final String wsdlLocation;
    private final String amDeploymentUrl;

    private SoapDeploymentConfig(SoapDeploymentConfigBuilderBase<?> builder) {
        super(builder);
        this.service = builder.service;
        this.port = builder.port;
        this.wsdlLocation = builder.wsdlLocation;
        this.amDeploymentUrl = builder.amDeploymentUrl;
        Reject.ifNull(amDeploymentUrl, "AM Deployment url cannot be null");
        Reject.ifNull(service, "Service QName cannot be null");
        Reject.ifNull(port, "Port QName cannot be null");
        Reject.ifNull(wsdlLocation, "wsdlLocation String cannot be null");
    }

    public static SoapDeploymentConfigBuilder builder() {
        return new SoapDeploymentConfigBuilder();
    }

    public QName getService() {
        return service;
    }

    public QName getPort() {
        return port;
    }

    public String getAmDeploymentUrl() {
        return amDeploymentUrl;
    }

    public String getWsdlLocation() {
        return wsdlLocation;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SoapDeploymentConfig instance: ").append('\n');
        sb.append('\t').append("Base class: ").append(super.toString()).append('\n');
        sb.append('\t').append("Service QName: ").append(service).append('\n');
        sb.append('\t').append("Port QName: ").append(port).append('\n');
        sb.append('\t').append("wsdlLocation: ").append(wsdlLocation).append('\n');
        sb.append('\t').append("OpenAM Deployment Url: ").append(amDeploymentUrl).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SoapDeploymentConfig) {
            SoapDeploymentConfig otherConfig = (SoapDeploymentConfig) other;
            return  super.equals(otherConfig) &&
                    service.equals(otherConfig.getService()) &&
                    port.equals(otherConfig.getPort()) &&
                    wsdlLocation.equals(otherConfig.getWsdlLocation()) &&
                    amDeploymentUrl.equals(otherConfig.getAmDeploymentUrl());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (super.toString() + service.toString() + port.toString() + wsdlLocation).hashCode();

    }

    /**
     * Method used to marshal the SoapDeploymentConfig instance into the json format required to programmatically publish
     * soap-sts instances.
     * @return json format required to programmatically publish soap-sts instances.
     */
    public JsonValue toJson() {
        JsonValue baseValue = super.toJson();
        baseValue.add(SERVICE_QNAME, service.toString());
        baseValue.add(SERVICE_PORT, port.toString());
        baseValue.add(WSDL_LOCATION, wsdlLocation);
        baseValue.add(AM_DEPLOYMENT_URL, amDeploymentUrl);
        return baseValue;
    }

    /**
     * Used by the sts-publish service to marshal the json representation of SoapDeploymentConfig instances back to their
     * native formation prior to SMS persistence.
     * @param json the json representation of the SoapDeploymentConfig instance
     * @return the SoapDeploymentConfig instance corresponding to the input json.
     */
    public static SoapDeploymentConfig fromJson(JsonValue json) {
        if (json == null) {
            throw new NullPointerException("JsonValue passed to SoapDeploymentConfig#fromJson cannot be null!");
        }
        DeploymentConfig baseConfig = DeploymentConfig.fromJson(json);
        SoapDeploymentConfigBuilderBase<?> builder = SoapDeploymentConfig.builder()
                .authTargetMapping(baseConfig.getAuthTargetMapping())
                .offloadedTwoWayTLSHeaderKey(baseConfig.getOffloadedTwoWayTlsHeaderKey())
                .realm(baseConfig.getRealm())
                .tlsOffloadEngineHostIpAddrs(baseConfig.getTlsOffloadEngineHostIpAddrs())
                .uriElement(baseConfig.getUriElement())
                .amDeploymentUrl(json.get(AM_DEPLOYMENT_URL).asString())
                .portQName(QName.valueOf(json.get(SERVICE_PORT).asString()))
                .serviceQName(QName.valueOf(json.get(SERVICE_QNAME).asString()))
                .wsdlLocation(json.get(WSDL_LOCATION).asString());
        return builder.build();
    }

    /**
     * Used by the sts-publish service to marshal a SoapDeploymentConfig instance to the Map<String, Set<String>>
     * representation required by the SMS.
     * @return a Map containing the state of the SoapDeploymentConfig instance in the format consumed by the SMS.
     */
    public Map<String, Set<String>> marshalToAttributeMap() {
        Map<String, Set<String>> baseMap = super.marshalToAttributeMap();
        baseMap.put(SERVICE_QNAME, CollectionUtils.asSet(service.toString()));
        baseMap.put(SERVICE_PORT, CollectionUtils.asSet(port.toString()));
        baseMap.put(WSDL_LOCATION, CollectionUtils.asSet(wsdlLocation));
        baseMap.put(AM_DEPLOYMENT_URL, CollectionUtils.asSet(amDeploymentUrl));
        return baseMap;
    }

    /**
     * Used by the sts-publish service to marshal the Map<String, Set<String>> returned by the SMS to a SoapDeploymentConfig
     * instance. Used as part of generating the json representation of published soap-sts instances returned by the
     * sts-publish service.
     * @return A SoapDeploymentConfig instance corresponding to Map state.
     */
    public static SoapDeploymentConfig marshalFromAttributeMap(Map<String, Set<String>> attributeMap) {
        DeploymentConfig baseConfig = DeploymentConfig.marshalFromAttributeMap(attributeMap);
        return SoapDeploymentConfig.builder()
                .authTargetMapping(baseConfig.getAuthTargetMapping())
                .offloadedTwoWayTLSHeaderKey(baseConfig.getOffloadedTwoWayTlsHeaderKey())
                .realm(baseConfig.getRealm())
                .tlsOffloadEngineHostIpAddrs(baseConfig.getTlsOffloadEngineHostIpAddrs())
                .uriElement(baseConfig.getUriElement())
                .amDeploymentUrl(CollectionUtils.getFirstItem(attributeMap.get(AM_DEPLOYMENT_URL), null))
                .portQName(QName.valueOf(CollectionUtils.getFirstItem(attributeMap.get(SERVICE_PORT), null)))
                .serviceQName(QName.valueOf(CollectionUtils.getFirstItem(attributeMap.get(SERVICE_QNAME), null)))
                .wsdlLocation(CollectionUtils.getFirstItem(attributeMap.get(WSDL_LOCATION), null))
                .build();
    }
}
