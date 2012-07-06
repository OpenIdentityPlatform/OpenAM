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
 * $Id: FMSubjectMapperTest.java,v 1.4 2008/06/25 05:50:18 qcheng Exp $
 *
 */

package com.sun.identity.xacml.plugins;

import com.sun.identity.shared.test.UnitTestBase;

import com.sun.identity.saml2.common.SAML2Exception;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.xacml.client.XACMLRequestProcessor;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Attribute;
import com.sun.identity.xacml.context.Subject;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.testng.annotations.Test;

public class FMSubjectMapperTest extends UnitTestBase {

    public FMSubjectMapperTest() {
        super("OpenFed-xacml-FMSubjectMapperTest");
    }

    @Test(groups={"xacml"})
    public void testConstrcutor() 
            throws XACMLException, SAML2Exception, URISyntaxException {
        FMSubjectMapper subjectMapper = new FMSubjectMapper();
        log(Level.INFO,"testConstructor():\n",null);
    }

    @Test(groups={"xacml"})
    public void testMapToNativeSubject()
        throws XACMLException, URISyntaxException {
	FMSubjectMapper subjectMapper = new FMSubjectMapper();
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
            new URI("urn:sun:names:xacml:2.0:data-type:opensso-session-id"));

        List<String> valueList = new ArrayList<String>();

	AuthContext lc = null;
	String callbacks[] = {"amadmin","admin123"};
        SSOToken ssot = null;
        try {
            lc = new AuthContext("/");
            AuthContext.IndexType indexType =
                AuthContext.IndexType.MODULE_INSTANCE;
            String indexName = "DataStore";
            log(Level.INFO, "testMapToNativeSubject():\n",
                " LDAPLogin: Obtained login context");
            lc.login(indexType, indexName, callbacks);
	    if (lc.getStatus() == AuthContext.Status.SUCCESS) {
		log(Level.INFO, "testMapToNativeSubject():\n",
                    " Login success!!");
	    }
            ssot = lc.getSSOToken();
        } catch (Exception le) {
            le.printStackTrace();
            log(Level.INFO,"testMapToNativeSubject():\n"," Login failed!!");
        }
        String sid = ssot.getTokenID().toString();
        log(Level.INFO,"testMapToNativeSubject():\n"," sid = "+sid);

        valueList.add(sid);
        attribute.setAttributeStringValues(valueList);
        List<Attribute> attributeList = new ArrayList<Attribute>();
        attributeList.add(attribute);
        subject1.setAttributes(attributeList);
        Subject subjects[] = {subject1};
        List<Subject> subjectsList = new ArrayList<Subject>();
        subjectsList.add(subject1);
        
        SSOToken retSSOToken = (SSOToken) subjectMapper.mapToNativeSubject(subjectsList);
        String retSid = retSSOToken.getTokenID().toString();
        log(Level.INFO,"testMapToNativeSubject():\n"," return sid = "+retSid);
        
    }

    /*
    public void testInitizlise() throws XACMLException {
    }
    */

}
