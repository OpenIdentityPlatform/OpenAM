/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 *                 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: XACMLRequestProcessorTest.java,v 1.4 2008/06/25 05:48:26 qcheng Exp $
 *
 */

package com.sun.identity.xacml.client;

import com.sun.identity.shared.test.UnitTestBase;
import com.sun.identity.xacml.client.XACMLRequestProcessor;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Action;
import com.sun.identity.xacml.context.Attribute;
import com.sun.identity.xacml.context.Environment;
import com.sun.identity.xacml.context.Request;
import com.sun.identity.xacml.context.Resource;
import com.sun.identity.xacml.context.Response;
import com.sun.identity.xacml.context.Subject;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.testng.annotations.Test;

public class XACMLRequestProcessorTest extends UnitTestBase {

    public XACMLRequestProcessorTest() {
        super("FedLibrary-XACML-XACMLReuestProcessorTest");
    }

    //@Test(groups={"xacml"}, expectedExceptions={XACMLException.class})
    @Test(groups={"xacml"})
    public void testGetInstance() throws XACMLException {
        entering("testGetInstance()", null);
        log(Level.INFO,"testGetInstance()","\n");
        XACMLRequestProcessor.getInstance();
        log(Level.INFO,"testGetInstance()","\n");
        exiting("testGetInstance()");
    }

    @Test(groups={"xacml"})
    public void processRequest() 
            throws XACMLException, SAML2Exception, URISyntaxException {
        Request xacmlRequest = createSampleXacmlRequest();
        log(Level.INFO,"processRequest():xacmlRequest:\n",
                xacmlRequest.toXMLString(true, true));
        Response xacmlResponse = XACMLRequestProcessor.getInstance()
                .processRequest(xacmlRequest, null, null);
        log(Level.INFO,"processRequest():xacmlResponse:\n",
                xacmlResponse.toXMLString(true, true));
    }

    //temporay for testing
    private Request createSampleXacmlRequest()
            throws XACMLException, URISyntaxException {
        Request request = ContextFactory.getInstance().createRequest();

        Subject subject1 = ContextFactory.getInstance().createSubject();

        //supported category for id
        //urn:oasis:names:tc:xacml:1.0:subject-category:access-subject
        subject1.setSubjectCategory(
            new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"));

        Attribute attribute = ContextFactory.getInstance().createAttribute();
        attribute.setIssuer("sampleIssuer1");

        //key attribute id
        //urn:oasis:names:tc:xacml:1.0:subject:subject-id
        attribute.setAttributeId(
            new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id"));

        //supported data type for id
        //urn:oasis:names:tc:xacml:1.0:data-type:x500Name
        //urn:sun:names:xacml:2.0:data-type:opensso-session-id
        //urn:sun:names:xacml:2.0:data-type:openfm-sp-nameid
        attribute.setDataType(
            new URI("urn:opensso:names:xacml:2.0:data-type:opensso-session-id"));

        List<String> valueList = new ArrayList<String>();
        valueList.add("sessionId1");
        valueList.add("sessionId2");
        attribute.setAttributeStringValues(valueList);
        List<Attribute> attributeList = new ArrayList<Attribute>();
        attributeList.add(attribute);
        subject1.setAttributes(attributeList);

        Subject subject2 = ContextFactory.getInstance().createSubject();
        subject2.setSubjectCategory(
            new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"));
        attribute = ContextFactory.getInstance().createAttribute();
        attribute.setIssuer("sampleIssuer2");
        attribute.setAttributeId(
            new URI("urn:oasis:names:tc:xacml:1.0:subject:subject-id"));
        attribute.setDataType(
            new URI("urn:sun:names:xacml:2.0:data-type:openfm-sp-nameid"));
        valueList = new ArrayList<String>();
        valueList.add("openfm-sp-nameid1");
        attribute.setAttributeStringValues(valueList);
        attributeList = new ArrayList<Attribute>();
        attributeList.add(attribute);
        subject2.setAttributes(attributeList);

        List<Subject> subjects = new ArrayList<Subject>();
        subjects.add(subject1);
        subjects.add(subject2);
        request.setSubjects(subjects);

        Resource resource = ContextFactory.getInstance().createResource();
        attribute = ContextFactory.getInstance().createAttribute();
        attribute.setIssuer("sampleIssuer3");

        //key attribute id
        //urn:oasis:names:tc:xacml:1.0:resource:resource-id
        //additional attribute id
        //urn:opensso:names:xacml:2.0:resource:target-service
        attribute.setAttributeId(
            new URI("urn:oasis:names:tc:xacml:1.0:resource:resource-id"));

        //supported data type
        //http://www.w3.org/2001/XMLSchema#string
        attribute.setDataType(
            new URI("http://www.w3.org/2001/XMLSchema#string"));
        valueList = new ArrayList<String>();
        valueList.add("http://insat.red.iplanet.com/banner.html");
        attribute.setAttributeStringValues(valueList);
        attributeList = new ArrayList<Attribute>();
        attributeList.add(attribute);

        attribute = ContextFactory.getInstance().createAttribute();
        attribute.setIssuer("sampleIssuer4");
        attribute.setAttributeId(
            new URI("urn:oasis:names:tc:xacml:1.0:resource:resource-id"));
        attribute.setDataType(
            new URI("http://www.w3.org/2001/XMLSchema#string"));
        valueList = new ArrayList<String>();
        valueList.add("http://insat.red.iplanet.com/banner.html");
        attribute.setAttributeStringValues(valueList);
        attributeList.add(attribute);

        resource.setAttributes(attributeList);
        List<Resource> resourceList = new ArrayList<Resource>();
        resourceList.add(resource);
        request.setResources(resourceList);

        Action action = ContextFactory.getInstance().createAction();
        attribute = ContextFactory.getInstance().createAttribute();
        attribute.setIssuer("sampleIssuer5");

        //key attribute id
        //urn:oasis:names:tc:xacml:1.0:action:action-id
        attribute.setAttributeId(
            new URI("urn:oasis:names:tc:xacml:1.0:action:action-id"));

        //supported data type
        //http://www.w3.org/2001/XMLSchema#string
        attribute.setDataType(
            new URI("http://www.w3.org/2001/XMLSchema#string"));
        valueList = new ArrayList<String>();
        valueList.add("GET");
        attribute.setAttributeStringValues(valueList);
        attributeList = new ArrayList<Attribute>();
        attributeList.add(attribute);

        action.setAttributes(attributeList);

        request.setAction(action);

        Environment environment =
            ContextFactory.getInstance().createEnvironment();
        request.setEnvironment(environment);
        return request;
    }
}
