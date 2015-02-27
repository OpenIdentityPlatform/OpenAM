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
 * Copyright 2014-2015 ForgeRock AS. All rights reserved.
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
        assertEquals("bobo?dodo", new UrlConstituentCatenatorImpl().catenateUrlConstituents("bobo/", "?dodo"));
    }

    @Test
    public void testRemoval() {
        assertEquals("bobo/dodo", new UrlConstituentCatenatorImpl().catenateUrlConstituents("bobo/", "/dodo"));
    }

    @Test
    public void testVarArgsCatenation() {
        assertEquals("json/rest/realm1/agents/agentId",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("json/", "/rest", "realm1", "/agents", "/agentId"));

        assertEquals("/json/rest/realm1/agents/agentId",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("/json/", "/rest/", "realm1", "/agents/", "/agentId"));

        assertEquals("json/rest/realm1/agents/agentId/",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("json/", "/rest", "/realm1", "/agents/", "/agentId/"));

        assertEquals("json/rest/realm1/agents/agentId/",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("json/", "/rest/", "/realm1", "/agents/", "/agentId/"));

        assertEquals("json/rest/realm1/agents/agentId/",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("json/", "/rest/", "/realm1", "/agents/", "/agentId/"));

        assertEquals("json/rest/agents/agentId/",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("json/", "/rest", "/", "/agents/", "/agentId/"));

        assertEquals("json/rest/agents/agentId/",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("json/", "/rest/", "/", "/agents/", "/agentId/"));

        assertEquals("json/rest/agents/agentId/",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("json/", "/rest/", "/", "agents/", "/agentId/"));

        assertEquals("json/rest/agents/agentId/",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("json/", "/rest", null, "/agents/", "/agentId/"));

        assertEquals("json/rest/agents/agentId/",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("json/", "", "/rest", null, "/agents/", "/agentId/"));

    }

    @Test
    public void testQueryParamCatenation() {
        assertEquals("bobo?dodo",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("bobo?", "dodo"));

        assertEquals("bobo?dodo",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("bobo", "?dodo"));

        assertEquals("json/rest/agents/agentId?foo=bar&fiz=bazz",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("json/", "", "/rest", null, "/agents/",
                        "/agentId/", "?", "foo=bar", "&", "fiz=bazz"));

        assertEquals("json/rest/agents/agentId?foo=bar&fiz=bazz",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("json/", "/rest", "/agents/",
                        "/agentId", "?", "foo=bar", "&", "fiz=bazz"));

        assertEquals("json/rest/agents/agentId?foo=bar&fiz=bazz",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("json/", "/rest", "/agents/",
                        "/agentId", "?", "foo=bar&", "fiz=bazz"));

        assertEquals("json/rest/agents/agentId?foo=bar&fiz=bazz",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("json/", "/rest", "/agents/",
                        "/agentId", "?", "foo=bar", "&fiz=bazz"));

        assertEquals("json/rest/agents/agentId?foo=bar&fiz=bazz",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("json/", "/rest", "/agents/",
                        "/agentId?", "foo=bar", "&fiz=bazz"));

        assertEquals("json/rest/agents/agentId?foo=bar&fiz=bazz",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("json/", "/rest", "/agents/",
                        "/agentId", "?foo=bar", "&fiz=bazz"));

        assertEquals("json/rest/agents/agentId?foo=bar&fiz=bazz",
                new UrlConstituentCatenatorImpl().catenateUrlConstituents("json/", "/rest", "/agents/",
                        "/agentId/", "?foo=bar", "&fiz=bazz"));
    }
}
