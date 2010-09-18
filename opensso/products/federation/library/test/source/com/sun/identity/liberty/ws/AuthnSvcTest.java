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
 * $Id: AuthnSvcTest.java,v 1.2 2008/06/25 05:48:24 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws;

import com.sun.identity.liberty.ws.authnsvc.AuthnSvcConstants;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcException;
import com.sun.identity.liberty.ws.authnsvc.protocol.SASLRequest;
import com.sun.identity.liberty.ws.authnsvc.protocol.SASLResponse;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.test.UnitTestBase;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AuthnSvcTest extends UnitTestBase {
    public AuthnSvcTest() {
        super("FedLibrary-AuthnSvc");
    }
    
    @Parameters({"saslrequest-filename"})
    @Test(groups = {"idwsf"})
    public void parseSASLRequest(String xmlFile)
        throws AuthnSvcException, FileNotFoundException
    {
        entering("parseSASLRequest", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element element = doc.getDocumentElement();
            SASLRequest req = new SASLRequest(element);
  
            assert (req.getData() != null) : "Null or empty Data Element";
            assert ((req.getMechanism() != null) && 
                (req.getMechanism().length() != 0)) : "Null or empty mechanism";
            log(Level.INFO, "parseSASLRequest", 
                XMLUtils.print(req.toElement()));
        } finally {
            exiting("parseSASLRequest");
        }
    }

    @Test(groups = {"idwsf"})
    public void createSASLRequest()
        throws AuthnSvcException
    {
        entering("createSASLRequest", null);
        try {
            String authzID = "amadmin";
            String advID = "password";
            SASLRequest saslReq =
                new SASLRequest(AuthnSvcConstants.MECHANISM_PLAIN);
            saslReq.setAuthzID(authzID);
            saslReq.setAdvisoryAuthnID(advID);

            assert (saslReq.getAuthzID().equals(authzID)) 
                : "Error in parsing Authz ID";
            assert (saslReq.getAdvisoryAuthnID().equals(advID)) 
                : "Error in parsing Advisory ID";
            assert ((saslReq.getMechanism() != null) && 
                (saslReq.getMechanism().equals(
                AuthnSvcConstants.MECHANISM_PLAIN))) 
                : "Error in parsing mechanism";
            log(Level.INFO, "createSASLRequest", 
                XMLUtils.print(saslReq.toElement()));
        } finally {
            exiting("createSASLRequest");
        }
    }
    
    @Parameters({"saslresponse-filename"})
    @Test(groups = {"idwsf"})
    public void parseSASLResponse(String xmlFile)
        throws AuthnSvcException, FileNotFoundException
    {
        entering("parseSASLResponse", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element element = doc.getDocumentElement();
            SASLResponse resp = new SASLResponse(element);
  
            assert ((resp.getStatusCode() != null) && 
                (resp.getStatusCode().length() != 0)) 
                : "Null or empty status code";
            assert (resp.getResourceOffering() != null)
                : "Null resource ID";
            log(Level.INFO, "parseSASLResponse", 
                XMLUtils.print(resp.toElement()));
        } finally {
            exiting("parseSASLResponse");
        }
    }
}
