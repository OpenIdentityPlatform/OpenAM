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
 * $Id: GroupSubjectTest.java,v 1.1 2009/08/19 05:41:00 veiming Exp $
 */
package com.sun.identity.entitlement;

import org.testng.annotations.Test;

public class GroupSubjectTest {

    @Test
    public void testConstruction() throws Exception {
        GroupSubject group1 = new GroupSubject("grouper1");
        group1.setPSubjectName("g1");

        GroupSubject group11 = new GroupSubject();
        group11.setState(group1.getState());
        boolean result = group11.equals(group1);
        if (!result) {
            throw new Exception("GroupSubject.testConstruction():" +
                " equals test for true failed");
        }

        group1.setID("group1");
        result = group11.equals(group1);
        if (result) {
            throw new Exception("GroupSubject.testConstruction():" +
                " equals test for false failed");
        }
    }
}
