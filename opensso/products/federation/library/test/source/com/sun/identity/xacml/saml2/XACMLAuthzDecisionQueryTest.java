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
 * $Id: XACMLAuthzDecisionQueryTest.java,v 1.4 2008/06/25 05:48:29 qcheng Exp $
 *
 */

package com.sun.identity.xacml.saml2;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.ProtocolFactory;

import com.sun.identity.shared.test.UnitTestBase;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Request;
import com.sun.identity.xacml.context.Resource;
import com.sun.identity.xacml.context.Subject;
import com.sun.identity.xacml.context.Action;
import com.sun.identity.xacml.context.Environment;
import com.sun.identity.xacml.saml2.XACMLAuthzDecisionQuery;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.testng.annotations.Test;

public class XACMLAuthzDecisionQueryTest extends UnitTestBase {

    public XACMLAuthzDecisionQueryTest() {
        super("FedLibrary-XACML-XAMLAuthzDescisionQueryTest");
    }

    //@Test(groups={"xacml"}, expectedExceptions={XACMLException.class})
    @Test(groups={"xacml"})
    public void getXACMLAuthzDecision() throws XACMLException, SAML2Exception {
    
        /*
         * Construct Request
         * Construct Subject, add Subject
         * Construct Subject, add Subject
         * Construct Resource, add Resource
         * Construct Resource, add Resource
         * Construct Action, add Action
         * Construct Environment, add Environment
         * Construct XACMLAuthzDecisionQuery, add Request
         * do xacmlAuthzDecisionQuery.toXMLString(), 
         * construct xacmlAuthzDecisionQuery from the string
         */

        entering("getXACMLAuthzDecisionQueryTest()", null);

        log(Level.INFO, "getXACMLAuthzDecisionQueryTest", 
                "create empty XACMLAuthzDecisionQuery");
        XACMLAuthzDecisionQuery query 
                = ContextFactory.getInstance().createXACMLAuthzDecisionQuery();
        query.setID("query-1");
        query.setVersion("2.0");
        query.setIssueInstant(new Date());
        query.setDestination("destination-uri");
        query.setConsent("consent-uri");

        Issuer issuer = AssertionFactory.getInstance().createIssuer();
        issuer.setValue("issuer-1");
        issuer.setNameQualifier("name-qualifier");
        //issuer.setSPProvidedID("sp-provided-id");
        issuer.setSPNameQualifier("sp-name-qualifier");
        issuer.setSPNameQualifier("sp-name-qualifier");
        issuer.setFormat("format");
        query.setIssuer(issuer);

        Request request = ContextFactory.getInstance().createRequest();

        Subject subject1 = ContextFactory.getInstance().createSubject();
        Subject subject2 = ContextFactory.getInstance().createSubject();
        List<Subject> subjects = new ArrayList<Subject>();
        subjects.add(subject1);
        subjects.add(subject2);
        request.setSubjects(subjects);

        Resource resource1 = ContextFactory.getInstance().createResource();
        Resource resource2 = ContextFactory.getInstance().createResource();
        List<Resource> resources = new ArrayList<Resource>();
        resources.add(resource1);
        resources.add(resource2);
        request.setResources(resources);

        Action action = ContextFactory.getInstance().createAction();
        request.setAction(action);

        Environment environment = ContextFactory.getInstance().createEnvironment();
        request.setEnvironment(environment);

        query.setRequest(request);

        log(Level.INFO, "getXACMLAuthzDecisionQueryTest", 
             "query xml:\n" + query.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionQueryTest", 
             "query xml, with ns declaration:\n" + query.toXMLString(true, true));

        log(Level.INFO, "getXACMLAuthzDecisionQueryTest", 
             "create query from xml sring");
        query = ContextFactory.getInstance().createXACMLAuthzDecisionQuery(
                query.toXMLString(true, true));
        log(Level.INFO, "getXACMLAuthzDecisionQueryTest", 
             "xml of  query created from xml sring" 
             + query.toXMLString(true, true));

        /*
        log(Level.INFO, "getXACMLAuthzDecisionQueryTest", 
                "create empty Request");
        Request request = ContextFactory.getInstance().createRequest();
        log(Level.INFO, "getXACMLAuthzDecisionQueryTest", 
             "request xml:\n" + request.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionQueryTest", 
             "request xml, with ns declaration:\n" + request.toXMLString(true, true));
        */

        exiting("getXACMLAuthzDecisionStatement()");

    }
}
