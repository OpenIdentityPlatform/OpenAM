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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 22014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

public class UrlConstituentCatenatorTest {
    @Test
    public void testInsertion() {
        assertEquals("bobo/dodo", new UrlConstituentCatenatorImpl().catenateUrlConstituents("bobo", "dodo"));
    }

    @Test
    public void testInsertionQuestionMark() {
        assertEquals("bobo/?dodo", new UrlConstituentCatenatorImpl().catenateUrlConstituents("bobo/", "?dodo"));
    }

    @Test
    public void testRemoval() {
        assertEquals("bobo/dodo", new UrlConstituentCatenatorImpl().catenateUrlConstituents("bobo/", "/dodo"));
    }

    @Test
    public void testStringBuilderAddition() {
        assertEquals("bobo/dodo",
                new UrlConstituentCatenatorImpl().catentateUrlConstituent(new StringBuilder("bobo"), "dodo").toString());
    }

    @Test
    public void testStringBuilderRemoval() {
        assertEquals("bobo/dodo",
                new UrlConstituentCatenatorImpl().catentateUrlConstituent(new StringBuilder("bobo/"), "/dodo").toString());
    }

    @Test
    public void testStringBuilderRemoval2() {
        assertEquals("/dodo",
                new UrlConstituentCatenatorImpl().catentateUrlConstituent(new StringBuilder("/"), "/dodo").toString());
    }

    @Test
    public void testStringBuilderRealmManipulation() {
        assertEquals("/dodo",
                new UrlConstituentCatenatorImpl().catentateUrlConstituent(new StringBuilder("/"), "dodo").toString());
    }

}
