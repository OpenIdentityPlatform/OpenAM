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
 * Copyright 2013 ForgeRock, Inc.
 */
package com.sun.identity.entitlement.util;

import org.testng.annotations.Test;

import java.net.MalformedURLException;

import static org.testng.Assert.assertEquals;

/**
 * Unit test to exercise the RelaxedURL behaviour.
 */
public class RelaxedURLTest {

    @Test
    public void validateRelaxedURL() throws MalformedURLException {
        RelaxedURL url = new RelaxedURL("http://www.test.com:123/hello?world=456");
        assertEquals("http", url.getProtocol());
        assertEquals("www.test.com", url.getHostname());
        assertEquals("123", url.getPort());
        assertEquals("/hello", url.getPath());
        assertEquals("world=456", url.getQuery());
        assertEquals("http://www.test.com:123/hello?world=456", url.toString());

        url = new RelaxedURL("http://www.test.com/");
        assertEquals("http", url.getProtocol());
        assertEquals("www.test.com", url.getHostname());
        assertEquals("80", url.getPort());
        assertEquals("/", url.getPath());
        assertEquals("", url.getQuery());
        assertEquals("http://www.test.com:80/", url.toString());

        url = new RelaxedURL("https://www.test.com/");
        assertEquals("https", url.getProtocol());
        assertEquals("www.test.com", url.getHostname());
        assertEquals("443", url.getPort());
        assertEquals("/", url.getPath());
        assertEquals("", url.getQuery());
        assertEquals("https://www.test.com:443/", url.toString());

        url = new RelaxedURL("http://www.test.com/hello/world/");
        assertEquals("http", url.getProtocol());
        assertEquals("www.test.com", url.getHostname());
        assertEquals("80", url.getPort());
        assertEquals("/hello/world/", url.getPath());
        assertEquals("", url.getQuery());
        assertEquals("http://www.test.com:80/hello/world/", url.toString());

        url = new RelaxedURL("http://www.test.com/?a=b&c=d&e=f");
        assertEquals("http", url.getProtocol());
        assertEquals("www.test.com", url.getHostname());
        assertEquals("80", url.getPort());
        assertEquals("/", url.getPath());
        assertEquals("a=b&c=d&e=f", url.getQuery());
        assertEquals("http://www.test.com:80/?a=b&c=d&e=f", url.toString());
    }

}
