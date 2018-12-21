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

package com.sun.identity.common;

import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static org.testng.Assert.*;

public class CaseInsensitivePropertiesTest {

    @Test
    public void test() throws Exception {
        CaseInsensitiveProperties p = new CaseInsensitiveProperties();
        p.put("One", "une");
        p.put("tWo", "deux");
        assertEquals(p.get("ONE"), "une");
        assertEquals(p.get("TWO"), "deux");
        p.setProperty("oNE", "uno");
        p.setProperty("tWo", "dos");
        assertEquals(p.get("ONE"), "uno");
        assertEquals(p.get("TWO"), "dos");

//        ByteArrayOutputStream pOut = new ByteArrayOutputStream();
//        p.store(pOut,null);
        //System.out.println(pOut.toString());
//        ByteArrayInputStream pIn = new ByteArrayInputStream(pOut.toByteArray());
//        CaseInsensitiveProperties pp = new CaseInsensitiveProperties();
//        pp.load(pIn);
//        assertEquals(pp.get("ONE"), "uno");
//        assertEquals(pp.get("TWO"), "dos");
    }

}