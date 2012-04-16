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
 * $Id: DebugLogTest.java,v 1.1 2009/08/19 05:41:02 veiming Exp $
 */

package com.sun.identity.entitlement.util;

import com.sun.identity.shared.debug.Debug;
import org.testng.annotations.Test;

/**
 *
 * @author dennis
 */
public class DebugLogTest {
    @Test
    public void testDebug() {
        Debug debug = Debug.getInstance("debugtest");
        try {
            String s = null;
            s.equals("test");
        } catch (NullPointerException e) {
            debug.error("DebugLogTest.testDebug", e);
        }
    }
}
