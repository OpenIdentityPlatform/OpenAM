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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.core.rest.sms;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Named;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import com.google.inject.Inject;
import com.sun.identity.authentication.util.ISAuthConstants;

/**
 * Responsible for answering the question of whether a service should be handled by the
 * SMS layer request handler, or whether it has its own specific request handler.
 *
 * Services which do not define specific handling will be handled by the SMS Root Tree
 * in an appropriate manner.
 */
public class SmsServiceHandlerFunction implements Predicate<String> {

    public static final String COT_CONFIG_SERVICE = "sunFMCOTConfigService";
    public static final String IDFF_METADATA_SERVICE = "sunFMIDFFMetadataService";
    public static final String SAML2_METADATA_SERVICE = "sunFMSAML2MetadataService";
    public static final String WS_METADATA_SERVICE = "sunFMWSFederationMetadataService";

    public final Predicate<String> CIRCLES_OF_TRUST_HANDLES_FUNCTION;
    public final Predicate<String> AUTHENTICATION_HANDLES_FUNCTION;
    public final Predicate<String> AUTHENTICATION_CHAINS_HANDLES_FUNCTION;
    public final Predicate<String> ENTITYPROVIDER_HANDLES_FUNCTION;
    public final Predicate<String> AUTHENTICATION_MODULE_HANDLES_FUNCTION;
    public final Predicate<String> AGENTS_MODULE_HANDLES_FUNCTION;

    /**
     * List of services which are known to have their own handling registered.
     */
    private final List<Predicate<String>> ALREADY_HANDLED;

    @Inject
    public SmsServiceHandlerFunction(@Named("authenticationServices") Set<String> authenticationServiceNames) {

        CIRCLES_OF_TRUST_HANDLES_FUNCTION = new SingleServiceFunction(COT_CONFIG_SERVICE);
        AUTHENTICATION_HANDLES_FUNCTION = new SingleServiceFunction(ISAuthConstants.AUTH_SERVICE_NAME);
        AUTHENTICATION_CHAINS_HANDLES_FUNCTION = new SingleServiceFunction(ISAuthConstants.AUTHCONFIG_SERVICE_NAME);
        AGENTS_MODULE_HANDLES_FUNCTION = new SingleServiceFunction(ISAuthConstants.AGENT_SERVICE_NAME);
        ENTITYPROVIDER_HANDLES_FUNCTION = new MultiServiceFunction(
                IDFF_METADATA_SERVICE, SAML2_METADATA_SERVICE, WS_METADATA_SERVICE);
        AUTHENTICATION_MODULE_HANDLES_FUNCTION = new MultiServiceFunction(authenticationServiceNames);

        ALREADY_HANDLED = Arrays.asList(
                AUTHENTICATION_HANDLES_FUNCTION,
                AUTHENTICATION_CHAINS_HANDLES_FUNCTION,
                AUTHENTICATION_MODULE_HANDLES_FUNCTION,
                CIRCLES_OF_TRUST_HANDLES_FUNCTION,
                ENTITYPROVIDER_HANDLES_FUNCTION,
                AGENTS_MODULE_HANDLES_FUNCTION
        );
    }

    /**
     * @param serviceName Non null name of the service to test.
     * @return True if the service requires handling, false indicates it is already handled.
     */
    @Override
    public boolean apply(String serviceName) {
        for (Predicate<String> handled : ALREADY_HANDLED) {
            if (handled.apply(serviceName)) {
                return false;
            }
        }
        return true;
    }

    private static final class SingleServiceFunction implements Predicate<String> {

        private final String serviceName;

        SingleServiceFunction(String serviceName) {
            this.serviceName = serviceName;
        }

        @Nullable
        @Override
        public boolean apply(String name) {
            return serviceName.equals(name);
        }
    }

    private static final class MultiServiceFunction implements Predicate<String> {
        private final Collection<String> serviceNames;

        MultiServiceFunction(String... serviceNames) {
            this(Arrays.asList(serviceNames));
        }

        MultiServiceFunction(Collection<String> serviceNames) {
            this.serviceNames = serviceNames;
        }

        @Nullable
        @Override
        public boolean apply(String name) {
            return serviceNames.contains(name);
        }
    }
}