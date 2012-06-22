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
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: XACMLClientSample.java,v 1.4 2008/06/25 05:48:48 qcheng Exp $
 *
 */

package samples.xacml;

import com.sun.identity.saml2.common.SAML2Exception;

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

import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

public class XACMLClientSample {


    public XACMLClientSample() {
    }

    public static void main(String[] args) throws Exception {
        XACMLClientSample clientSample = new XACMLClientSample();
        clientSample.runSample(args);
        System.exit(0);
    }

    public void runSample(String[] args) throws Exception {
        if (args.length == 0 || args.length > 1) {
            System.out.println("Missing argument:"
                    + "properties file name not specified");
        } else {
            System.out.println("Using properties file:" + args[0]);
            Properties sampleProperties = getProperties(args[0]);
            testProcessRequest(
                (String)sampleProperties.get("pdp.entityId"),
                (String)sampleProperties.get("pep.entityId"),
                (String)sampleProperties.get("subject.id"),
                (String)sampleProperties.get("subject.id.datatype"),
                (String)sampleProperties.get("subject.category"),
                (String)sampleProperties.get("resource.id"),
                (String)sampleProperties.get("resource.id.datatype"),
                (String)sampleProperties.get("resource.servicename"),
                (String)sampleProperties.get("resource.servicename.datatype"),
                (String)sampleProperties.get("action.id"),
                (String)sampleProperties.get("action.id.datatype")
            );
        }
    }

    private void testProcessRequest(
            String pdpEntityId, String pepEntityId,
            String subjectId, String subjectIdType,
            String subjectCategory,
            String resourceId, String resourceIdType,
            String serviceName, String serviceNameType,
            String actionId, String actionIdType) 
            throws XACMLException, SAML2Exception, 
            URISyntaxException, Exception {

        Request xacmlRequest = createSampleXacmlRequest(
            subjectId, subjectIdType,
            subjectCategory,
            resourceId, resourceIdType,
            serviceName, serviceNameType,
            actionId, actionIdType); 

        System.out.println("\ntestProcessRequest():xacmlRequest:\n" 
                + xacmlRequest.toXMLString(true, true));

        Response xacmlResponse = XACMLRequestProcessor.getInstance()
                .processRequest(xacmlRequest, pdpEntityId, pepEntityId);

        System.out.println("testProcessRequest():xacmlResponse:\n"
                + xacmlResponse.toXMLString(true, true));
    }

    private Request createSampleXacmlRequest(
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

        //Enviornment, our PDP does not use environment now
        Environment environment = ContextFactory.getInstance().createEnvironment();
        request.setEnvironment(environment);
        return request;
    }

    private Properties getProperties(String file) throws MissingResourceException {
        Properties properties = new Properties();
        ResourceBundle bundle = ResourceBundle.getBundle(file);
        Enumeration e = bundle.getKeys();
        System.out.println("sample properties:");
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = bundle.getString(key);
            properties.put(key, value);
            System.out.println(key + ":" + value);
        }
        return properties;
    }

}
