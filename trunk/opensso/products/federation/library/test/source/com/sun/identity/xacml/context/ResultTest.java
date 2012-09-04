/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: ResultTest.java,v 1.3 2008/06/25 05:48:27 qcheng Exp $
 */
package com.sun.identity.xacml.context;

import com.sun.identity.shared.test.UnitTestBase;
import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Decision;
import com.sun.identity.xacml.context.Result;
import com.sun.identity.xacml.context.Status;
import com.sun.identity.xacml.context.StatusCode;
import com.sun.identity.xacml.context.StatusMessage;
import com.sun.identity.xacml.context.StatusDetail;
import com.sun.identity.xacml.policy.PolicyFactory;
import com.sun.identity.xacml.policy.Obligation;
import com.sun.identity.xacml.policy.Obligations;

import java.net.URI;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;

import org.testng.annotations.Test;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ResultTest extends UnitTestBase {

    public ResultTest() {
        super("FedLibrary-XACML-ResultTest");
    }

    //@Test(groups={"xacml"}, expectedExceptions={XACMLException.class})
    @Test(groups={"xacml"})
    public void getResult() throws XACMLException, Exception {

        entering("getResult()", null);
        log(Level.INFO, "getResult()","\n");
        log(Level.INFO, "getResult()","result-test-1-b");
        log(Level.INFO, "getResult()","\n");

        log(Level.INFO, "getResult()","create empty status code");
        StatusCode code = ContextFactory.getInstance().createStatusCode();
        code.setValue("10");
        code.setMinorCodeValue("5");
        String statusCodeXml = code.toXMLString(true, true);
        log(Level.INFO, "getResult()","status code xml:" + statusCodeXml);
        log(Level.INFO, "getResult()","\n");

        log(Level.INFO, "getResult()","create empty status message");
        StatusMessage message = ContextFactory.getInstance().createStatusMessage();
        message.setValue("success");
        String statusMessageXml = message.toXMLString(true, true);
        log(Level.INFO, "getResult()","status message xml:" + statusMessageXml);
        log(Level.INFO, "getResult()","\n");

        log(Level.INFO, "getResult()","create empty statusDetail");
        StatusDetail detail = ContextFactory.getInstance().createStatusDetail();
        log(Level.INFO, "getResult()","detail-xml:" + detail.toXMLString());
        log(Level.INFO, "getResult()","add a child");
        detail.getElement().insertBefore(detail.getElement().cloneNode(true), null);
        log(Level.INFO, "getResult()","detail-xml:" + detail.toXMLString());
        log(Level.INFO, "getResult()","create statusDetail from xml string");
        StatusDetail detail1 = ContextFactory.getInstance().createStatusDetail(
                detail.toXMLString(true, true));
        log(Level.INFO, "getResult()","detail-xml:" + detail1.toXMLString());
        log(Level.INFO, "getResult()","\n");

        log(Level.INFO, "getResult()","create empty status");
        Status status = ContextFactory.getInstance().createStatus();
        log(Level.INFO, "getResult()","status-xml:" + status.toXMLString());
        log(Level.INFO, "getResult()","set status code");
        status.setStatusCode(code);
        log(Level.INFO, "getResult()","status-xml:" + status.toXMLString());
        log(Level.INFO, "getResult()","set status message");
        status.setStatusMessage(message);
        log(Level.INFO, "getResult()","status-xml:" + status.toXMLString());
        log(Level.INFO, "getResult()","set status detail");
        status.setStatusDetail(detail1);
        log(Level.INFO, "getResult()","status-xml:" + status.toXMLString());
        log(Level.INFO, "getResult()","\n");
        log(Level.INFO, "getResult()","status-xml, with ns declared:" + status.toXMLString(true,
                true));
        log(Level.INFO, "getResult()","create status from xml");
        Status status1 = ContextFactory.getInstance().createStatus(
                status.toXMLString(true, true));
        log(Level.INFO, "getResult()","status-xml, with ns declared:" + status1.toXMLString(true,
                true));
        log(Level.INFO, "getResult()","\n");

        log(Level.INFO, "getResult()","create empty decision");
        Decision decision = ContextFactory.getInstance().createDecision();
        log(Level.INFO, "getResult()","decision-xml:" + decision.toXMLString());
        log(Level.INFO, "getResult()","set value to Permit");
        decision.setValue("Permit");
        log(Level.INFO, "getResult()","detail-xml:" + decision.toXMLString());
        log(Level.INFO, "getResult()","create decision from xml string");
        Decision decision1 = ContextFactory.getInstance().createDecision(
                decision.toXMLString(true, true));
        log(Level.INFO, "getResult()","decision-xml:" + decision1.toXMLString());
        log(Level.INFO, "getResult()","\n");

        log(Level.INFO, "getResult()","create obligation1");
        Obligation obligation1 = PolicyFactory.getInstance().createObligation();
        log(Level.INFO, "getResult()","set obligationId");
        obligation1.setObligationId(new URI("obligation-10"));
        log(Level.INFO, "getResult()","set fulfillOn");
        obligation1.setFulfillOn("Permit");

        log(Level.INFO, "getResult()","create obligation2");
        Obligation obligation2 = PolicyFactory.getInstance().createObligation();
        obligation2.setObligationId(new URI("obligation-20"));
        obligation2.setFulfillOn("Permit");
        List list = new ArrayList();
        Document doc = XMLUtils.newDocument();
        Element elem = doc.createElementNS(XACMLConstants.XACML_NS_URI, 
                "xacml:AttributeAssignment");
        elem.setAttribute("AttributeId", "a-120");
        elem.setAttribute("DataType", "f-120");
        list.add(elem);
        log(Level.INFO, "getResult()","setting attributeAssignments");
        obligation2.setAttributeAssignments(list);
        log(Level.INFO, "getResult()","obligation xml:" 
                + obligation2.toXMLString(true, true));

        log(Level.INFO, "getResult()","create obligations");
        Obligations obligations = PolicyFactory.getInstance().createObligations();
        obligations.addObligation(obligation1);
        obligations.addObligation(obligation2);
        log(Level.INFO, "getResult()","obligations xml:" 
                + obligation2.toXMLString(true, true));

        log(Level.INFO, "getResult()","create empty result");
        Result result = ContextFactory.getInstance().createResult();
        log(Level.INFO, "getResult()","result-xml:" + result.toXMLString());
        log(Level.INFO, "getResult()","resource id:" + result.getResourceId());
        log(Level.INFO, "getResult()","set resource id");
        log(Level.INFO, "getResult()","http://insat.red.iplanet.com/banner.html");
        result.setResourceId("http://insat.red.iplanet.com/banner.html");
        log(Level.INFO, "getResult()","get resource id:" + result.getResourceId());
        log(Level.INFO, "getResult()","result-xml:" + result.toXMLString());
        log(Level.INFO, "getResult()","set decision");
        result.setDecision(decision1);
        log(Level.INFO, "getResult()","result-xml:" + result.toXMLString());
        log(Level.INFO, "getResult()","set status");
        result.setStatus(status1);
        log(Level.INFO, "getResult()","set obligations");
        result.setObligations(obligations);
        log(Level.INFO, "getResult()","result-xml:" + result.toXMLString());
        log(Level.INFO, "getResult()","result-xml, with nsDeclaration:" 
                + result.toXMLString(true, true));
        log(Level.INFO, "getResult()","create result from xml string");
        Result result1 = ContextFactory.getInstance().createResult(
                result.toXMLString(true, true));
        log(Level.INFO, "getResult()","result-xml:" + result1.toXMLString());
        log(Level.INFO, "getResult()","\n");

        log(Level.INFO, "getResult()","result-test-1-e");
        log(Level.INFO, "getResult()","\n");
        exiting("getResult()");

    }

}
