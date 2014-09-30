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
 * $Id: AndSubjectTest.java,v 1.1 2009/08/19 05:41:00 veiming Exp $
 */

/**
 * Portions copyright 2014 ForgeRock AS.
 */

package com.sun.identity.entitlement;



import java.util.HashSet;
import java.util.Set;
import org.testng.annotations.Test;


public class AndSubjectTest {

    @Test
    public void testConstruction() throws Exception {

        UserSubject us1 = new UserSubject("user11");
        us1.setPSubjectName("u1");
        UserSubject us2 = new UserSubject("user12");
        us2.setPSubjectName("u1");
        GroupSubject gs1 = new GroupSubject("group11");
        gs1.setPSubjectName("g1");
        GroupSubject gs2 = new GroupSubject("group12");
        gs1.setPSubjectName("g1");
        GroupSubject gs3 = new GroupSubject("group31");
        gs1.setPSubjectName("g3");
        Set<EntitlementSubject> subjects = new HashSet<EntitlementSubject>();
        subjects.add(us1);
        subjects.add(us2);
        subjects.add(gs1);
        subjects.add(gs2);
        AndSubject andSubject = new AndSubject(subjects);
        AndSubject andSubuject1 = new AndSubject();
        andSubuject1.setState(andSubject.getState());
        boolean result = andSubject.equals(andSubuject1);
        if (!result) {
            throw new Exception("AndSubjectTest.testConstruction():"
                    + "AndSubject with setState="
                    +  "does not equal AndSubject with getState()");
             
        }
    }
}
