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
 * $Id: StatusCodeTest.java,v 1.3 2008/06/25 05:48:27 qcheng Exp $
 */
package com.sun.identity.xacml.context;

import com.sun.identity.shared.test.UnitTestBase;

import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.StatusCode;

import java.util.logging.Level;

import org.testng.annotations.Test;

public class StatusCodeTest extends UnitTestBase {

    public StatusCodeTest() {
        super("FedLibrary-XACML-StatusCodeTest");
    }

    //@Test(groups={"xacml"}, expectedExceptions={XACMLException.class})
    @Test(groups={"xacml"})
    public void getStatusCode() throws XACMLException {

        entering("getStatusCode()", null);
        log(Level.INFO,"getStatusCode()","\n");
        log(Level.INFO,"getStatusCode()","code-test-1-b");
        StatusCode code = ContextFactory.getInstance().createStatusCode();

        log(Level.INFO,"getStatusCode()","set value to Permit");
        code.setValue("Permit");
        String xml = code.toXMLString();
        log(Level.INFO,"getStatusCode()","status code xml:" + xml);
        log(Level.INFO,"getStatusCode()","status code value:" + code.getValue());
        log(Level.INFO,"getStatusCode()","\n");

        log(Level.INFO,"getStatusCode()","set value to Deny");
        code.setValue("Deny");
        log(Level.INFO,"getStatusCode()","set minor code value to allow");
        code.setValue("Deny");
        code.setMinorCodeValue("allow");
        xml = code.toXMLString();
        log(Level.INFO,"getStatusCode()","status code xml:" + xml);
        log(Level.INFO,"getStatusCode()","status code value:" + code.getValue());
        log(Level.INFO,"getStatusCode()","minor code value:" + code.getMinorCodeValue());
        log(Level.INFO,"getStatusCode()","mutable value:" + code.isMutable());
        log(Level.INFO,"getStatusCode()","\n");

        log(Level.INFO,"getStatusCode()","make immutable");
        code.makeImmutable();
        xml = code.toXMLString(true, true);
        log(Level.INFO,"getStatusCode()","status code xml, include prefix, ns:" + xml);
        log(Level.INFO,"getStatusCode()","code value:" + code.getValue());
        log(Level.INFO,"getStatusCode()","mutable value:" + code.isMutable());
        log(Level.INFO,"getStatusCode()","\n");

        log(Level.INFO,"getStatusCode()","creating status code from xml");
        StatusCode code1 = ContextFactory.getInstance().createStatusCode(xml);
        log(Level.INFO,"getStatusCode()","status code value:" + code1.getValue());
        xml = code1.toXMLString();
        log(Level.INFO,"getStatusCode()","status code xml:" + xml);
        log(Level.INFO,"getStatusCode()","\n");

        log(Level.INFO,"getStatusCode()","code-test-1-e");
        log(Level.INFO,"getStatusCode()","\n");
        exiting("getStatusCode()");

    }

}
