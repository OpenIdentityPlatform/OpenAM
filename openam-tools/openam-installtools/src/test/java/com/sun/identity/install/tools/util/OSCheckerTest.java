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

package com.sun.identity.install.tools.util;

import org.testng.Assert;
import org.testng.annotations.Test;

public class OSCheckerTest {

    @Test
    public void testIsUnixOrWindows() {

        Assert.assertTrue(OSChecker.isUnix() || OSChecker.isWindows());
    }

    @Test
    public void testLinuxVersionStringParsing() {

        String linuxVersionString = "2.6.32-279.14.1.el6.x86_64";
        OSChecker.parseVersion(linuxVersionString);
        Assert.assertEquals(OSChecker.getOsMajorVersion(), 2);
        Assert.assertEquals(OSChecker.getOsMinorVersion(), 6);
        // These are hard-coded in the POM
        Assert.assertTrue(OSChecker.atleast("Linux", 2, 6));
        Assert.assertTrue(OSChecker.matchArch("x86"));
    }

    @Test
    public void testOSXVersionStringParsing() {

        String osXVersionString = "10.9.5";
        OSChecker.parseVersion(osXVersionString);
        Assert.assertEquals(OSChecker.getOsMajorVersion(), 10);
        Assert.assertEquals(OSChecker.getOsMinorVersion(), 9);

    }

    @Test
    public void testWindowsVersionStringParsing() {

        String windowsVersionString = "5.1";
        OSChecker.parseVersion(windowsVersionString);
        Assert.assertEquals(OSChecker.getOsMajorVersion(), 5);
        Assert.assertEquals(OSChecker.getOsMinorVersion(), 1);
    }

    @Test
    public void testNoMinorVersionStringParsing() {

        String noMinorString = "100";
        OSChecker.parseVersion(noMinorString);
        Assert.assertEquals(OSChecker.getOsMajorVersion(), 100);
        Assert.assertEquals(OSChecker.getOsMinorVersion(), 0);
    }

    @Test
    public void testInvalidVersionStringParsing() {

        // The exception output will end up in the TestCase debug log under the
        // target/test-cases directory as set in the POM.
        String exceptionString = "abc.10";
        OSChecker.parseVersion(exceptionString);
        Assert.assertEquals(OSChecker.getOsMajorVersion(), 0);
        Assert.assertEquals(OSChecker.getOsMinorVersion(), 0);
    }
}
