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
 * $Id: EntitlementServiceTest.java,v 1.1 2009/08/19 05:41:01 veiming Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.unittest.UnittestLog;
import java.security.AccessController;
import org.testng.annotations.Test;

public class EntitlementServiceTest {
    @Test
    public void hasEntitlementDITs() {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());

        EntitlementConfiguration ec = new EntitlementService(SubjectUtils.createSubject(adminToken), "/");
        boolean result = ec.hasEntitlementDITs();
        UnittestLog.logMessage(
            "EntitlementServiceTest.hasEntitlementDITs: returns " + result);
    }
    
}
