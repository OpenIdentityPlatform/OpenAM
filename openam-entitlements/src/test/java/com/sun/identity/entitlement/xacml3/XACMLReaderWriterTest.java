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

import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.xacml3.core.PolicySet;
import org.testng.annotations.Test;

import java.util.Calendar;

import static com.sun.identity.entitlement.xacml3.Assertions.*;
import static com.sun.identity.entitlement.xacml3.FactoryMethods.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asList;
import static org.forgerock.openam.utils.Time.*;

public class XACMLReaderWriterTest {

    private static final String ROOT_REALM = "/";
    private long now = getCalendarInstance().getTimeInMillis();

    @Test
    public void canReadAndWritePrivilegesAsXACML() throws Exception {

        // Given
        Privilege privilege = createArbitraryPrivilege("Privilege", now);
        ReferralPrivilege referralPrivilege = createArbitraryReferralPrivilege("ReferralPrivilege", now);
        XACMLReaderWriter xacmlReaderWriter = new XACMLReaderWriter();

        PrivilegeSet inputPrivilegeSet = new PrivilegeSet(asList(referralPrivilege), asList(privilege));

        // When
        PolicySet policySet = xacmlReaderWriter.toXACML(ROOT_REALM, inputPrivilegeSet);
        PrivilegeSet outputPrivilegeSet = xacmlReaderWriter.fromXACML(policySet);

        // Then
        assertThat(outputPrivilegeSet.getPrivileges()).hasSize(1);
        assertPrivilegesEquivalent(outputPrivilegeSet.getPrivileges().get(0), privilege);
        assertThat(outputPrivilegeSet.getReferralPrivileges()).hasSize(1);
        assertReferralPrivilegesEquivalent(outputPrivilegeSet.getReferralPrivileges().get(0), referralPrivilege);
    }

}
