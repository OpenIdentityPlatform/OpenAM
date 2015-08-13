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

package org.forgerock.openam.sts.soap.config.user;

import org.forgerock.guava.common.base.Objects;
import org.forgerock.json.JsonValue;

import javax.xml.namespace.QName;

import org.forgerock.openam.shared.sts.SharedSTSConstants;
import org.forgerock.openam.sts.config.user.DeploymentConfig;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * This class represents the deployment configuration for an STS instance. This includes:
 * 1. the wsdl location (which will determine the SecurityPolicy bindings)
 * 2. The serviceQName and portQName QNames
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
    static final String SERVICE_QNAME = SharedSTSConstants.SERVICE_QNAME;
    static final String PORT_QNAME = SharedSTSConstants.PORT_QNAME;
    static final String WSDL_LOCATION = SharedSTSConstants.WSDL_LOCATION;
    static final String AM_DEPLOYMENT_URL = SharedSTSConstants.AM_DEPLOYMENT_URL;
    static final String CUSTOM_WSDL_LOCATION = SharedSTSConstants.CUSTOM_WSDL_LOCATION;
    static final String CUSTOM_SERVICE_QNAME = SharedSTSConstants.CUSTOM_SERVICE_QNAME;
    static final String CUSTOM_PORT_QNAME = SharedSTSConstants.CUSTOM_PORT_QNAME;

    /*
    The AdminUI will allow users to publish soap-sts instances with a set of pre-deployed wsdl files. These wsdl files will
    define the WS-Trust defined interface, and a specific SecurityPolicy binding. However, we must support the specification
    of a custom wsdl file deployed in the soap-sts .war file. Thus the soap-sts AdminUI
    configuration page will all users to specify a custom wsdl-location(in addition to the standard selections)
    in the drop-down defining the wsdl-location, and free-form text-entry fields where the end-users can specify the
    actual name of the custom serviceQName/portQName/wsdl-file.
    The bottom line is that we want to constrain the 80% of the use-cases, while still allowing for ultimate flexibility.
    When a custom wsdl file is not specified, the AdminUI will set the serviceQName name and portQName values to the standard values
    defined in our standard wsdl files. When a custom wsdl file is indicated, then the fields from the custom wsdl location, portQName and
    serviceQName names will be used.

    The WSDL_LOCATION field will always be set - it will be set to either one of the standard locations, or to
    CUSTOM_SOAP_STS_WSDL_FILE_INDICATOR. If set to CUSTOM_SOAP_STS_WSDL_FILE_INDICATOR, then serviceQName and portQName
    will be null, and the customWsdlLocation, and custom port and service QNames set, and when the deployment-wsdl-location
    is not set to CUSTOM_SOAP_STS_WSDL_FILE_INDICATOR, then the deployment-service-name and deployment-service-port values
    will be set, and the custom equivalents will be null.
     */
    public static final String CUSTOM_SOAP_STS_WSDL_FILE_INDICATOR = SharedSTSConstants.CUSTOM_WSDL_FILE_INDICATOR;

    abstract static class SoapDeploymentConfigBuilderBase<T extends SoapDeploymentConfigBuilderBase<T>>
            extends DeploymentConfig.DeploymentConfigBuilderBase<T> {
        private QName serviceQName;
        private QName portQName;
        private String wsdlLocation;
        private String amDeploymentUrl;
        private String customWsdlLocation;
        private QName customPortQName;
        private QName customServiceQName;

        protected abstract T self();

        /**
         * Called to set the serviceQName QName for standard wsdl deployments
         * @param service the QName of the wsdl serviceQName
         * @return the builder
         */
        public T serviceQName(QName service)  {
            this.serviceQName = service;
            return self();
        }

        /**
         * Called to set the serviceQName qname for custom wsdl deployments
         * @param service the to-be-exposed serviceQName in the custom wsdl
         * @return the builder
         */
        public T customServiceQName(QName service)  {
            this.customServiceQName = service;
            return self();
        }

        /**
         * Called to set the portQName QName for standard wsdl deployments
         * @param port the QName of the wsdl portQName
         * @return the builder
         */
        public T portQName(QName port)  {
            this.portQName = port;
            return self();
        }

        /**
         * Called to set the portQName qname for custom wsdl deployments
         * @param port the to-be-exposed portQName in the custom wsdl
         * @return the builder
         */
        public T customPortQName(QName port)  {
            this.customPortQName = port;
            return self();
        }
        /**
         * This method is called to deploy one of the standard wsdl files bundled with OpenAM
         * @param wsdlLocation the name of one of the bundled wsdl files
         * @return the builder
         */
        public T wsdlLocation(String wsdlLocation)  {
            this.wsdlLocation = wsdlLocation;
            return self();
        }

        /**
         * This method is called to deploy a user-specified wsdl file
         * @param customWsdlLocation the name of the user-specified wsdl file
         * @return the builder
         */
        public T customWsdlLocation(String customWsdlLocation) {
            if (customWsdlLocation != null) {
                this.customWsdlLocation = customWsdlLocation;
                this.wsdlLocation = CUSTOM_SOAP_STS_WSDL_FILE_INDICATOR;
            }
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

    private final QName serviceQName;
    private final QName portQName;
    private final String wsdlLocation;
    private final String amDeploymentUrl;
    private final String customWsdlLocation;
    private final QName customServiceQName;
    private final QName customPortQName;

    private SoapDeploymentConfig(SoapDeploymentConfigBuilderBase<?> builder) {
        super(builder);
        this.serviceQName = builder.serviceQName; //might be null in custom wsdl case
        this.portQName = builder.portQName; //might be null in custom wsdl case
        this.wsdlLocation = builder.wsdlLocation;
        this.amDeploymentUrl = builder.amDeploymentUrl;
        this.customWsdlLocation = builder.customWsdlLocation; //might be null
        this.customPortQName = builder.customPortQName; //might be null
        this.customServiceQName = builder.customServiceQName; //might be null
        Reject.ifNull(amDeploymentUrl, "AM Deployment url cannot be null");
        Reject.ifNull(wsdlLocation, "wsdlLocation String cannot be null");
        /*
        The AdminUI allows users to select one of the standard .wsdl files, or to make a selection that indicates that
        they want to enter a custom wsdl location. If they have made this selection, the other necessary pieces of
        information must be specified.
         */
        if (CUSTOM_SOAP_STS_WSDL_FILE_INDICATOR.equals(wsdlLocation)) {
            if (StringUtils.isBlank(customWsdlLocation) || (customPortQName == null) || (customServiceQName == null)) {
                throw new IllegalArgumentException("The wsdlLocation of " + wsdlLocation + " indicates the specification of " +
                        "a custom wsdl location, which requires this custom location, the to-be-deployed serviceQName-portQName, and " +
                        "the to-be-deployed serviceQName, to be specified.");
            }
        }
    }

    public static SoapDeploymentConfigBuilder builder() {
        return new SoapDeploymentConfigBuilder();
    }

    public QName getService() {
        if (CUSTOM_SOAP_STS_WSDL_FILE_INDICATOR.equals(wsdlLocation)) {
            return customServiceQName;
        }
        return serviceQName;
    }

    public QName getPort() {
        if (CUSTOM_SOAP_STS_WSDL_FILE_INDICATOR.equals(wsdlLocation)) {
            return customPortQName;
        }
        return portQName;
    }

    public String getAmDeploymentUrl() {
        return amDeploymentUrl;
    }

    public String getWsdlLocation() {
        if (CUSTOM_SOAP_STS_WSDL_FILE_INDICATOR.equals(wsdlLocation)) {
            return customWsdlLocation;
        }
        return wsdlLocation;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SoapDeploymentConfig instance: ").append('\n');
        sb.append('\t').append("Base class: ").append(super.toString()).append('\n');
        sb.append('\t').append("Service QName: ").append(serviceQName).append('\n');
        sb.append('\t').append("Port QName: ").append(portQName).append('\n');
        sb.append('\t').append("Custom Service QName: ").append(customServiceQName).append('\n');
        sb.append('\t').append("Custom Port QName: ").append(customPortQName).append('\n');
        sb.append('\t').append("wsdlLocation: ").append(wsdlLocation).append('\n');
        sb.append('\t').append("custom wsdlLocation: ").append(customWsdlLocation).append('\n');
        sb.append('\t').append("OpenAM Deployment Url: ").append(amDeploymentUrl).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SoapDeploymentConfig) {
            SoapDeploymentConfig otherConfig = (SoapDeploymentConfig) other;
            return  super.equals(otherConfig) &&
                    Objects.equal(serviceQName, otherConfig.serviceQName) &&
                    Objects.equal(portQName, otherConfig.portQName) &&
                    wsdlLocation.equals(otherConfig.wsdlLocation) &&
                    Objects.equal(customWsdlLocation, otherConfig.customWsdlLocation) &&
                    Objects.equal(customServiceQName, otherConfig.customServiceQName) &&
                    Objects.equal(customPortQName, otherConfig.customPortQName) &&
                    amDeploymentUrl.equals(otherConfig.getAmDeploymentUrl());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (super.toString() + amDeploymentUrl + wsdlLocation).hashCode();

    }

    /**
     * Method used to marshal the SoapDeploymentConfig instance into the json format required to programmatically publish
     * soap-sts instances.
     * @return json format required to programmatically publish soap-sts instances.
     */
    public JsonValue toJson() {
        JsonValue baseValue = super.toJson();
        baseValue.add(SERVICE_QNAME, serviceQName != null ? serviceQName.toString() : null);
        baseValue.add(PORT_QNAME, portQName != null ? portQName.toString() : null);
        baseValue.add(WSDL_LOCATION, wsdlLocation);
        baseValue.add(CUSTOM_WSDL_LOCATION, customWsdlLocation);
        baseValue.add(CUSTOM_PORT_QNAME, customPortQName != null ? customPortQName.toString() : null);
        baseValue.add(CUSTOM_SERVICE_QNAME, customServiceQName != null ? customServiceQName.toString() : null);
        baseValue.add(AM_DEPLOYMENT_URL, amDeploymentUrl);
        return baseValue;
    }

    /**
     * Used by the sts-publish serviceQName to marshal the json representation of SoapDeploymentConfig instances back to their
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
                .portQName(json.get(PORT_QNAME).isString() ? QName.valueOf(json.get(PORT_QNAME).asString()) : null)
                .serviceQName(json.get(SERVICE_QNAME).isString() ? QName.valueOf(json.get(SERVICE_QNAME).asString()) : null)
                .customWsdlLocation(json.get(CUSTOM_WSDL_LOCATION).isString() ? json.get(CUSTOM_WSDL_LOCATION).asString() : null)
                .customPortQName(json.get(CUSTOM_PORT_QNAME).isString() ? QName.valueOf(json.get(CUSTOM_PORT_QNAME).asString()) : null)
                .customServiceQName(json.get(CUSTOM_SERVICE_QNAME).isString() ? QName.valueOf(json.get(CUSTOM_SERVICE_QNAME).asString()) : null)
                .wsdlLocation(json.get(WSDL_LOCATION).asString());
        return builder.build();
    }

    /**
     * Used by the sts-publish serviceQName to marshal a SoapDeploymentConfig instance to the Map<String, Set<String>>
     * representation required by the SMS.
     * @return a Map containing the state of the SoapDeploymentConfig instance in the format consumed by the SMS.
     */
    public Map<String, Set<String>> marshalToAttributeMap() {
        Map<String, Set<String>> baseMap = super.marshalToAttributeMap();
        baseMap.put(SERVICE_QNAME, serviceQName != null ? CollectionUtils.asSet(serviceQName.toString()) : Collections.<String>emptySet());
        baseMap.put(PORT_QNAME, portQName != null ? CollectionUtils.asSet(portQName.toString()) : Collections.<String>emptySet());
        baseMap.put(WSDL_LOCATION, CollectionUtils.asSet(wsdlLocation));
        baseMap.put(CUSTOM_WSDL_LOCATION, customWsdlLocation != null ? CollectionUtils.asSet(customWsdlLocation) : Collections.<String>emptySet());
        baseMap.put(CUSTOM_PORT_QNAME, customPortQName != null ? CollectionUtils.asSet(customPortQName.toString()) : Collections.<String>emptySet());
        baseMap.put(CUSTOM_SERVICE_QNAME, customServiceQName != null ? CollectionUtils.asSet(customServiceQName.toString()) : Collections.<String>emptySet());
        baseMap.put(AM_DEPLOYMENT_URL, CollectionUtils.asSet(amDeploymentUrl));
        return baseMap;
    }

    /**
     * Used by the sts-publish serviceQName to marshal the Map<String, Set<String>> returned by the SMS to a SoapDeploymentConfig
     * instance. Used as part of generating the json representation of published soap-sts instances returned by the
     * sts-publish serviceQName.
     * @return A SoapDeploymentConfig instance corresponding to Map state.
     */
    public static SoapDeploymentConfig marshalFromAttributeMap(Map<String, Set<String>> attributeMap) {
        DeploymentConfig baseConfig = DeploymentConfig.marshalFromAttributeMap(attributeMap);
        SoapDeploymentConfigBuilder builder = SoapDeploymentConfig.builder()
                .authTargetMapping(baseConfig.getAuthTargetMapping())
                .offloadedTwoWayTLSHeaderKey(baseConfig.getOffloadedTwoWayTlsHeaderKey())
                .realm(baseConfig.getRealm())
                .tlsOffloadEngineHostIpAddrs(baseConfig.getTlsOffloadEngineHostIpAddrs())
                .uriElement(baseConfig.getUriElement())
                .amDeploymentUrl(CollectionUtils.getFirstItem(attributeMap.get(AM_DEPLOYMENT_URL), null));
        handleCustomWsdlLocationSettings(builder, attributeMap);

        return builder.build();
    }

    /*
    End-users can specify either a standard, packaged wsdl file with pre-defined SecurityPolicy bindings, or a custom
    wsdl file. If they specify a custom wsdl file, then they need to specify the custom serviceQName and portQName QNames which identify
    the serviceQName in their custom wsdl file. If they do not select a custom wsdl file, then the standard serviceQName and portQName
    settings will be used. The AdminUI handles the setting of these values, and callers of the programmatic publish must
    also handle setting these values.
     */
    private static void handleCustomWsdlLocationSettings(SoapDeploymentConfigBuilder builder, Map<String, Set<String>> attributeMap) {
        final String wsdlLocation = CollectionUtils.getFirstItem(attributeMap.get(WSDL_LOCATION), null);
        /*
        if we enter this branch, then a custom wsdl file was selected. This means we have to set the custom wsdl location
         */
        if (CUSTOM_SOAP_STS_WSDL_FILE_INDICATOR.equals(wsdlLocation)) {
            builder.customWsdlLocation(CollectionUtils.getFirstItem(attributeMap.get(CUSTOM_WSDL_LOCATION), null));
            builder.customPortQName(QName.valueOf(CollectionUtils.getFirstItem(attributeMap.get(CUSTOM_PORT_QNAME), null)));
            builder.customServiceQName(QName.valueOf(CollectionUtils.getFirstItem(attributeMap.get(CUSTOM_SERVICE_QNAME), null)));
        } else {
            builder.wsdlLocation(wsdlLocation);
            builder.portQName(QName.valueOf(CollectionUtils.getFirstItem(attributeMap.get(PORT_QNAME), null)));
            builder.serviceQName(QName.valueOf(CollectionUtils.getFirstItem(attributeMap.get(SERVICE_QNAME), null)));
        }
    }
}
