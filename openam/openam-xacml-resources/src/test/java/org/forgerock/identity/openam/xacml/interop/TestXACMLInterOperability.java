/**
 *
 ~ DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 ~
 ~ Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 ~
 ~ The contents of this file are subject to the terms
 ~ of the Common Development and Distribution License
 ~ (the License). You may not use this file except in
 ~ compliance with the License.
 ~
 ~ You can obtain a copy of the License at
 ~ http://forgerock.org/license/CDDLv1.0.html
 ~ See the License for the specific language governing
 ~ permission and limitations under the License.
 ~
 ~ When distributing Covered Code, include this CDDL
 ~ Header Notice in each file and include the License file
 ~ at http://forgerock.org/license/CDDLv1.0.html
 ~ If applicable, add the following below the CDDL Header,
 ~ with the fields enclosed by brackets [] replaced by
 ~ your own identifying information:
 ~ "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.identity.openam.xacml.interop;

import org.forgerock.identity.openam.xacml.commons.ContentType;
import org.forgerock.identity.openam.xacml.services.XacmlContentHandlerService;
import org.junit.runner.RunWith;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.powermock.modules.junit4.PowerMockRunner;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Scanner;

import static org.testng.Assert.*;


/**
 * XACML Content Handler Test Suite for InterOp 2013.
 *
 * @author Jeff.Schenk@ForgeRock.com
 */
@RunWith(PowerMockRunner.class)
public class TestXACMLInterOperability {

    private static ServletTester servletTester;

    @BeforeClass
    public void before() throws Exception {

        servletTester = new ServletTester();
        servletTester.addServlet(XacmlContentHandlerService.class, "/xacml");
        servletTester.addServlet(XacmlContentHandlerService.class, "/xacml/authorization");
        servletTester.addServlet(XacmlContentHandlerService.class, "/xacml/pdp");
        servletTester.addServlet(XacmlContentHandlerService.class, "/xacml/relation/pdp");
        servletTester.start();
    }

    @AfterClass
    public void after() throws Exception {
        servletTester.stop();
    }




}
