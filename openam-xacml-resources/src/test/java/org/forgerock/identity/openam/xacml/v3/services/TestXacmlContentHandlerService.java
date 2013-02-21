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
package org.forgerock.identity.openam.xacml.v3.services;

import static org.testng.Assert.*;

import org.forgerock.identity.openam.xacml.v3.commons.ContentType;
import org.forgerock.identity.openam.xacml.v3.commons.XACML3Utils;
import org.forgerock.identity.openam.xacml.v3.model.XACML3Constants;
import org.junit.runner.RunWith;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

import org.powermock.modules.junit4.PowerMockRunner;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.util.Enumeration;


/**
 * XACML Content Handler Test Suite
 *
 * @author Jeff.Schenk@ForgeRock.com
 */
@RunWith(PowerMockRunner.class)
public class TestXacmlContentHandlerService {

    private static ServletTester servletTester;

    private final static String testAuthzDecisionQuery_ResourceName = "test_data/xacml3_authzDecisionQuery.xml";

    @BeforeClass
    public void before() throws Exception {

        servletTester = new ServletTester();
        servletTester.addServlet(XacmlContentHandlerService.class, "/openam/xacml");
        servletTester.addServlet(XacmlContentHandlerService.class, "/openam/xacml/home");
        servletTester.addServlet(XacmlContentHandlerService.class, "/openam/xacml/status");
        servletTester.addServlet(XacmlContentHandlerService.class, "/openam/xacml/ping");
        servletTester.addServlet(XacmlContentHandlerService.class, "/openam/xacml/pdp/authorization");
        servletTester.addServlet(XacmlContentHandlerService.class, "/openam/xacml/pdp");
        servletTester.addServlet(XacmlContentHandlerService.class, "/openam/xacml/pip");
        servletTester.addServlet(XacmlContentHandlerService.class, "/openam/xacml/pap");

        servletTester.start();
    }

    @AfterClass
    public void after() throws Exception {
        servletTester.stop();
    }

    // *********************************************************
    // GET Tests
    // *********************************************************

    @Test
    public void testUseCase_GET() {

        HttpTester request = new HttpTester();
        request.setMethod("GET");
        request.addHeader("Host", "example.org");
        request.setURI("/openam/xacml");
        request.setVersion("HTTP/1.1");
        request.addHeader("Content-Type", ContentType.XML.applicationType());

        try {
            // Check for a 200 on our Get.
            HttpTester response = new HttpTester();
            response.parse(servletTester.getResponses(request.generate()));
            assertEquals(response.getStatus(),200);
        } catch (IOException ioe) {

        } catch (Exception e) {

        }

    }

    @Test
    public void testUseCase_GET_HOME() {

        HttpTester request = new HttpTester();
        request.setMethod("GET");
        request.addHeader("Host", "example.org");
        request.setURI("/openam/xacml/home");
        request.setVersion("HTTP/1.1");
        request.addHeader("Content-Type", ContentType.XML.applicationType());

        try {
            // Check for a 200 on our Get.
            HttpTester response = new HttpTester();
            response.parse(servletTester.getResponses(request.generate()));
            assertEquals(response.getStatus(),200);
        } catch (IOException ioe) {

        } catch (Exception e) {

        }

    }

    @Test
    public void testUseCase_GET_STATUS() {

        HttpTester request = new HttpTester();
        request.setMethod("GET");
        request.addHeader("Host", "example.org");
        request.setURI("/openam/xacml/status");
        request.setVersion("HTTP/1.1");
        request.addHeader("Content-Type", ContentType.XML.applicationType());

        try {
            // Check for a 200 on our Get.
            HttpTester response = new HttpTester();
            response.parse(servletTester.getResponses(request.generate()));
            assertEquals(response.getStatus(),200);
        } catch (IOException ioe) {

        } catch (Exception e) {

        }

    }

    @Test
    public void testUseCase_GET_PDP() {

        HttpTester request = new HttpTester();
        request.setMethod("GET");
        request.addHeader("Host", "example.org");
        request.setURI("/openam/xacml/pdp");
        request.setVersion("HTTP/1.1");
        request.addHeader("Content-Type", ContentType.XACML_PLUS_XML.applicationType());

        try {
            // Check for a 200 on our Get.
            HttpTester response = new HttpTester();
            response.parse(servletTester.getResponses(request.generate()));
            assertEquals(response.getStatus(),200);
        } catch (IOException ioe) {

        } catch (Exception e) {

        }

    }

    @Test
    public void testUseCase_GET_PIP() {

        HttpTester request = new HttpTester();
        request.setMethod("GET");
        request.addHeader("Host", "example.org");
        request.setURI("/openam/xacml/pip");
        request.setVersion("HTTP/1.1");
        request.addHeader("Content-Type", ContentType.JSON.applicationType());

        try {
            // Check for a 401 on our Get.
            HttpTester response = new HttpTester();
            response.parse(servletTester.getResponses(request.generate()));
            assertEquals(response.getStatus(),401);
        } catch (IOException ioe) {

        } catch (Exception e) {

        }

    }

    @Test
    public void testUseCase_GET_PAP() {

        HttpTester request = new HttpTester();
        request.setMethod("GET");
        request.addHeader("Host", "example.org");
        request.setURI("/openam/xacml/pap");
        request.setVersion("HTTP/1.1");
        request.addHeader("Content-Type", ContentType.XACML_PLUS_JSON.applicationType());

        try {
            // Check for a 401 on our Get.
            HttpTester response = new HttpTester();
            response.parse(servletTester.getResponses(request.generate()));
            assertEquals(response.getStatus(),401);
        } catch (IOException ioe) {

        } catch (Exception e) {

        }

    }

    @Test
    public void testUseCase_GET_UnsupportedMediaType() {

        HttpTester request = new HttpTester();
        request.setMethod("GET");
        request.addHeader("Host", "example.org");
        request.setURI("/openam/xacml");
        request.setContent(""); // Set content Length to zero.
        request.setVersion("HTTP/1.1");

        try {
            // Check for a 415 Unsupported Media Type.
            HttpTester response = new HttpTester();
            response.parse(servletTester.getResponses(request.generate()));
            assertEquals(response.getStatus(),415);
        } catch (IOException ioe) {

        } catch (Exception e) {

        }

    }

    @Test
    public void testUseCase_GET_PING() {

        HttpTester request = new HttpTester();
        request.setMethod("GET");
        request.addHeader("Host", "example.org");
        request.setURI("/openam/xacml/ping");
        request.setVersion("HTTP/1.1");
        request.addHeader("Content-Type", ContentType.JSON.applicationType());

        try {
            // Check for a 200 on our Get.
            HttpTester response = new HttpTester();
            response.parse(servletTester.getResponses(request.generate()));
            assertEquals(response.getStatus(),200);
        } catch (IOException ioe) {

        } catch (Exception e) {

        }

    }

    // *********************************************************
    // POST Tests
    // *********************************************************


    @Test
    public void testUseCase_NoContentLengthSpecified() {

        HttpTester request = new HttpTester();
        request.setMethod("POST");
        request.addHeader("Host", "example.org");
        request.setURI("/openam/xacml");
        request.setVersion("HTTP/1.1");

        try {
            // Check for a 411 No Content Length Provided.
            HttpTester response = new HttpTester();
            response.parse(servletTester.getResponses(request.generate()));
            assertEquals(response.getStatus(),411);
        } catch (IOException ioe) {

        } catch (Exception e) {

        }

    }

    @Test
    public void testUseCase_UnsupportedMediaType() {

        HttpTester request = new HttpTester();
        request.setMethod("POST");
        request.addHeader("Host", "example.org");
        request.setURI("/openam/xacml");
        request.setContent(""); // Set content Length to zero.
        request.setVersion("HTTP/1.1");

        try {
            // Check for a 415 Unsupported Media Type.
            HttpTester response = new HttpTester();
            response.parse(servletTester.getResponses(request.generate()));
            assertEquals(response.getStatus(),415);
        } catch (IOException ioe) {

        } catch (Exception e) {

        }

    }

    @Test
    public void testUseCase_JSON_NotAuthorized() {

        HttpTester request = new HttpTester();
        request.setMethod("POST");
        request.addHeader("Host", "example.org");
        request.addHeader("Content-Type", ContentType.JSON.applicationType());
        request.setURI("/openam/xacml/pdp/authorization");
        request.setContent(""); // Set content Length to zero.
        request.setVersion("HTTP/1.1");

        try {
            // Check for a 401 Not Authorized with a WWW-Authenticate Digest.
            HttpTester response = new HttpTester();
            response.parse(servletTester.getResponses(request.generate()));
            assertEquals(response.getStatus(),401);
        } catch (IOException ioe) {

        } catch (Exception e) {

        }

    }

    @Test
    public void testUseCase_XML_NotAuthorized() {

        HttpTester request = new HttpTester();
        request.setMethod("POST");
        request.addHeader("Host", "example.org");
        request.addHeader("Content-Type", ContentType.XML.applicationType());
        request.setURI("/openam/xacml/pdp/authorization");
        request.setContent(""); // Set content Length to zero.
        request.setVersion("HTTP/1.1");

        try {
            // Check for a 401 Not Authorized with a WWW-Authenticate Digest.
            HttpTester response = new HttpTester();
            response.parse(servletTester.getResponses(request.generate()));
            assertEquals(response.getStatus(),401);
            assertNotNull(response.getHeader("Content-Type"));
            assertTrue(response.getHeader("Content-Type").startsWith(ContentType.XML.applicationType()));

            assertNotNull(response.getHeader("Content-Length"));
            assertTrue(response.getHeader("Content-Length").equals("0"));

            // Example of Data for WWW-Authenticate.
            // Digest realm="example.org",qop=auth,nonce="9fc422776b40c52a8a107742f9a08d5c",opaque="aba7d38a079f1a7d2e0ba2d4b84f3aa2"
            assertNotNull(response.getHeader(XACML3Constants.WWW_AUTHENTICATE_HEADER));
            assertTrue(response.getHeader(XACML3Constants.WWW_AUTHENTICATE_HEADER).startsWith("Digest "));


            // Dump all Headers
            Enumeration enumeration = response.getHeaderNames();
            while(enumeration.hasMoreElements())
            {
                String headerName = (String) enumeration.nextElement();
                System.out.println("Header Attribute:["+headerName+"], Value:["+response.getHeader(headerName)+"]");
            }

        } catch (IOException ioe) {

        } catch (Exception e) {

        }

    }

    @Test
    public void testUseCase_XML_Forbidden() {

        HttpTester request = new HttpTester();
        request.setMethod("POST");
        request.addHeader("Host", "example.org");
        request.addHeader("Content-Type", ContentType.XML.applicationType());
        request.setURI("/openam/xacml/pdp");
        request.setVersion("HTTP/1.1");

        String testData = XACML3Utils.getResourceContents(testAuthzDecisionQuery_ResourceName);
        assertNotNull(testData);
        request.setContent(testData);

        try {
            // Check for a 403 Forbidden.
            HttpTester response = new HttpTester();
            response.parse(servletTester.getResponses(request.generate()));
            assertEquals(response.getStatus(),403);
            assertNotNull(response.getHeader("Content-Type"));
            assertTrue(response.getHeader("Content-Type").startsWith(ContentType.XML.applicationType()));

            assertNotNull(response.getHeader("Content-Length"));
            assertTrue(response.getHeader("Content-Length").equals("0"));

        } catch (IOException ioe) {

        } catch (Exception e) {

        }

    }


}
