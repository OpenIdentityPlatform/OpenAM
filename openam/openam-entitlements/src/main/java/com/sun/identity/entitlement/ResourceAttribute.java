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
 * $Id: ResourceAttribute.java,v 1.1 2009/08/19 05:40:33 veiming Exp $
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.entitlement;

import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * Interface specification for entitlement <code>ResourceAttribute</code>
 */
public interface ResourceAttribute {

    /**
     * Sets property name.
     *
     * @param name property name.
     */
    void setPropertyName(String name);

    /**
     * Returns property name.
     *
     * @return name property name.
     */
    String getPropertyName();

    /**
     * Returns property values.
     *
     * @return properties for this <code>ResourceAttribute</code>.
     */
    Set<String> getPropertyValues();

    /**
     * Returns resource attributes applicable to the request.
     *
     * @param adminSubject Subject who is performing the evaluation.
     * @param realm Realm name.
     * @param subject Subject who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return applicable resource attributes
     * @throws com.sun.identity.entitlement.EntitlementException
     * if can not get condition decision
     */
    Map<String, Set<String>> evaluate(
        Subject adminSubject,
        String realm,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment)
        throws EntitlementException;

    /**
     * Sets OpenSSO policy response provider name of the object
     * @param pResponseProviderName response provider name as used in OpenSSO
     *        policy, this is relevant only when StaticAttributes was created
     *        from OpenSSO policy Subject
     */
    void setPResponseProviderName(String pResponseProviderName);

    /**
     * Returns OpenSSO policy response provider name of the object
     * @return response provider name as used in OpenSSO policy,
     * this is relevant only when StaticAttributes was created from
     * OpenSSO policy Subject
     */
    String getPResponseProviderName();

    /**
     * Returns the state of this object.
     *
     * @return state of this object.
     */
    String getState();

    /**
     * Sets the state of the object.
     *
     * @param s state of the object.
     */
    void setState(String s);
}
