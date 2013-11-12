/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: Evaluator.java,v 1.2 2009/09/10 16:35:38 veiming Exp $
 *
 * Portions copyright 2013 ForgeRock AS.
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.util.NetworkMonitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * The class evaluates entitlement request and provides decisions.
 * @supported.api
 */
public class Evaluator {

    // Application retrieval log message code.
    private static final int APP_RETRIEVAL_ERROR = 248;

    private Subject adminSubject;
    private String applicationName =
        ApplicationTypeManager.URL_APPLICATION_TYPE_NAME;

    public static final int DEFAULT_POLICY_EVAL_THREAD = 10;

    // Statistics monitors
    private static final NetworkMonitor HAS_ENTITLEMENT_MONITOR =
        NetworkMonitor.getInstance("hasEntitltmentMonitor");
    private static final NetworkMonitor EVAL_SINGLE_LEVEL_MONITOR =
        NetworkMonitor.getInstance("evalSingleLevelMonitor");
    private static final NetworkMonitor EVAL_SUB_TREE_MONITOR =
        NetworkMonitor.getInstance("evalSubTreeMonitor");
    
    /**
     * Constructor to create an evaluator of default service type.
     *
     * @throws EntitlementException if any other abnormal condition occ.
     */
    private Evaluator()
        throws EntitlementException {
    }

    /**
     * Constructor to create an evaluator given the service type.
     *
     * @param subject Subject who credential is used for performing the 
     *        evaluation.
     * @param applicationName the name of the aplication for
     *        which this evaluator can be used.
     * @throws EntitlementException if any other abnormal condition occured.
     */
    public Evaluator(Subject subject, String applicationName)
        throws EntitlementException {
        adminSubject = subject;
        this.applicationName = applicationName;
    }

    /**
     * Constructor to create an evaluator the default service type.
     *
     * @param subject Subject who credential is used for performing the 
     *        evaluation.
     * @throws EntitlementException if any other abnormal condition occured.
     */
    public Evaluator(Subject subject)
        throws EntitlementException {
        adminSubject = subject;
    }
    
    /**
     * Returns <code>true</code> if the subject is granted to an
     * entitlement.
     *
     * @param realm Realm name.
     * @param subject Subject who is under evaluation.
     * @param e Entitlement object which describes the resource name and 
     *          actions.
     * @param envParameters Map of environment parameters.
     * @return <code>true</code> if the subject is granted to an
     *         entitlement.
     * @throws EntitlementException if the result cannot be determined.
     */
    public boolean hasEntitlement(
        String realm,
        Subject subject, 
        Entitlement e,
        Map<String, Set<String>> envParameters
    ) throws EntitlementException {
        long start = HAS_ENTITLEMENT_MONITOR.start();

        PrivilegeEvaluator evaluator = new PrivilegeEvaluator();
        boolean result = evaluator.hasEntitlement(realm,
            adminSubject, subject, applicationName, e, envParameters);

        HAS_ENTITLEMENT_MONITOR.end(start);
        return result;
    }

    /**
     * Returns a list of entitlements for a given subject, resource names
     * and environment.
     *
     * @param realm Realm Name.
     * @param subject Subject who is under evaluation.
     * @param resourceNames Resource names.
     * @param environment Environment parameters.
     * @return a list of entitlements for a given subject, resource name
     *         and environment.
     * @throws EntitlementException if the result cannot be determined.
     */
    public List<Entitlement> evaluate(
        String realm,
        Subject subject,
        Set<String> resourceNames,
        Map<String, Set<String>> environment
    ) throws EntitlementException {
        if ((resourceNames == null) || resourceNames.isEmpty()) {
            throw new EntitlementException(424);
        }

        List<Entitlement> results = new ArrayList<Entitlement>();

        for (String res : resourceNames) {
            List<Entitlement> r = evaluate(realm, subject, res, environment,
                false);
            if ((r != null) && !r.isEmpty()) {
                results.addAll(r);
            }
        }
        return results;
    }

    /**
     * Returns a list of entitlements for a given subject, resource name
     * and environment.
     *
     * @param realm
     *         Realm Name.
     * @param subject
     *         Subject who is under evaluation.
     * @param resourceName
     *         Resource name.
     * @param environment
     *         Environment parameters.
     * @param recursive
     *         <code>true</code> to perform evaluation on sub resources
     *         from the given resource name.
     * @return a list of entitlements for a given subject, resource name
     *         and environment.
     * @throws EntitlementException
     *         if the result cannot be determined.
     */
    public List<Entitlement> evaluate(
            String realm,
            Subject subject,
            String resourceName,
            Map<String, Set<String>> environment,
            boolean recursive
    ) throws EntitlementException {
        long start = (recursive) ? EVAL_SUB_TREE_MONITOR.start() : EVAL_SINGLE_LEVEL_MONITOR.start();

        Application application = ApplicationManager.getApplication(adminSubject, realm, applicationName);

        if (application == null) {
            // App retrieval error.
            throw new EntitlementException(APP_RETRIEVAL_ERROR, new String[] {realm});
        }

        // Normalise the incoming resource URL.
        resourceName = application.getResourceComparator().canonicalize(resourceName);

        PrivilegeEvaluator evaluator = new PrivilegeEvaluator();
        List<Entitlement> results = evaluator.evaluate(realm, adminSubject, subject,
                applicationName, resourceName, environment, recursive);

        if (recursive) {
            EVAL_SUB_TREE_MONITOR.end(start);
        } else {
            EVAL_SINGLE_LEVEL_MONITOR.end(start);
        }

        return results;
    }

    /**
     * Returns application name.
     * 
     * @return application name.
     */
    public String getApplicationName() {
        return applicationName;
    }
}

