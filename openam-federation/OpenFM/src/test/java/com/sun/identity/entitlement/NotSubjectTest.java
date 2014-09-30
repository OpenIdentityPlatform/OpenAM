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
 * $Id: NotSubjectTest.java,v 1.1 2009/08/19 05:41:00 veiming Exp $
 */

/**
 * Portions copyright 2014 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import java.util.HashSet;
import java.util.Set;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

public class NotSubjectTest {

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void testSingleSubject() {
        //given
        Set<EntitlementSubject> subjects = new HashSet<EntitlementSubject>();

        AnyUserSubject aus = new AnyUserSubject();
        AnyUserSubject aus2 = new AnyUserSubject();

        subjects.add(aus);
        subjects.add(aus2);

        NotSubject myNotSubject = new NotSubject();

        //when
        myNotSubject.setESubjects(subjects);

        //then -- expect error

    }

    @Test
    public void testSingleSubjectEnforced() {
        //given
        Set<EntitlementSubject> subjects = new HashSet<EntitlementSubject>();

        AnyUserSubject aus = new AnyUserSubject();

        subjects.add(aus);

        NotSubject myNotSubject = new NotSubject();

        //when
        myNotSubject.setESubjects(subjects);

        //then
        assertTrue(myNotSubject.getESubject().equals(aus));

    }

    @Test
    public void testSingleSubjectEnforcedRetrieval() {
        //given
        AnyUserSubject aus = new AnyUserSubject();
        NotSubject myNotSubject = new NotSubject();

        //when
        myNotSubject.setESubject(aus);

        //then
        assertTrue(myNotSubject.getESubjects().iterator().next().equals(aus));
    }
}
