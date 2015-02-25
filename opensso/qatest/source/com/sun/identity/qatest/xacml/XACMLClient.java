/* The contents of this file are subject to the terms
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
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: XACMLClient.java,v 1.3 2008/06/26 20:28:34 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.xacml;

import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.client.XACMLRequestProcessor;
import com.sun.identity.xacml.context.Action;
import com.sun.identity.xacml.context.Attribute;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Environment;
import com.sun.identity.xacml.context.Request;
import com.sun.identity.xacml.context.Resource;
import com.sun.identity.xacml.context.Response;
import com.sun.identity.xacml.context.Subject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

/**
 * XACML Client makes the SOAP request to the PEP and
 * gets the Decision/Response
 */

public class XACMLClient extends TestCommon {
    
    String testResourceName;
    
    /**
     * XACMLClient Constructor
     **/
    public XACMLClient() {
        super("XACMLClient");
    }
    
    public String testXACML(String testResource) throws Exception {
        testResourceName = testResource;
        Properties testProperties = getProperties(testResourceName);
        String response = testProcessRequest(
                (String)testProperties.get("pdp.entityId"),
                (String)testProperties.get("pep.entityId"),
                (String)testProperties.get("subject.id"),
                (String)testProperties.get("subject.id.datatype"),
                (String)testProperties.get("subject.category"),
                (String)testProperties.get("resource.id"),
                (String)testProperties.get("resource.id.datatype"),
                (String)testProperties.get("resource.servicename"),
                (String)testProperties.get("resource.servicename.datatype"),
                (String)testProperties.get("action.id"),
                (String)testProperties.get("action.id.datatype")
                );
        log(Level.FINEST, "testXACML", "Response received from XACML Request" +
                " is " + response);
        
        return response;
    }
    
    /**
     * Creates a XACML request and gets the response
     * @return response
     */
    private String testProcessRequest(
            String pdpEntityId, String pepEntityId,
            String subjectId, String subjectIdType,
            String subjectCategory,
            String resourceId, String resourceIdType,
            String serviceName, String serviceNameType,
            String actionId, String actionIdType)
            throws XACMLException, SAML2Exception,
            URISyntaxException, Exception {
        
        Request xacmlRequest = createTestXacmlRequest(
                subjectId, subjectIdType,
                subjectCategory,
                resourceId, resourceIdType,
                serviceName, serviceNameType,
                actionId, actionIdType);
        log(Level.FINEST, "testProcessRequest", " XACML Request is " +
                xacmlRequest.toXMLString());
        Response xacmlResponse = XACMLRequestProcessor.getInstance()
        .processRequest(xacmlRequest, pdpEntityId, pepEntityId);
        String strxacmlResponse = xacmlResponse.toXMLString(true, true);
        
        return strxacmlResponse;
        
    }
    
    /**
     * XACML test request is constructed with set of attribute values
     * @return xacml request
     */
    private Request createTestXacmlRequest(
            String subjectId, String subjectIdType,
            String subjectCategory,
            String resourceId, String resourceIdType,
            String serviceName, String serviceNameType,
            String actionId, String actionIdType)
            throws XACMLException, URISyntaxException {
        
        Request request = ContextFactory.getInstance().createRequest();
        
        //Subject
        Subject subject = ContextFactory.getInstance().createSubject();
        subject.setSubjectCategory(new URI(subjectCategory));
        
        //set subject id
        Attribute attribute = ContextFactory.getInstance().createAttribute();
        attribute.setAttributeId(new URI(XACMLConstants.SUBJECT_ID));
        attribute.setDataType(new URI(subjectIdType));
        List valueList = new ArrayList();
        valueList.add(subjectId);
        attribute.setAttributeStringValues(valueList);
        List attributeList = new ArrayList();
        attributeList.add(attribute);
        subject.setAttributes(attributeList);
        
        
        //set Subject in Request
        List subjectList = new ArrayList();
        subjectList.add(subject);
        request.setSubjects(subjectList);
        
        //Resource
        Resource resource = ContextFactory.getInstance().createResource();
        
        //set resource id
        attribute = ContextFactory.getInstance().createAttribute();
        attribute.setAttributeId(new URI(XACMLConstants.RESOURCE_ID));
        attribute.setDataType( new URI(resourceIdType));
        valueList = new ArrayList();
        valueList.add(resourceId);
        attribute.setAttributeStringValues(valueList);
        attributeList = new ArrayList();
        attributeList.add(attribute);
        
        //set serviceName
        attribute = ContextFactory.getInstance().createAttribute();
        attribute.setAttributeId(new URI(XACMLConstants.TARGET_SERVICE));
        attribute.setDataType(new URI(serviceNameType));
        valueList = new ArrayList();
        valueList.add(serviceName);
        attribute.setAttributeStringValues(valueList);
        attributeList.add(attribute);
        resource.setAttributes(attributeList);
        
        //set Resource in Request
        List resourceList = new ArrayList();
        resourceList.add(resource);
        request.setResources(resourceList);
        
        //Action
        Action action = ContextFactory.getInstance().createAction();
        attribute = ContextFactory.getInstance().createAttribute();
        attribute.setAttributeId(new URI(XACMLConstants.ACTION_ID));
        attribute.setDataType(new URI(actionIdType));
        
        //set actionId
        valueList = new ArrayList();
        valueList.add(actionId);
        attribute.setAttributeStringValues(valueList);
        attributeList = new ArrayList();
        attributeList.add(attribute);
        action.setAttributes(attributeList);
        
        //set Action in Request
        request.setAction(action);
        
        //Enviornment
        Environment environment = 
                ContextFactory.getInstance().createEnvironment();
        request.setEnvironment(environment);
        log(Level.FINEST, "createTestXacmlRequest", " XACML Request is " +
                request.toXMLString());
        
        return request;
    }
}
