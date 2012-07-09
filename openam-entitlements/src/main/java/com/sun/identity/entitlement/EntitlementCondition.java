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
 * $Id: EntitlementCondition.java,v 1.2 2009/09/05 00:24:04 veiming Exp $
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.entitlement;

import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * Interface specification for entitlement <code>EntitlementCondition</code>
 */
public interface EntitlementCondition {

    /**
     * Sets display type.
     *
     * @param displayType Display Type.
     */
    void setDisplayType(String displayType);

    /**
     * Returns display type.
     *
     * @return Display Type.
     */
    String getDisplayType();

    /**
     * Initializes the condition object.
     *
     * @param parameters Parameters for initializing the condition.
     */
    void init(Map<String, Set<String>> parameters);

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
     * Returns condition decision.
     *
     * @param realm Realm Name
     * @param subject Subject who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return resulting condition decision
     * @throws EntitlementException if can not get condition decision.
     */
    ConditionDecision evaluate(
        String realm,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment)
        throws EntitlementException;
}
