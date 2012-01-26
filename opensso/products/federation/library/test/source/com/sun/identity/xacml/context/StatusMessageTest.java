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
 * $Id: StatusMessageTest.java,v 1.3 2008/06/25 05:48:27 qcheng Exp $
 */
package com.sun.identity.xacml.context;

import com.sun.identity.shared.test.UnitTestBase;

import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.StatusMessage;

import java.util.logging.Level;

import org.testng.annotations.Test;

public class StatusMessageTest extends UnitTestBase {

    public StatusMessageTest() {
        super("FedLibrary-XACML-StatusMessageTest");
    }

    //@Test(groups={"xacml"}, expectedExceptions={XACMLException.class})
    @Test(groups={"xacml"})
    public void getStatusMessage() throws XACMLException {

        log(Level.INFO,"getStatusMessage()","\n");
        entering("getStatusMessage()", null);
        log(Level.INFO,"getStatusMessage()","message-test-1-b");
        StatusMessage message = ContextFactory.getInstance().createStatusMessage();

        log(Level.INFO,"getStatusMessage()","set value to Permit");
        message.setValue("Permit");
        String xml = message.toXMLString();
        log(Level.INFO,"getStatusMessage()","message xml:" + xml);
        log(Level.INFO,"getStatusMessage()","message value:" + message.getValue());
        log(Level.INFO,"getStatusMessage()","\n");

        log(Level.INFO,"getStatusMessage()","set value to Deny");
        message.setValue("Deny");
        xml = message.toXMLString();
        log(Level.INFO,"getStatusMessage()","message xml:" + xml);
        log(Level.INFO,"getStatusMessage()","message value:" + message.getValue());
        log(Level.INFO,"getStatusMessage()","mutable value:" + message.isMutable());
        log(Level.INFO,"getStatusMessage()","\n");

        log(Level.INFO,"getStatusMessage()","make immutable");
        message.makeImmutable();
        xml = message.toXMLString();
        log(Level.INFO,"getStatusMessage()","message xml:" + xml);
        log(Level.INFO,"getStatusMessage()","message value:" + message.getValue());
        log(Level.INFO,"getStatusMessage()","mutable value:" + message.isMutable());
        log(Level.INFO,"getStatusMessage()","\n");


        log(Level.INFO,"getStatusMessage()","make immutable");
        message.makeImmutable();
        xml = message.toXMLString(true, true);
        log(Level.INFO,"getStatusMessage()","message xml, include prefix, ns:" + xml);
        log(Level.INFO,"getStatusMessage()","message value:" + message.getValue());
        log(Level.INFO,"getStatusMessage()","mutable value:" + message.isMutable());
        log(Level.INFO,"getStatusMessage()","\n");

        log(Level.INFO,"getStatusMessage()","creating message from xml");
        StatusMessage message1 = ContextFactory.getInstance().createStatusMessage(xml);
        log(Level.INFO,"getStatusMessage()","message value:" + message1.getValue());
        xml = message1.toXMLString();
        log(Level.INFO,"getStatusMessage()","message xml:" + xml);
        log(Level.INFO,"getStatusMessage()","\n");

        log(Level.INFO,"getStatusMessage()","message-test-1-e");
        log(Level.INFO,"getStatusMessage()","\n");
        exiting("getStatusMessage()");

    }

}
