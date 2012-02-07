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
 * $Id: DecisionTest.java,v 1.3 2008/06/25 05:48:27 qcheng Exp $
 */
package com.sun.identity.xacml.context;

import com.sun.identity.shared.test.UnitTestBase;

import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Decision;

import java.util.logging.Level;

import org.testng.annotations.Test;

public class DecisionTest extends UnitTestBase {

    public DecisionTest() {
        super("FedLibrary-XACML-DecisionTest");
    }

    //@Test(groups={"xacml"}, expectedExceptions={XACMLException.class})
    @Test(groups={"xacml"})
    public void getDecision() throws XACMLException {

        entering("getDecision()", null);
        log(Level.INFO,"getDecision()","\n");
        log(Level.INFO,"getDecision()","decision-test-1-b");
        Decision decision = ContextFactory.getInstance().createDecision();

        log(Level.INFO,"getDecision()","set value to Permit");
        decision.setValue("Permit");
        String xml = decision.toXMLString();
        log(Level.INFO,"getDecision()","decision xml:" + xml);
        log(Level.INFO,"getDecision()","decision value:" + decision.getValue());
        log(Level.INFO,"getDecision()","\n");

        log(Level.INFO,"getDecision()","set value to Deny");
        decision.setValue("Deny");
        xml = decision.toXMLString();
        log(Level.INFO,"getDecision()","decision xml:" + xml);
        log(Level.INFO,"getDecision()","decision value:" + decision.getValue());
        log(Level.INFO,"getDecision()","\n");

        log(Level.INFO,"getDecision()","set value to Indeterminate");
        decision.setValue("Indeterminate");
        xml = decision.toXMLString();
        log(Level.INFO,"getDecision()","decision xml:" + xml);
        log(Level.INFO,"getDecision()","decision value:" + decision.getValue());
        log(Level.INFO,"getDecision()","\n");

        log(Level.INFO,"getDecision()","set value to NotApplicable");
        decision.setValue("NotApplicable");
        xml = decision.toXMLString();
        log(Level.INFO,"getDecision()","decision xml:" + xml);
        log(Level.INFO,"getDecision()","decision value:" + decision.getValue());
        log(Level.INFO,"getDecision()","mutable value:" + decision.isMutable());
        log(Level.INFO,"getDecision()","\n");

        log(Level.INFO,"getDecision()","make immutable");
        decision.makeImmutable();
        xml = decision.toXMLString();
        log(Level.INFO,"getDecision()","decision xml:" + xml);
        log(Level.INFO,"getDecision()","decision value:" + decision.getValue());
        log(Level.INFO,"getDecision()","mutable value:" + decision.isMutable());
        //Decision decision1 = ContextFactory.getInstance().createDecision(xml);
        //log(Level.INFO,"getDecision()","decision value:" + decision1.getValue());
        //xml = decision1.toXMLString();
        //log(Level.INFO,"getDecision()","decision xml:" + xml);
        log(Level.INFO,"getDecision()","\n");


        log(Level.INFO,"getDecision()","make immutable");
        decision.makeImmutable();
        xml = decision.toXMLString(true, true);
        log(Level.INFO,"getDecision()","decision xml, include prefix, ns:" + xml);
        log(Level.INFO,"getDecision()","decision value:" + decision.getValue());
        log(Level.INFO,"getDecision()","mutable value:" + decision.isMutable());
        log(Level.INFO,"getDecision()","\n");

        log(Level.INFO,"getDecision()","creating decision from xml");
        Decision decision1 = ContextFactory.getInstance().createDecision(xml);
        log(Level.INFO,"getDecision()","decision value:" + decision1.getValue());
        xml = decision1.toXMLString();
        log(Level.INFO,"getDecision()","decision xml:" + xml);
        log(Level.INFO,"getDecision()","\n");

        log(Level.INFO,"getDecision()","decision-test-1-e");
        log(Level.INFO,"getDecision()","\n");
        exiting("getDecision()");

    }

}
