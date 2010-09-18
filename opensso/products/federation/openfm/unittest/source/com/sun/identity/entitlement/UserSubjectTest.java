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
 * $Id: UserSubjectTest.java,v 1.1 2009/08/19 05:41:01 veiming Exp $
 */

package com.sun.identity.entitlement;

import org.testng.annotations.Test;

public class UserSubjectTest {
    @Test
    public void testConstruction() throws Exception {
        UserSubject us = new UserSubject("user1");
        us.setPSubjectName("u1");

        UserSubject us1 = new UserSubject();
        us1.setState(us.getState());
        us1.setID("user2");

        UserSubject us3 = new UserSubject("user1");
        us3.setPSubjectName("u1");
        boolean result = us.equals(us3);
        if (!result) {
             throw new Exception(
                     "UserSubjectTest.testConstruction(): equality test for true failed");
        }

        result = us1.equals(us3);
        if (result) {
             throw new Exception(
                     "UserSubjectTest.testConstruction(): "
                     +  "equality test for false failed");
        }
    }
}
