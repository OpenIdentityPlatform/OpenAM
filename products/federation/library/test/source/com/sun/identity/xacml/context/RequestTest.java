/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RequestTest.java,v 1.3 2008/06/25 05:48:27 qcheng Exp $
 *
 */
package com.sun.identity.xacml.context;

import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import com.sun.identity.shared.test.UnitTestBase;
import com.sun.identity.xacml.context.Attribute;
import com.sun.identity.xacml.context.Subject;
import com.sun.identity.xacml.context.Resource;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import javax.xml.parsers.ParserConfigurationException;
import com.sun.identity.shared.xml.XMLUtils;
import org.xml.sax.SAXException;
import java.net.URI;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit Test Cases to test
 * <code>com.sun.identity.xacml.context.Request</code> class.
 */

public class RequestTest extends UnitTestBase {
    
    public RequestTest() {
        super("FedLibrary-XACML-RequestTest");
    }
    
    @Test(groups={"xacml"})
    public void getResult() throws XACMLException, Exception {

        Request request = ContextFactory.getInstance().createRequest();

        Subject subject1 =
                ContextFactory.getInstance().createSubject();
        subject1.setSubjectCategory(new URI(XACMLConstants.ACCESS_SUBJECT));
        List<Attribute> s1attrs = new ArrayList<Attribute>();
        Attribute s1attr1 = ContextFactory.getInstance().createAttribute();
        s1attr1.setAttributeId(new URI("testid1"));
        s1attr1.setDataType(new URI("testDataType1"));
        List s1values1 = new ArrayList();
        s1values1.add("value-1");
        s1attr1.setAttributeStringValues(s1values1);
        Attribute s1attr2 = ContextFactory.getInstance().createAttribute();
        s1attr2.setAttributeId(new URI("testid2"));
        s1attr2.setDataType(new URI("testDataType2"));
        s1attr2.setIssuer("Bhavna");
        List s1values2 = new ArrayList();
        s1values2.add("value-2");
        s1attr2.setAttributeStringValues(s1values2);
        s1attrs.add(s1attr1);
        s1attrs.add(s1attr2);
        subject1.setAttributes(s1attrs);

        Subject subject2 =
                ContextFactory.getInstance().createSubject();
        subject1.setSubjectCategory(new URI(XACMLConstants.ACCESS_SUBJECT));
        List<Attribute> s2attrs = new ArrayList<Attribute>();
        Attribute s2attr1 = ContextFactory.getInstance().createAttribute();
        s2attr1.setAttributeId(new URI("testid1"));
        s2attr1.setDataType(new URI("testDataType1"));
        List s2values1 = new ArrayList();
        s2values1.add("value-1");
        s2attr1.setAttributeStringValues(s2values1);
        Attribute s2attr2 = ContextFactory.getInstance().createAttribute();
        s2attr2.setAttributeId(new URI("testid2"));
        s2attr2.setDataType(new URI("testDataType2"));
        s2attr2.setIssuer("Dilli");
        List s2values2 = new ArrayList();
        s2values2.add("value-2");
        s2attr2.setAttributeStringValues(s2values2);
        s2attrs.add(s2attr1);
        s2attrs.add(s2attr2);
        subject2.setAttributes(s2attrs);

        List subjects = new ArrayList();
        subjects.add(subject1);
        subjects.add(subject2);
        request.setSubjects(subjects);

        Resource resource1 =
                ContextFactory.getInstance().createResource();
        List<Attribute> r1attrs = new ArrayList<Attribute>();
        Attribute r1attr1 = ContextFactory.getInstance().createAttribute();
        r1attr1.setAttributeId(new URI("testid1"));
        r1attr1.setDataType(new URI("testDataType1"));
        List r1values1 = new ArrayList();
        r1values1.add("value");
        r1attr1.setAttributeStringValues(r1values1);
        Attribute r1attr2 = ContextFactory.getInstance().createAttribute();
        r1attr2.setAttributeId(new URI("testid2"));
        r1attr2.setDataType(new URI("testDataType2"));
        r1attr2.setIssuer("Bhavna");
        List r1values2 = new ArrayList();
        r1values2.add("value-1");
        r1attr2.setAttributeStringValues(r1values2);
        r1attrs.add(r1attr1);
        r1attrs.add(r1attr2);
        resource1.setAttributes(r1attrs);

        Resource resource2 =
                ContextFactory.getInstance().createResource();
        List<Attribute> r2attrs = new ArrayList<Attribute>();
        Attribute r2attr1 = ContextFactory.getInstance().createAttribute();
        r2attr1.setAttributeId(new URI("testid1"));
        r2attr1.setDataType(new URI("testDataType1"));
        List r2values1 = new ArrayList();
        r2values1.add("value-1");
        r2attr1.setAttributeStringValues(r2values1);
        Attribute r2attr2 = ContextFactory.getInstance().createAttribute();
        r2attr2.setAttributeId(new URI("testid2"));
        r2attr2.setDataType(new URI("testDataType2"));
        r2attr2.setIssuer("Bhavna");
        List r2values2 = new ArrayList();
        r2values2.add("value-2");
        r2attr2.setAttributeStringValues(r2values2);
        r2attrs.add(r2attr1);
        r2attrs.add(r2attr2);
        resource2.setAttributes(r1attrs);

        List resources = new ArrayList();
        resources.add(resource1);
        resources.add(resource2);
        request.setResources(resources);

        Action action1 =
                ContextFactory.getInstance().createAction();
        List<Attribute> a1attrs = new ArrayList<Attribute>();
        Attribute a1attr1 = ContextFactory.getInstance().createAttribute();
        a1attr1.setAttributeId(new URI("testid1"));
        a1attr1.setDataType(new URI("testDataType1"));
        List a1values1 = new ArrayList();
        a1values1.add("value-1");
        a1attr1.setAttributeStringValues(a1values1);
        Attribute a1attr2 = ContextFactory.getInstance().createAttribute();
        a1attr2.setAttributeId(new URI("testid2"));
        a1attr2.setDataType(new URI("testDataType2"));
        a1attr2.setIssuer("Bhavna");
        List a1values2 = new ArrayList();
        a1values2.add("value-1");
        a1attr2.setAttributeStringValues(a1values2);
        a1attrs.add(a1attr1);
        a1attrs.add(a1attr2);
        action1.setAttributes(a1attrs);
        request.setAction(action1);

        Environment environment1 =
                ContextFactory.getInstance().createEnvironment();
        List<Attribute> e1attrs = new ArrayList<Attribute>();
        Attribute e1attr1 = ContextFactory.getInstance().createAttribute();
        e1attr1.setAttributeId(new URI("testid1"));
        e1attr1.setDataType(new URI("testDataType1"));
        List e1values1 = new ArrayList();
        e1values1.add("value-1");
        e1attr1.setAttributeStringValues(e1values1);
        Attribute e1attr2 = ContextFactory.getInstance().createAttribute();
        e1attr2.setAttributeId(new URI("testid2"));
        e1attr2.setDataType(new URI("testDataType2"));
        e1attr2.setIssuer("Bhavna");
        List e1values2 = new ArrayList();
        e1values2.add("value-2");
        e1attr2.setAttributeStringValues(e1values2);
        e1attrs.add(e1attr1);
        e1attrs.add(e1attr2);
        environment1.setAttributes(e1attrs);
        request.setEnvironment(environment1);

        log(Level.INFO, "getRequest", 
             "query xml:\n" + request.toXMLString());
        log(Level.INFO, "getRequest", 
             "query xml, with ns declaration:\n" + request.toXMLString(true, true));

        log(Level.INFO, "getRequest", 
             "create query from xml sring");
        request = ContextFactory.getInstance().createRequest(
                request.toXMLString(true, true));
        log(Level.INFO, "getRequest", 
             "xml of  request created from xml sring" 
             + request.toXMLString(true, true));

    }
}
