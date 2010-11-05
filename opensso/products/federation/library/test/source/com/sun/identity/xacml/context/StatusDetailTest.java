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
 * $Id: StatusDetailTest.java,v 1.3 2008/06/25 05:48:27 qcheng Exp $
 */
package com.sun.identity.xacml.context;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.test.UnitTestBase;

import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.StatusCode;
import com.sun.identity.xacml.context.StatusDetail;

import java.util.logging.Level;

import org.testng.annotations.Test;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class StatusDetailTest extends UnitTestBase {

    public StatusDetailTest() {
        super("FedLibrary-XACML-StatusDetailTest");
    }

    //@Test(groups={"xacml"}, expectedExceptions={XACMLException.class})
    @Test(groups={"xacml"})
    public void getStatusDetail() throws XACMLException {

        entering("getStatusDetail()", null);
        log(Level.INFO,"getStatusDetail()","\n");
        log(Level.INFO,"getStatusDetail()","detail-test-1-b");
        StatusDetail detail = ContextFactory.getInstance().createStatusDetail();
        log(Level.INFO,"getStatusDetail()","detail-xml:" + detail.toXMLString());
        log(Level.INFO,"getStatusDetail()","\n");
        log(Level.INFO,"getStatusDetail()","detail-xml, include nsDeclaration:" 
                + detail.toXMLString(true, true));
        log(Level.INFO,"getStatusDetail()","\n");

        log(Level.INFO,"getStatusDetail()","create statusDetail form xml string");
        StatusDetail detail1 = ContextFactory.getInstance().createStatusDetail(
                detail.toXMLString(true, true));
        log(Level.INFO,"getStatusDetail()","detail-xml:" + detail1.toXMLString());
        log(Level.INFO,"getStatusDetail()","\n");

        StatusCode code = ContextFactory.getInstance().createStatusCode();
        code.setValue("10");
        code.setMinorCodeValue("5");
        String statusCodeXml = code.toXMLString(true, true);
        log(Level.INFO,"getStatusDetail()","status code xml:" + statusCodeXml);
        Document document = XMLUtils.toDOMDocument(statusCodeXml, XACMLSDKUtils.debug);
        Element statusElement = document.getDocumentElement();

        log(Level.INFO,"getStatusDetail()","create empty statusDetail");
        detail = ContextFactory.getInstance().createStatusDetail();
        log(Level.INFO,"getStatusDetail()","detail-xml:" + detail.toXMLString());
        log(Level.INFO,"getStatusDetail()","add a child");
        detail.getElement().insertBefore(detail.getElement().cloneNode(true), null);
        log(Level.INFO,"getStatusDetail()","detail-xml:" + detail.toXMLString());
        log(Level.INFO,"getStatusDetail()","create statusDetail form xml string");
        StatusDetail detail2 = ContextFactory.getInstance().createStatusDetail(
                detail.toXMLString(true, true));
        log(Level.INFO,"getStatusDetail()","detail-xml:" + detail2.toXMLString());
        log(Level.INFO,"getStatusDetail()","add a child second time");
        detail2.getElement().insertBefore(detail2.getElement().cloneNode(true), null);
        log(Level.INFO,"getStatusDetail()","detail-xml:" + detail2.toXMLString());
        log(Level.INFO,"getStatusDetail()","create statusDetail form xml string");
        StatusDetail detail3 = ContextFactory.getInstance().createStatusDetail(
                detail.toXMLString(true, true));
        log(Level.INFO,"getStatusDetail()","detail-xml:" + detail3.toXMLString());
        log(Level.INFO,"getStatusDetail()","\n");

        log(Level.INFO,"getStatusDetail()","detail-test-1-e");
        log(Level.INFO,"getStatusDetail()","\n");
        exiting("getStatusDetail()");

    }

}
