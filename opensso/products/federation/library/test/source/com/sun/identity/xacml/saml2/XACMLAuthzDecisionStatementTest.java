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
 * $Id: XACMLAuthzDecisionStatementTest.java,v 1.4 2008/06/25 05:48:29 qcheng Exp $
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
import com.sun.identity.xacml.context.Decision;
import com.sun.identity.xacml.context.Response;
import com.sun.identity.xacml.context.Result;
import com.sun.identity.xacml.context.Status;
import com.sun.identity.xacml.context.StatusCode;
import com.sun.identity.xacml.context.StatusMessage;
import com.sun.identity.xacml.context.StatusDetail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.util.logging.Level;

import org.testng.annotations.Test;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XACMLAuthzDecisionStatementTest extends UnitTestBase {

    public XACMLAuthzDecisionStatementTest() {
        super("FedLibrary-XACML-XACMLAuthzDecisionStatementTest");
    }

    //@Test(groups={"xacml"}, expectedExceptions={XACMLException.class})
    @Test(groups={"xacml"})
    public void getXACMLAuthzDecision() throws XACMLException, SAML2Exception {

        entering("getXACMLAuthzDecisionStatement()", null);
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "\n");
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "xacml-authz-decision-statement-test-1-b");
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "\n");

        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "create empty status code");
        StatusCode code = ContextFactory.getInstance().createStatusCode();
        code.setValue("10");
        code.setMinorCodeValue("5");
        String statusCodeXml = code.toXMLString(true, true);
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "status code xml:" + statusCodeXml);
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "\n");

        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "create empty status message");
        StatusMessage message 
                = ContextFactory.getInstance().createStatusMessage();
        message.setValue("success");
        String statusMessageXml = message.toXMLString(true, true);
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "status message xml:" 
                + statusMessageXml);
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "\n");

        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "create empty statusDetail");
        StatusDetail detail 
                = ContextFactory.getInstance().createStatusDetail();
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "detail-xml:" + detail.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "add a child");
        detail.getElement().insertBefore(detail.getElement().cloneNode(true)
                , null);
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "detail-xml:" + detail.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "create statusDetail from xml string");
        StatusDetail detail1 = ContextFactory.getInstance().createStatusDetail(
                detail.toXMLString(true, true));
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "detail-xml:" + detail1.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "\n");

        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "create empty status");
        Status status = ContextFactory.getInstance().createStatus();
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "status-xml:" + status.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "set status code");
        status.setStatusCode(code);
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "status-xml:" + status.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "set status message");
        status.setStatusMessage(message);
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "status-xml:" + status.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "set status detail");
        status.setStatusDetail(detail1);
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "status-xml:" + status.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "\n");
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "status-xml, with ns declared:" 
                + status.toXMLString(true, true));
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "create status from xml");
        Status status1 = ContextFactory.getInstance().createStatus(
                status.toXMLString(true, true));
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "status-xml, with ns declared:" 
                + status1.toXMLString(true, true));
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "\n");

        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "create empty decision");
        Decision decision = ContextFactory.getInstance().createDecision();
        log(Level.INFO, "getXACMLAuthzDecisionStatement",  "decision-xml:" 
                + decision.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement",  
                "set value to Permit");
        decision.setValue("Permit");
        log(Level.INFO, "getXACMLAuthzDecisionStatement",  "detail-xml:" 
                + decision.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement",  
                "create decision from xml string");
        Decision decision1 = ContextFactory.getInstance().createDecision(
                decision.toXMLString(true, true));
        log(Level.INFO, "getXACMLAuthzDecisionStatement",  "decision-xml:" 
                + decision1.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement",  "\n");

        log(Level.INFO, "getXACMLAuthzDecisionStatement",  
                "create empty result");
        Result result = ContextFactory.getInstance().createResult();
        log(Level.INFO, "getXACMLAuthzDecisionStatement",  
                "result-xml:" + result.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement",  "resource id:" 
                + result.getResourceId());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "set resource id");
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "http://insat.red.iplanet.com/banner.html");
        result.setResourceId("http://insat.red.iplanet.com/banner.html");
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "get resource id:" 
                + result.getResourceId());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "result1-xml:" + result.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "set decision");
        result.setDecision(decision1);
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "result-xml:" + result.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "set status");
        result.setStatus(status1);
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "result-xml:" + result.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "result-xml, with nsDeclaration:" 
                + result.toXMLString(true, true));
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "create result from xml string");
        Result result1 = ContextFactory.getInstance().createResult(
                result.toXMLString(true, true));
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "result-xml:" + result1.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "\n");

        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "create empty response");
        Response response = ContextFactory.getInstance().createResponse();
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "response-xml:" 
                + response.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "add result");
        response.addResult(result);
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "response-xml:" 
                + response.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "add result");
        response.addResult(result1);
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "response-xml:" 
                + response.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "\n");
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "response-xml, with nsDeclaration:" 
                + response.toXMLString(true, true));
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "create response from xml string");
        Response response1 = ContextFactory.getInstance().createResponse(
                response.toXMLString(true, true));
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "response-xml:" 
                + response1.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "\n");

        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "create empty atuhz statement");
        XACMLAuthzDecisionStatement statement = ContextFactory.getInstance()
                .createXACMLAuthzDecisionStatement();
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "staement-xml:" 
                + statement.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "add response");
        statement.setResponse(response1);
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "statement-xml:" 
                + statement.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "statement-xml, with nsDeclaration:" 
                + statement.toXMLString(true, true));
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "create statement from xml string");
        XACMLAuthzDecisionStatement statement1 
                = ContextFactory.getInstance().createXACMLAuthzDecisionStatement(
                statement.toXMLString(true, true));
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "statement-xml:" 
                + statement1.toXMLString());
        log(Level.INFO, "getXACMLAuthzDecisionStatement", "\n");
        
        com.sun.identity.saml2.protocol.Response samlpResponse
                = ProtocolFactory.getInstance().createResponse();
        samlpResponse.setID("response-id:1");
        samlpResponse.setVersion("2.0");
        samlpResponse.setIssueInstant(new Date());
        com.sun.identity.saml2.protocol.StatusCode samlStatusCode
                = ProtocolFactory.getInstance().createStatusCode();
        samlStatusCode.setValue("stausCode");
        com.sun.identity.saml2.protocol.Status samlStatus
                = ProtocolFactory.getInstance().createStatus();
        samlStatus.setStatusCode(samlStatusCode);
        samlpResponse.setStatus(samlStatus );

        Assertion assertion = AssertionFactory.getInstance().createAssertion();
        assertion.setVersion("2.0");
        assertion.setID("response-id:1");
        assertion.setIssueInstant(new Date());
        Issuer issuer = AssertionFactory.getInstance().createIssuer();
        issuer.setValue("issuer-1");
        assertion.setIssuer(issuer);
        List<String> statements = new ArrayList<String>();
        statements.add(statement1.toXMLString(true, true));
        assertion.setStatements(statements);
        List<Assertion> assertions = new ArrayList<Assertion>();
        assertions.add(assertion);
        samlpResponse.setAssertion(assertions);
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "samlp-response-xml:\n" 
                + samlpResponse.toXMLString());

        com.sun.identity.saml2.protocol.Response samlpResponse1
                = ProtocolFactory.getInstance().createResponse(
                samlpResponse.toXMLString(true, true));
        log(Level.INFO, "getXACMLAuthzDecisionStatement", 
                "samlp-response1-xml:" 
                + samlpResponse1.toXMLString());


        exiting("getXACMLAuthzDecisionStatement()");

    }

}
