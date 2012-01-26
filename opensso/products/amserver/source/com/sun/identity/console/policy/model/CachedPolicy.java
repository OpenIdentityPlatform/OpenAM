/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CachedPolicy.java,v 1.2 2008/06/25 05:43:07 qcheng Exp $
 *
 */

package com.sun.identity.console.policy.model;

import com.sun.identity.policy.Policy;

/* - NEED NOT LOG - */

/**
 * This object is stored in <code>PolicyCacher</code> class
 */
public class CachedPolicy {
    private boolean isModified = false;
    private Policy policy = null;
    // policy name for retrieving original policy
    private String trackPolicyName = null;

    /**
     * Constructs a cached policy object
     *
     * @param policy object
     */
    public CachedPolicy(Policy policy) {
        this.policy = policy;
        trackPolicyName = policy.getName();
    }

    /**
     * Returns tracking policy name
     *
     * @return tracking policy name
     */
    public String getTrackPolicyName() {
        return trackPolicyName;
    }

    /**
     * Set tracking policy name.
     *
     * @param name name of policy.
     */
    public void setTrackPolicyName(String name) {
        trackPolicyName = name;
    }

    /**
     * Returns true if policy is modified.
     *
     * @return true if policy is modified.
     */
    public boolean isPolicyModified() {
        return isModified;
    }

    /**
     * Set modified flag.
     *
     * @param flag <code>true</code> for modified policy.
     */
    public void setPolicyModified(boolean flag) {
        isModified = flag;
    }

    /**
     * Returns policy object.
     *
     * @return policy object.
     */
    public Policy getPolicy() {
        return policy;
    }
}
