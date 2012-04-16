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
 * $Id: LogTest.java,v 1.1 2009/08/19 05:41:01 veiming Exp $
 */

package com.sun.identity.entitlement.log;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.internal.server.AuthSPrincipal;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import org.testng.annotations.Test;

/**
 *
 * @author dennis
 */
public class LogTest {

    @Test
    public void testLog() {
        Logger logger = LoggerFactory.getLogger("logtest");

        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        Subject sbj = createSubject(adminToken);

        ELogRecord lr = new ELogRecord(Level.SEVERE, "test log message",
            sbj, sbj);
        logger.log(lr);
    }

    private Subject createSubject(SSOToken token) {
        Principal userP = new AuthSPrincipal(token.getTokenID().toString());
        Set userPrincipals = new HashSet(2);
        userPrincipals.add(userP);
        Set privateCred = new HashSet(2);
        privateCred.add(token);
        return new Subject(true, userPrincipals, Collections.EMPTY_SET,
            privateCred);
    }

}
