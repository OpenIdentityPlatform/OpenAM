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
 * Copyright 2014 ForgeRock AS.
 */

package com.sun.identity.entitlement.xacml3;

import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ReferralPrivilege;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple collection class for passing collections of Privilege and ReferralPrivilege objects as method argument
 * or return type.
 *
 * This class was introduced specifically to allow methods to return both Privilege and ReferralPrivilge results
 * in separate collections so later instanceof checks can be avoided.
 *
 * @since 12.0.0
 */
public class PrivilegeSet {

    private final List<Privilege> privileges;
    private final List<ReferralPrivilege> referralPrivileges;

    public PrivilegeSet() {
        this.privileges = new ArrayList<Privilege>();
        this.referralPrivileges = new ArrayList<ReferralPrivilege>();
    }

    public PrivilegeSet(List<ReferralPrivilege> referralPrivileges, List<Privilege> privileges) {
        this();
        this.privileges.addAll(privileges);
        this.referralPrivileges.addAll(referralPrivileges);
    }

    public void addPrivilege(Privilege privilege) {
        privileges.add(privilege);
    }

    /**
     * Obtain list of Privilege objects held by this class.
     *
     * @return Unmodifiable list of Privilege objects held by this class.
     */
    public List<Privilege> getPrivileges() {
        return Collections.unmodifiableList(privileges);
    }

    public void addReferralPrivilege(ReferralPrivilege referralPrivilege) {
        referralPrivileges.add(referralPrivilege);
    }

    /**
     * Obtain list of ReferralPrivilege objects held by this class.
     *
     * @return Unmodifiable list of ReferralPrivilege objects held by this class.
     */
    public List<ReferralPrivilege> getReferralPrivileges() {
        return Collections.unmodifiableList(referralPrivileges);
    }
}
