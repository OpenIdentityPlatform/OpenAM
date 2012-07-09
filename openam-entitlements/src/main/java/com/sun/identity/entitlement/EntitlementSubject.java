/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: EntitlementSubject.java,v 1.1 2009/08/19 05:40:32 veiming Exp $
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.entitlement;

import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * Interface specifiction for privilige subject
 * @author ddorai
 */
public interface EntitlementSubject {

    /**
     * Sets state of the object
     * @param state State of the object encoded as string
     */
    void setState(String state);

    /**
     * Returns state of the object
     * @return state of the object encoded as string
     */
    String getState();

    /**
     * Returns attribute names and values that could be used for indexing.
     * These values will be used by the authorization engine to obtain the
     * applicable policies for a given <class>Subject</class>.
     *
     * @return a maps of key-value pairs that will be used for indexing the
     * entitlements that contain this <class>EntitlementSubject</class>
     */
    Map<String, Set<String>> getSearchIndexAttributes();

    /**
     * Returns a set of attribute names that are used for evaluation.
     * During evaluation, the <class>Evaluator</class> would try to populate
     * these attributes in the <class>Subject</class> for the <class>
     * EntitlementSubject</class>'s consumption.
     *
     * @return a set of attributes that would be required by the <class>
     * EntitlementSubject</class>'s implementation
     */
    Set <String> getRequiredAttributeNames();

    /**
     * Returns <code>SubjectDecision</code> of
     * <code>EntitlementSubject</code> evaluation
     *
     * @param realm Realm name.
     * @param subject EntitlementSubject who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return <code>SubjectDecision</code> of
     * <code>EntitlementSubject</code> evaluation
     * @throws EntitlementException if any errors occur.
     */
    SubjectDecision evaluate(
        String realm,
        SubjectAttributesManager mgr,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment)
        throws EntitlementException;

    /**
     * Returns <code>true</code> is this subject is an identity object.
     *
     * @return <code>true</code> is this subject is an identity object.
     */
    boolean isIdentity();
}
