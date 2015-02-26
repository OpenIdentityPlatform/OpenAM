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
 * $Id: NotConditionTest.java,v 1.1 2009/08/19 05:41:00 veiming Exp $
 */
package com.sun.identity.entitlement;

import com.sun.identity.unittest.UnittestLog;

import java.util.Date;
import org.testng.annotations.Test;


public class NotConditionTest {

    @Test
    public void testConstruction() throws Exception {

        IPCondition ipc = new IPCondition("100.100.100.100", "200.200.200.200");
        ipc.setPConditionName("ip1");
        NotCondition ac = new NotCondition(ipc);
        NotCondition ac1 = new NotCondition();
        ac1.setState(ac.getState());

        if (!ac1.equals(ac)) {
            throw new Exception(
                "NotConditionTest.testConstruction():" +
                " NotCondition with setState does not equal NotCondition " +
                "with getState()");
        }

    }

    public static void main(String[] args) throws Exception {
        new AndConditionTest().testConstruction();
        UnittestLog.flush(new Date().toString());
    }
}
