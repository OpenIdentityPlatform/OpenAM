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
 * Copyright 2014-2015 ForgeRock AS.
 */
package com.sun.identity.agents.tools.tomcat.v6;

import junit.framework.Assert;
import org.testng.annotations.Test;


public class TomcatVersionValidatorTest {

    public static final String VMWARE_TOMCAT6_SERVERINFO = "Server version: SpringSource tc Runtime/2.0.4.RELEASE\n"+
            "Server built: August 3 2010 0710\n"+
            "Server number: 6.0.28.29\n"+
            "OS Name: Linux\n"+
            "OS Version: 2.6.18-194.11.1.el5\n"+
            "Architecture: i386\n"+
            "JVM Version: 1.6.0_21-b06\n"+
            "JVM Vendor: Sun Microsystems Inc.";

    public static final String WINDOWS_TOMCAT6_SERVERINFO = "Server version: Apache Tomcat/6.0.32\n" +
            "Server built: February 2 2011 2003\n" +
            "Server number: 6.0.32.0\n" +
            "OS Name: Windows XP\n" +
            "OS Version: 5.2\n" +
            "Architecture: amd64\n" +
            "JVM Version: 1.6.0_19-b04\n" +
            "JVM Vendor: Sun Microsystems Inc.";

    public static final String LINUX_TOMCAT5_SERVERINFO = "﻿Server version: Apache Tomcat/5.5.29\n" +
            "Server built:   Mar 29 2010 07:46:34\n" +
            "Server number:  5.5.29.0\n" +
            "OS Name:        Linux\n" +
            "OS Version:     2.6.32-431.23.3.el6.x86_64\n" +
            "Architecture:   amd64\n" +
            "JVM Version:    1.7.0_65-mockbuild_2014_07_16_06_06-b00\n" +
            "JVM Vendor:     Oracle Corporation";

    public static final String LINUX_TOMCAT6_SERVERINFO = "﻿Server version: Apache Tomcat/6.0.24\n" +
            "Server built:   August 11 2014 1706\n" +
            "Server number:  6.0.24.0\n" +
            "OS Name:        Linux\n" +
            "OS Version:     2.6.32-431.23.3.el6.x86_64\n" +
            "Architecture:   amd64\n" +
            "JVM Version:    1.7.0_65-mockbuild_2014_07_16_06_06-b00\n" +
            "JVM Vendor:     Oracle Corporation";

    public static final String LINUX_TOMCAT7_SERVERINFO = "﻿Server version: Apache Tomcat/7.0.39\n" +
            "Server built:   Mar 22 2013 12:37:24\n" +
            "Server number:  7.0.39.0\n" +
            "OS Name:        Linux\n" +
            "OS Version:     2.6.32-431.23.3.el6.x86_64\n" +
            "Architecture:   amd64\n" +
            "JVM Version:    1.7.0_65-mockbuild_2014_07_16_06_06-b00\n" +
            "JVM Vendor:     Oracle Corporation";

    public static final String LINUX_TOMCAT8_SERVERINFO = "﻿Server version: Apache Tomcat/8.0.11\n" +
            "Server built:   Aug 15 2014 08:14:59\n" +
            "Server number:  8.0.11.0\n" +
            "OS Name:        Linux\n" +
            "OS Version:     2.6.32-431.23.3.el6.x86_64\n" +
            "Architecture:   amd64\n" +
            "JVM Version:    1.7.0_65-mockbuild_2014_07_16_06_06-b00\n" +
            "JVM Vendor:     Oracle Corporation";

    @Test
    public void testCheckTomcatVersionIsValid() {

        Assert.assertEquals(IConstants.TOMCAT_VER_60, TomcatVersionValidator.getTomcatVersion(VMWARE_TOMCAT6_SERVERINFO));
        Assert.assertEquals(IConstants.TOMCAT_VER_60, TomcatVersionValidator.getTomcatVersion(WINDOWS_TOMCAT6_SERVERINFO));
        Assert.assertEquals(IConstants.TOMCAT_VER_60, TomcatVersionValidator.getTomcatVersion(LINUX_TOMCAT6_SERVERINFO));
        Assert.assertEquals(IConstants.TOMCAT_VER_70, TomcatVersionValidator.getTomcatVersion(LINUX_TOMCAT7_SERVERINFO));
        Assert.assertEquals(IConstants.TOMCAT_VER_80, TomcatVersionValidator.getTomcatVersion(LINUX_TOMCAT8_SERVERINFO));

        Assert.assertNull(TomcatVersionValidator.getTomcatVersion(LINUX_TOMCAT5_SERVERINFO));
        Assert.assertNull(TomcatVersionValidator.getTomcatVersion(null));
        Assert.assertNull(TomcatVersionValidator.getTomcatVersion(""));
    }
}
