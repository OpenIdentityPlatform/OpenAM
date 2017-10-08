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

package org.forgerock.openam.xacml.v3;

import static com.sun.identity.entitlement.ApplicationTypeManager.*;
import static com.sun.identity.entitlement.xacml3.XACMLPrivilegeUtils.getRuleCombiningAlgId;

import java.util.Collections;
import java.util.Set;

import javax.security.auth.Subject;

import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.utils.EntitlementUtils;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.xacml3.XACMLPrivilegeUtils;
import com.sun.identity.entitlement.xacml3.core.Policy;

/**
 * Helper class to map from Application to XACML.
 *
 * @since 13.5.0
 */
public final class XACMLApplicationUtils {

    private XACMLApplicationUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Service class for the creation of Application Types.
     */
    public static class ApplicationTypeService {
        /**
         * Get the application type already present in the system. Application Type is matched first based on the name
         * of the application if not found then matched based on ApplicationTypeManager.URL_APPLICATION_TYPE_NAME.
         *
         * @param subject
         *         Admin subject.
         * @param applicationName
         *         Name of the application for the request is made.
         *
         * @return ApplicationType retrieved.o
         *
         * @throws EntitlementException
         *         if no results found.
         */
        public ApplicationType getApplicationType(Subject subject, String applicationName)
                throws EntitlementException {
            ApplicationType applicationType = getAppplicationType(subject, applicationName);
            if (applicationType == null) {
                applicationType = getAppplicationType(subject, URL_APPLICATION_TYPE_NAME);
            }

            if (applicationType == null) {
                throw new EntitlementException(
                        EntitlementException.APPLICATION_TYPE_NOT_FOUND, applicationName, URL_APPLICATION_TYPE_NAME);
            }

            return applicationType;
        }

    }

    /**
     * Creates an Application instance from the attributes of the Policy instance.
     *
     * @param policy
     *         Policy instance from which the attributes will be copied.
     *
     * @return Application instance created.
     *
     * @throws EntitlementException
     *         If there was any unexpected error.
     */
    public static Application policyToApplication(Policy policy) throws EntitlementException {
        String applicationName = getApplicationNameFromPolicy(policy);

        Application application = new Application();
        application.setName(applicationName);
        application.setDescription(generateDefaultApplicationDescription(applicationName));
        application.setEntitlementCombiner(getRuleCombiningAlg(applicationName));
        application.setSubjects(EntitlementUtils.getSubjectsShortNames());
        application.setConditions(EntitlementUtils.getConditionsShortNames());

        return application;
    }

    /**
     * Gets application name from the policy.
     *
     * @param policy
     *         from which the application name has to be read.
     *
     * @return application name
     */
    public static String getApplicationNameFromPolicy(Policy policy) {
        return XACMLPrivilegeUtils.getApplicationNameFromPolicy(policy);
    }

    /**
     * Copies attributes from source application instance to destination instance.
     *
     * @param sourceApp
     *         application instance from attributes are copied.
     * @param destApp
     *         application instance to which the attributes are copied.
     */
    public static void copyAttributes(Application sourceApp, Application destApp) {
        if (destApp == null || sourceApp == null) {
            return;
        }

        destApp.addAllResourceTypeUuids(sourceApp.getResourceTypeUuids());
    }

    private static String generateDefaultApplicationDescription(String applicationName) {
        return "Policy Set " + applicationName;
    }

    private static Class getRuleCombiningAlg(String applicationName) {
        // EntitlementUtils line may log the following ERROR message however, the default value
        //  returned would be good enough to support the feature.
        //  ERROR: EntitlementService.getEntitlementCombiner
        //  java.lang.ClassNotFoundException: urn:oasis:names:tc:xacml:3.0:rule-combining-algorithm:deny-overrides
        //  ..
        //  at org.forgerock.openam.entitlement.utils.EntitlementUtils.getEntitlementCombiner(EntitlementUtils.java:437)
        return EntitlementUtils.getEntitlementCombiner(getRuleCombiningAlgId(applicationName));
    }

}
