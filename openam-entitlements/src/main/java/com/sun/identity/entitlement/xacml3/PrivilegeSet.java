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
 * Copyright 2014-2016 ForgeRock AS.
 */

package com.sun.identity.entitlement.xacml3;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.forgerock.openam.entitlement.ResourceType;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ReferralPrivilege;

/**
 * Simple collection class for passing collections of Privilege, ReferralPrivilege, Application and ResourceType
 * objects as method argument or return type.
 *
 * This class was introduced specifically to allow methods to return both Privilege and ReferralPrivilge results
 * in separate collections so later instanceof checks can be avoided.
 *
 * @since 12.0.0
 */
public class PrivilegeSet {

    private final List<Privilege> privileges;
    private final List<Application> applications;
    private final List<ResourceType> resourceTypes;
    private final List<ReferralPrivilege> referralPrivileges;

    /**
     * Constructs PrivilegeSet instance.
     */
    public PrivilegeSet() {
        this.privileges = new ArrayList<>();
        this.applications = new ArrayList<>();
        this.resourceTypes = new ArrayList<>();
        this.referralPrivileges = new ArrayList<>();
    }

    /**
     * Adds privilege to this set.
     *
     * @param privilege
     *         to be added.
     */
    public void addPrivilege(Privilege privilege) {
        privileges.add(privilege);
    }

    /**
     * Adds referralPrivilege to this set.
     *
     * @param referralPrivilege
     *         to be added.
     */
    public void addReferralPrivilege(ReferralPrivilege referralPrivilege) {
        referralPrivileges.add(referralPrivilege);
    }

    /**
     * Adds application to this set.
     *
     * @param application
     *         to be added.
     */
    public void addApplication(Application application) {
        applications.add(application);
    }

    /**
     * Adds resource type to this set.
     *
     * @param resourceType
     *         to be added.
     */
    public void addResourceType(ResourceType resourceType) {
        resourceTypes.add(resourceType);
    }

    /**
     * Obtain list of Privilege objects held by this class.
     *
     * @return Unmodifiable list of Privilege objects held by this class.
     */
    public List<Privilege> getPrivileges() {
        return Collections.unmodifiableList(privileges);
    }

    /**
     * Obtain list of ReferralPrivilege objects held by this class.
     *
     * @return Unmodifiable list of ReferralPrivilege objects held by this class.
     */
    public List<ReferralPrivilege> getReferralPrivileges() {
        return Collections.unmodifiableList(referralPrivileges);
    }

    /**
     * Obtain list of Application objects held by this class.
     *
     * @return Unmodifiable list of Application objects held by this class.
     */
    public List<Application> getApplication() {
        return Collections.unmodifiableList(applications);
    }

    /**
     * Obtain list of ResourceType objects held by this class.
     *
     * @return Unmodifiable list of ResourceType objects held by this class.
     */
    public List<ResourceType> getResourceTypes() {
        return Collections.unmodifiableList(resourceTypes);
    }

}
