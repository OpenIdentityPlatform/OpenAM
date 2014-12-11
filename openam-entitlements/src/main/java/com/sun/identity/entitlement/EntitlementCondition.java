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
 * Portions copyright 2010-2014 ForgeRock AS.
 */
package com.sun.identity.entitlement;

import javax.security.auth.Subject;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates a Strategy to decide if a {@link com.sun.identity.entitlement.Privilege} applies to a given request.
 *
 * @supported.all.api
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
     * Sets state of this object from a JSON string.
     *
     * @param state State of the object encoded as a JSON string
     */
    void setState(String state);

    /**
     * Returns state of the object encoded as a JSON string.
     *
     * @return state of the object encoded as a JSON string.
     */
    String getState();

    /**
     * Checks that this condition is configured correctly. Throws {@link EntitlementException} if not with an
     * informative message to display to the user creating/updating the policy.
     *
     * @throws EntitlementException if the configuration state is not valid.
     */
    void validate() throws EntitlementException;

    /**
     * Returns condition decision.
     *
     * @param realm Realm Name.
     * @param subject Subject who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return resulting condition decision.
     * @throws EntitlementException if cannot get condition decision.
     */
    ConditionDecision evaluate(
        String realm,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment)
        throws EntitlementException;
}
