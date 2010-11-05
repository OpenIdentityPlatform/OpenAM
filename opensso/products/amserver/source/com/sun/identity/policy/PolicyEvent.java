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
 * $Id: PolicyEvent.java,v 1.2 2008/06/25 05:43:44 qcheng Exp $
 *
 */


package com.sun.identity.policy;

import java.util.Set;

/**
 * Class to represent policy change events, used by policy framework to
 *  notify interested listeners
 *  @see com.sun.identity.policy.interfaces.PolicyListener
 *
 * @supported.all.api
 */
public class PolicyEvent {

    /**
     * constant to indicate change type policy added
     */
    public static final int POLICY_ADDED =
        com.sun.identity.sm.ServiceListener.ADDED;

    /**
     * constant to indicate change type policy modified
     */
    public static final int POLICY_MODIFIED =
        com.sun.identity.sm.ServiceListener.MODIFIED;

    /**
     * constant to indicate change type policy removed
     */
    public static final int POLICY_REMOVED =
        com.sun.identity.sm.ServiceListener.REMOVED;
    
    private Set resourceNames;
    private int changeType;

    /**
     * No argument constructor 
     */
    PolicyEvent() {
    }

    /**
     * Sets the resource names affected by the policy change 
     * @param resourceNames names of the resources affected by 
     *       the policy change
     */
    void setResourceNames(Set resourceNames) {
        this.resourceNames = resourceNames;
    }

    /**
     * Gets the resource names affected by the policy change.
     * This indicates that policy decisions for the affected resource names
     * would likely  be different from those computed before the change. 
     * @return names of the resources affected by the policy change.
     */
    public Set getResourceNames() {
        return resourceNames;
    }

    /**
     * Sets the change type
     * @param changeType <code>int</code> representing change type
     */
    void setChangeType(int changeType) {
        this.changeType = changeType;
    }

    /**
     * Gets the change type
     * The change type gives the type of policy change that triggered this
     * event indicating whether a policy was added, modified or removed. 
     * This change type does not indicate whether resource(s) were added,
     * modified or removed.
     * @return change type
     */
    public int getChangeType() {
        return changeType;
    }
}

