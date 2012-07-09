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
 * $Id: IPrivilege.java,v 1.2 2009/11/19 01:02:03 veiming Exp $
 */

package com.sun.identity.entitlement;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * Class that implements this interface has a evaluate method.
 */
public interface IPrivilege {
    /**
     * Returns name.
     *
     * @return name.
     */
    String getName();

    /**
     * Returns resource save indexes.
     *
     * @param adminSubject Admin Subject.
     * @param realm Realm Name
     * @return resource save indexes.
     */
    ResourceSaveIndexes getResourceSaveIndexes(
        Subject adminSubject,
        String realm
    ) throws EntitlementException;
    
    /**
     * Returns a list of entitlement for a given subject, resource name
     * and environment.
     *
     * @param adminSubject Admin Subject
     * @param realm Realm Name
     * @param subject Subject who is under evaluation.
     * @param applicationName Application name.
     * @param resourceName Resource name.
     * @param actionNames Set of action names.
     * @param environment Environment parameters.
     * @param recursive <code>true</code> to perform evaluation on sub resources
     *        from the given resource name.
     * @param context A security context
     * @return a list of entitlement for a given subject, resource name
     *         and environment.
     * @throws EntitlementException if the result cannot be determined.
     */
    List<Entitlement> evaluate(
        Subject adminSubject,
        String realm,
        Subject subject,
        String applicationName,
        String resourceName,
        Set<String> actionNames,
        Map<String, Set<String>> environment,
        boolean recursive,
        Object context) throws EntitlementException;
}
