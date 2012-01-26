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
 * $Id: SubjectTest.java,v 1.4 2008/06/25 05:48:28 qcheng Exp $
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
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import com.sun.identity.shared.xml.XMLUtils;
import org.xml.sax.SAXException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Unit Test Cases to test
 * <code>com.sun.identity.xacml.context.Subject</code> class.
 */

public class SubjectTest extends UnitTestBase {
    private static XACMLConstants xc;
    
    public SubjectTest() {
        super("FedLibrary-XACML-SubjectTest");
    }
    
    /**
     * Validates the <code>Subject</code> object.
     *
     * @param xmlFile the file containing the Subject XML.
     * @throws XACMLException if there is creating the <code>Subject</code>
     *         object or the XML String does not conform to the XML Schema
     * @throws ParserConfigurationException if there is an error parsing the
     *         Subject XML string.
     * @throws IOException if there is an error reading the file.
     * @throws SAXException if there is an error during XML parsing.
     */
    @Test(groups = {"xacml"})
    public void testSubject() throws XACMLException, URISyntaxException {
        entering("testSubject",null);
        try {
            Subject subject =
                    ContextFactory.getInstance().createSubject();
            subject.setSubjectCategory(new URI(xc.ACCESS_SUBJECT));
            List<Attribute> attrs = new ArrayList<Attribute>();
            Attribute attr = ContextFactory.getInstance().createAttribute();
            attr.setAttributeId(new URI("testid1"));
            attr.setDataType(new URI("testDataType1"));
            List values = new ArrayList();
            values.add("value");
            attr.setAttributeStringValues(values);
            attrs.add(attr);
            Attribute attr1 = ContextFactory.getInstance().createAttribute();
            attr1.setAttributeId(new URI("testid2"));
            attr1.setDataType(new URI("testDataType2"));
            attr1.setIssuer("Bhavna");
            List values1 = new ArrayList();
            values1.add("value-1");
            attr1.setAttributeStringValues(values1);
            attrs.add(attr1);
            subject.setAttributes(attrs);
            // object to xml string
            String xmlString = subject.toXMLString(true,true);
            System.out.println("subject xmlString:"+ xmlString);
            assert (xmlString != null) :
                "Error creating XML String from Subject object";
            // create Subject again from the String
            subject = ContextFactory.getInstance().createSubject(xmlString);
            URI subjectCategory = subject.getSubjectCategory();
            System.out.println("subjectCategory:"+subjectCategory.toString());
            System.out.println("subject string:"
                +subject.toXMLString(true, true));
            for (int j= 0; j < subject.getAttributes().size(); j++) {
                attr = (Attribute)subject.getAttributes().get(j);
                System.out.println("issuer:"+attr.getIssuer());
                System.out.println("attributId:"+attr.getAttributeId());
                System.out.println("datatype:"+attr.getDataType());
                System.out.println("attrValue:"
                    +attr.getAttributeValues().toString());
            }
        } finally {
            exiting("testSubject");
        }
    }
}
