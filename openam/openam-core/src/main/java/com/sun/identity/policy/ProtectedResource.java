/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ProtectedResource.java,v 1.2 2008/06/25 05:43:44 qcheng Exp $
 *
 */



package com.sun.identity.policy;
import java.util.Set;

/**
 * Class that encapsulates a resource and policies protecting the resource
 * for a given user
 *
 * @supported.all.api
 */
public class ProtectedResource {

    private String resourceName;
    private Set policies;

    /**
     * Constructs a <code>ProtectedResource</code> based on 
     * the given resource name and protecting policies
     * @param resourceName the resource that is protected
     * @param policies set of policies that protect
     *        the resource. The set contains Policy objects.
     */
    ProtectedResource(String resourceName, Set policies) {
        this.resourceName = resourceName;
        this.policies = policies;
    }

    /**
     * Sets the protected resource name
     * @param resourceName protected resource name
     */
    void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    /**
     * Gets the protected resource name 
     * @return protected resource name
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * Sets policies that protect the resource name
     * @param policies set of policies that protect the resource.
     *       The set contains Policy objects.
     */
    void setPolicies(Set policies) {
        this.policies = policies;
    }

    /**
     * Gets policies that protect the resource name
     * @return set of policies that protect the resource.
     *       The set contains Policy objects.
     */
    public Set getPolicies() {
        return policies;
    }
}
