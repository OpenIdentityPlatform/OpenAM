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
 *           *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: XACMLRequestProcessorTest.java,v 1.4 2008/06/25 05:50:18 qcheng Exp $
 *
 */

package com.sun.identity.xacml.client;

import com.iplanet.sso.SSOToken;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.shared.test.UnitTestBase;
import com.sun.identity.xacml.client.XACMLRequestProcessor;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
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
import org.testng.annotations.Parameters;

public class XACMLRequestProcessorTest extends UnitTestBase {

    public XACMLRequestProcessorTest() {
        super("OpenFed-xacml-XACMLRequestProcessorTest");
    }

    //@Test(groups={"xacml"}, expectedExceptions={XACMLException.class})
    //@Test(groups={"xacml"})
    public void testGetInstance() throws XACMLException {
        entering("testGetInstance()", null);
        log(Level.INFO,"testGetInstance()","\n");
        XACMLRequestProcessor.getInstance();
        log(Level.INFO,"testGetInstance()","\n");
        exiting("testGetInstance()");
    }

    @Test(groups={"xacml"})
    @Parameters({"pdp.entityId", "pep.entityId",
            "login.id", "login.password",
            "subject.id", "subject.id.datatype", 
            "subject.category", 
            "resource.id", "resource.id.datatype",
            "resource.servicename", "resource.servicename.datatype", 
            "action.id", "action.id.datatype"})
    public void testProcessRequest(
            String pdpEntityId, String pepEntityId,
            String loginId, String loginPassword,
            String subjectId, String subjectIdType,
            String subjectCategory,
            String resourceId, String resourceIdType,
            String serviceName, String serviceNameType,
            String actionId, String actionIdType) 
            throws XACMLException, SAML2Exception, 
            URISyntaxException, Exception {

        if ((subjectId == null) || (subjectId.length() == 0)) {
            SSOToken ssoToken 
                    = TokenUtils.getSessionToken("/", loginId, loginPassword);
            subjectId = ssoToken.getTokenID().toString();
            subjectIdType = XACMLConstants.OPENSSO_SESSION_ID;
        }

        Request xacmlRequest = createSampleXacmlRequest(
            subjectId, subjectIdType,
            subjectCategory,
            resourceId, resourceIdType,
            serviceName, serviceNameType,
            actionId, actionIdType); 

        log(Level.INFO,"testProcessRequest():xacmlRequest:\n",
                xacmlRequest.toXMLString(true, true));

        Response xacmlResponse = XACMLRequestProcessor.getInstance()
                .processRequest(xacmlRequest, pdpEntityId, pepEntityId);

        log(Level.INFO,"testProcessRequest():xacmlResponse:\n",
                xacmlResponse.toXMLString(true, true));
    }

    private Request createSampleXacmlRequest(
            String subjectId, String subjectIdType,
            String subjectCategory,
            String resourceId, String resourceIdType,
            String serviceName, String serviceNameType,
            String actionId, String actionIdType) 
            throws XACMLException, URISyntaxException {
        Request request = ContextFactory.getInstance().createRequest();

        //Subject1, access-subject
        Subject subject1 = ContextFactory.getInstance().createSubject();

        //supported category for id
        //urn:oasis:names:tc:xacml:1.0:subject-category:access-subject
        subject1.setSubjectCategory(new URI(subjectCategory));

        Attribute attribute = ContextFactory.getInstance().createAttribute();

        //key attribute id
        //urn:oasis:names:tc:xacml:1.0:subject:subject-id
        attribute.setAttributeId(
            new URI(XACMLConstants.SUBJECT_ID));

        //supported data type for id
        //urn:oasis:names:tc:xacml:1.0:data-type:x500Name
        //urn:sun:names:xacml:2.0:data-type:opensso-session-id
        //urn:sun:names:xacml:2.0:data-type:openfm-sp-nameid
        attribute.setDataType(new URI(subjectIdType));

        attribute.setIssuer("sampleIssuer1");

        //set values
        List<String> valueList = new ArrayList<String>();
        valueList.add(subjectId);
        attribute.setAttributeStringValues(valueList);
        List<Attribute> attributeList = new ArrayList<Attribute>();
        attributeList.add(attribute);
        subject1.setAttributes(attributeList);

        //Subject2, intermediary-subject
        Subject subject2 = ContextFactory.getInstance().createSubject();

        subject2.setSubjectCategory(
            new URI(XACMLConstants.INTERMEDIARY_SUBJECT));

        attribute = ContextFactory.getInstance().createAttribute();

        attribute.setAttributeId(
            new URI(XACMLConstants.SUBJECT_ID));

        //supported data type for id
        //urn:oasis:names:tc:xacml:1.0:data-type:x500Name
        //urn:sun:names:xacml:2.0:data-type:opensso-session-id
        //urn:sun:names:xacml:2.0:data-type:openfm-sp-nameid
        attribute.setDataType(new URI(subjectIdType)); 

        attribute.setIssuer("sampleIssuer2");

        //set values
        valueList = new ArrayList<String>();
        valueList.add(subjectId);
        attribute.setAttributeStringValues(valueList);
        attributeList = new ArrayList<Attribute>();
        attributeList.add(attribute);
        subject2.setAttributes(attributeList);

        //set subjects in request
        List<Subject> subjectList = new ArrayList<Subject>();
        subjectList.add(subject1);
        subjectList.add(subject2);
        request.setSubjects(subjectList);

        //Resource
        Resource resource = ContextFactory.getInstance().createResource();

        //resoruce-id attribute
        attribute = ContextFactory.getInstance().createAttribute();

        //key attribute id
        //urn:oasis:names:tc:xacml:1.0:resource:resource-id
        attribute.setAttributeId(
            new URI(XACMLConstants.RESOURCE_ID));

        //supported data type
        //http://www.w3.org/2001/XMLSchema#string
        attribute.setDataType(
            new URI(resourceIdType));

        attribute.setIssuer("sampleIssuer3");


        //set values
        valueList = new ArrayList<String>();
        valueList.add(resourceId);
        attribute.setAttributeStringValues(valueList);

        attributeList = new ArrayList<Attribute>();
        attributeList.add(attribute);

        //serviceName attribute
        attribute = ContextFactory.getInstance().createAttribute();

        //additional attribute id
        //urn:sun:names:xacml:2.0:resource:target-service
        attribute.setAttributeId(
            new URI(XACMLConstants.TARGET_SERVICE));

        //supported data type
        //http://www.w3.org/2001/XMLSchema#string
        attribute.setDataType(
            new URI(serviceNameType));

        attribute.setIssuer("sampleIssuer3");


        //set values
        valueList = new ArrayList<String>();
        valueList.add(serviceName);
        attribute.setAttributeStringValues(valueList);

        attributeList.add(attribute);

        resource.setAttributes(attributeList);

        List<Resource> resourceList = new ArrayList<Resource>();
        resourceList.add(resource);
        request.setResources(resourceList);

        //Action
        Action action = ContextFactory.getInstance().createAction();

        attribute = ContextFactory.getInstance().createAttribute();

        //key attribute id
        //urn:oasis:names:tc:xacml:1.0:action:action-id
        attribute.setAttributeId(
            new URI(XACMLConstants.ACTION_ID));

        //supported data type
        //http://www.w3.org/2001/XMLSchema#string
        attribute.setDataType(
            new URI(actionIdType));

        attribute.setIssuer("sampleIssuer5");

        valueList = new ArrayList<String>();
        valueList.add(actionId);
        attribute.setAttributeStringValues(valueList);
        attributeList = new ArrayList<Attribute>();
        attributeList.add(attribute);

        action.setAttributes(attributeList);

        request.setAction(action);

        //Enviornment
        Environment environment =
            ContextFactory.getInstance().createEnvironment();
        request.setEnvironment(environment);
        return request;
    }

}
