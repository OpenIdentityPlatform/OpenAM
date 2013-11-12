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
 * $Id: XACMLSDKUtilsTest.java,v 1.3 2008/06/25 05:48:26 qcheng Exp $
 */

package com.sun.identity.xacml.common;

import com.sun.identity.shared.test.UnitTestBase;

import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Decision;

import java.util.logging.Level;

import org.testng.annotations.Test;

public class XACMLSDKUtilsTest extends UnitTestBase {

    public XACMLSDKUtilsTest() {
        super("FedLibrary-XACML-XACMLSDKUtilsTest");
    }

    //@Test(groups={"xacml"}, expectedExceptions={XACMLException.class})
    @Test(groups={"xacml"})
    public void isValidDecision() throws XACMLException {
        entering("isValidDecision()", null);
        log(Level.INFO,"isValidDecision()","\n");
        log(Level.INFO,"isValidDecision()","xacmlsdkutils-test-1b");
        log(Level.INFO,"isValidDecision()","decision value Permit is valid:" 
                + XACMLSDKUtils.isValidDecision("Permit"));
        log(Level.INFO,"isValidDecision()","decision value Deny is valid:" 
                + XACMLSDKUtils.isValidDecision("Deny"));
        log(Level.INFO,"isValidDecision()","decision value Indeterminate is valid:" 
                + XACMLSDKUtils.isValidDecision("NotApplicable"));
        log(Level.INFO,"isValidDecision()","decision value Indeterminate is valid:" 
                + XACMLSDKUtils.isValidDecision("NotApplicable"));
        log(Level.INFO,"isValidDecision()","decision value allow is valid:" 
                + XACMLSDKUtils.isValidDecision("allow"));
        log(Level.INFO,"isValidDecision()","decision value deny is valid:" 
                + XACMLSDKUtils.isValidDecision("deny"));
        log(Level.INFO,"isValidDecision()","xacmlsdkutils-test-1e");
        log(Level.INFO,"isValidDecision()","\n");
        exiting("isValidDecision()");
    }

}
