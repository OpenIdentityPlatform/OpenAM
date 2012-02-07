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
 * $Id: MessageTest.java,v 1.2 2008/06/25 05:48:23 qcheng Exp $
 *
 */

package com.sun.identity.federation;

import com.sun.identity.federation.common.FSException;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.protocol.Status;
import com.sun.identity.saml.protocol.StatusCode;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.FSAuthnResponse;
import com.sun.identity.federation.message.FSFederationTerminationNotification;
import com.sun.identity.federation.message.FSLogoutNotification;
import com.sun.identity.federation.message.FSLogoutResponse;
import com.sun.identity.federation.message.FSNameRegistrationRequest;
import com.sun.identity.federation.message.FSNameRegistrationResponse;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.message.common.IDPProvidedNameIdentifier;
import com.sun.identity.federation.message.common.OldProvidedNameIdentifier;
import com.sun.identity.federation.message.common.RequestAuthnContext;
import com.sun.identity.federation.message.common.SPProvidedNameIdentifier;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.test.UnitTestBase;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MessageTest extends UnitTestBase {
    public MessageTest() {
        super("FedLibrary-IDFF");
    }
    
    @Parameters({"authnrequest-filename"})
    @Test(groups = {"idff-message"})
    public void parseAuthnRequest(String xmlFile)
        throws FSException, FileNotFoundException
    {
        entering("parseAuthnRequest", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element element = doc.getDocumentElement();
            FSAuthnRequest req = new FSAuthnRequest(element);
  
            assert (req.getMajorVersion() == 1) : "wrong Major Version";
            assert (req.getMinorVersion() == 2) : "Wrong Minor Version";
            assert (req.getRespondWith().get(0).equals(
                "AuthenticationStatement")) : "Wrong RespondWith Element";
            assert (req.getProviderId().equals(
                "http://moonriver.red.iplanet.com")) : "Wrong Provider ID";
            assert (req.getForceAuthn() == false) : "Wrong ForceAuthn Element";
            assert (req.getIsPassive() == false) : "Wrong IsPassive Element";
            assert (req.getNameIDPolicy().equals("none")) : 
                "Wrong NameIDPolicy Element";
            assert (req.getAuthnContext().getAuthnContextComparison().equals(
                "minimum")) : "Wrong AuthnContextComparison Element";
            assert (req.getRelayState().equals(
                "http://moonriver.red.iplanet.com:58080/sp1/index.jsp")) :
                "Wrong Relay State";
            assert (req.toXMLString(true, true) != null) : 
                "Error in outputing XML string for the AuthnRequest";
            log(Level.INFO, "parseAuthnRequest", req.toXMLString(true, true));
        } finally {
            exiting("parseAuthnRequest");
        }
    }

    @Test(groups = {"idff-message"})
    public void createAuthnRequest()
        throws FSException
    {
        entering("createAuthnRequest", null);
        try {
            List<String> respondWiths = new ArrayList<String>();
            respondWiths.add("AuthenticationStatement");
            RequestAuthnContext context = new RequestAuthnContext();
            context.setMinorVersion(2);
            List<String> classRef = new ArrayList<String>();
            classRef.add(
              "http://www.projectliberty.org/schemas/authctx/classes/Password");
            context.setAuthnContextComparison("minimum");
            context.setAuthnContextClassRefList(classRef);
            FSAuthnRequest req = new FSAuthnRequest("12345567", respondWiths, 
                "http://www.sun.com", false, false, true, "none", 
                "http://projectliberty.org/profiles/brws-art", context,
                "http://moonriver.red.iplanet.com:58080/sp1/index.jsp",
                "minimum");
            req.setMinorVersion(2);

            assert (req.getMajorVersion() == 1) : "wrong Major Version";
            assert (req.getMinorVersion() == 2) : "Wrong Minor Version";
            assert (req.getRespondWith().get(0).equals(
                "AuthenticationStatement")) : "Wrong RespondWith Element";
            assert (req.getProviderId().equals(
                "http://www.sun.com")) : "Wrong Provider ID";
            assert (req.getForceAuthn() == false) : "Wrong ForceAuthn Element";
            assert (req.getIsPassive() == false) : "Wrong IsPassive Element";
            assert (req.getNameIDPolicy().equals("none")) : 
                "Wrong NameIDPolicy Element";
            assert (req.getAuthnContext().getAuthnContextComparison().equals(
                "minimum")) : "Wrong AuthnContextComparison Element";
            assert (req.getRelayState().equals(
                "http://moonriver.red.iplanet.com:58080/sp1/index.jsp")) :
                "Wrong Relay State";
            assert (req.toXMLString(true, true) != null) : 
                "Error in outputing XML string for the AuthnRequest";
            log(Level.INFO, "createAuthnRequest", req.toXMLString(true, true));
        } finally {
            exiting("createAuthnRequest");
        }
    }
    
    @Parameters({"authnresponse-filename"})
    @Test(groups = {"idff-message"})
    public void parseAuthnResponse(String xmlFile)
        throws FSException, FileNotFoundException, SAMLException
    {
        entering("parseAuthnResponse", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element element = doc.getDocumentElement();
            FSAuthnResponse resp = new FSAuthnResponse(element);
  
            assert (resp.getMajorVersion() == 1) : "wrong Major Version";
            assert ((resp.getMinorVersion() >= 0) && 
                (resp.getMinorVersion() <= 2)) : "Wrong Minor Version";
            assert ((resp.getInResponseTo() != null) || 
                (resp.getInResponseTo().length() == 0)) :
                "Missing InRespondTo Attribute";
            assert (resp.toXMLString(true, true) != null) : 
                "Error in outputing XML string for the AuthnResponse";
            log(Level.INFO, "parseAuthnResponse", resp.toXMLString(true, true));
        } finally {
            exiting("parseAuthnResponse");
        }
    }

    @Test(groups = {"idff-message"})
    public void createAuthnResponse()
        throws FSException, SAMLException
    {
        entering("createAuthnResponse", null);
        try {
            StatusCode code = new StatusCode("samlp:Success");
            Status status = new Status(code);
            FSAuthnResponse resp = new FSAuthnResponse("1234567", "7654321", 
                status, Collections.EMPTY_LIST,
                "http://www.sun.com:58080/sp1/index.jsp");
            resp.setMinorVersion(2);
            resp.setProviderId("http://www.sun.com");

            assert (resp.getMajorVersion() == 1) : "wrong Major Version";
            assert (resp.getMinorVersion() == 2) : "Wrong Minor Version";
            assert (resp.getStatus().getStatusCode().getValue().equals(
                "samlp:Success")) : "Wrong Status";
            assert (resp.getInResponseTo().equals(
                "7654321")) : "Wrong InResponseTo ID";
            assert (resp.getResponseID().equals("1234567")) : 
                "Wrong Response ID";
            assert (resp.getRelayState().equals(
                "http://www.sun.com:58080/sp1/index.jsp")) :
                "Wrong Relay State";
            assert (resp.getProviderId().equals("http://www.sun.com")) :
                "Wrong Provider ID";
            assert (resp.toXMLString(true, true) != null) : 
                "Error in outputing XML string for the AuthnResponse";
            log(Level.INFO, "createAuthnResponse",resp.toXMLString(true, true));
        } finally {
            exiting("createAuthnResponse");
        }
    }

    @Parameters({"logoutrequest-filename"})
    @Test(groups = {"idff-message"})
    public void parseLogoutRequest(String xmlFile)
        throws FSException, SAMLException, FileNotFoundException
    {
        entering("parseLogoutRequest", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element element = doc.getDocumentElement();
            FSLogoutNotification logoutReq = new FSLogoutNotification(element);

            assert (logoutReq.getMajorVersion() == 1) : "wrong Major Version";
            assert (logoutReq.getMinorVersion() == 2) : "Wrong Minor Version";
            assert ((logoutReq.getRequestID() != null) && 
                (logoutReq.getRequestID().length() != 0)) : 
                "Null Request ID";
            assert (logoutReq.getNameIdentifier() != null) : 
                "Wrong Name Identifier";
            assert ((logoutReq.getSessionIndex() != null) && 
                (logoutReq.getSessionIndex().length() != 0)) :
                "Null Session Index";
            assert ((logoutReq.getProviderId() != null) &&
                (logoutReq.getProviderId().length() != 0)) :
                "Null Provider ID";
            assert (logoutReq.toXMLString(true, true) != null) : 
                "Error in outputing XML string for the LogoutRequest";
            log(Level.INFO, "parseLogoutRequest",
                logoutReq.toXMLString(true, true));
        } finally {
            exiting("parseLogoutRequest");
        }
    }
    
    @Test(groups = {"idff-message"})
    public void createLogoutRequest()
        throws FSException, SAMLException
    {
        entering("createLogoutRequest", null);
        try {
            FSLogoutNotification logoutReq = new FSLogoutNotification();
            NameIdentifier nameIdentifier = new NameIdentifier(
                "uid=amadmin,ou=people,dc=sun,dc=com", "http://www.sun.com");
            logoutReq.setProviderId("http://www.sun.com");
            logoutReq.setNameIdentifier(nameIdentifier);
            logoutReq.setSessionIndex("123456789");
            logoutReq.setRequestID("123456789");
            logoutReq.setMinorVersion(2);

            assert (logoutReq.getMajorVersion() == 1) : "wrong Major Version";
            assert (logoutReq.getMinorVersion() == 2) : "Wrong Minor Version";
            assert (logoutReq.getRequestID().equals("123456789")) : 
                "Wrong Request ID";
            assert (logoutReq.getNameIdentifier() != null) : 
                "Wrong Name Identifier";
            assert (logoutReq.getSessionIndex().equals("123456789")) : 
                "Wrong Session Index";
            assert (logoutReq.getProviderId().equals("http://www.sun.com")) :
                "Wrong Provider ID";
            assert (logoutReq.toXMLString(true, true) != null) : 
                "Error in outputing XML string for the LogoutRequest";
            log(Level.INFO, "createLogoutRequest",
                logoutReq.toXMLString(true, true));
        } finally {
            exiting("createLogoutRequest");
        }
    }

    @Parameters({"logoutresponse-filename"})
    @Test(groups = {"idff-message"})
    public void parseLogoutResponse(String xmlFile)
        throws FSMsgException, SAMLException, FileNotFoundException
    {
        entering("parseLogoutResponse", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element element = doc.getDocumentElement();
            FSLogoutResponse logoutResp = new FSLogoutResponse(element);

            assert (logoutResp.getMajorVersion() == 1) : "wrong Major Version";
            assert (logoutResp.getMinorVersion() == 2) : "Wrong Minor Version";
            assert (logoutResp.getStatus() != null) : "Null Status";
            assert ((logoutResp.getResponseID() != null) &&
                (logoutResp.getResponseID().length() != 0)) : 
                "Null Response ID";
            assert ((logoutResp.getResponseTo() != null) && 
                (logoutResp.getResponseTo().length() != 0)) : 
                "Null InResponseTo Attribute";
            assert ((logoutResp.getProviderId() != null) &&
                (logoutResp.getProviderId().length() != 0)) :
                "Null Provider ID";
            assert (logoutResp.toXMLString(true, true) != null) : 
                "Error in outputing XML string for the LogoutResponse";
            log(Level.INFO, "parseLogoutResponse",
                logoutResp.toXMLString(true, true));
        } finally {
            exiting("parseLogoutResponse");
        }
    }
    
    @Test(groups = {"idff-message"})
    public void createLogoutResponse()
        throws FSMsgException, SAMLException
    {
        entering("createLogoutResponse", null);
        try {
            StatusCode code = new StatusCode("samlp:Success");
            Status status = new Status(code);
            FSLogoutResponse logoutResp = new FSLogoutResponse(
                "987654321", "123456789", status, "http://www.sun.com", 
                "http://www.sun.com/amserver/home.jsp");
            logoutResp.setMinorVersion(2);

            assert (logoutResp.getMajorVersion() == 1) : "wrong Major Version";
            assert (logoutResp.getMinorVersion() == 2) : "Wrong Minor Version";
            assert (logoutResp.getStatus() != null) : "Null Status";
            assert (logoutResp.getResponseID().equals("987654321")) : 
                "Wrong Response ID";
            assert (logoutResp.getResponseTo().equals("123456789")) : 
                "Wrong InResponseTo Attribute";
            assert (logoutResp.getProviderId().equals("http://www.sun.com")) :
                "Wrong Provider ID";
            assert (logoutResp.getRelayState().equals(
                "http://www.sun.com/amserver/home.jsp")) : "Wrong Relay State";
            assert (logoutResp.toXMLString(true, true) != null) : 
                "Error in outputing XML string for the LogoutResponse";
            log(Level.INFO, "createLogoutResponse",
                logoutResp.toXMLString(true, true));
        } finally {
            exiting("createLogoutResponse");
        }
    }


    @Test(groups = {"idff-message"})
    public void createNameRegistrationRequest()
        throws FSMsgException, SAMLException
    {
        entering("createNameRegistrationRequest", null);
        try {
            FSNameRegistrationRequest nameRegReq =
                new FSNameRegistrationRequest();
            nameRegReq.setProviderId("http://www.sun.com");
            NameIdentifier nameIdentifier = new NameIdentifier(
                "uid=amadmin,ou=people,dc=sun,dc=com", "http://www.sun.com");
            SPProvidedNameIdentifier newNameIdenifier = 
                new SPProvidedNameIdentifier(nameIdentifier.getName(), 
                nameIdentifier.getNameQualifier(),
                nameIdentifier.getFormat());
            nameRegReq.setSPProvidedNameIdentifier(newNameIdenifier);
            NameIdentifier remoteIdentifier = new NameIdentifier( 
                "uid=amadmin,ou=people,dc=iplanet,dc=com", 
                "http://www.idp.com");
            IDPProvidedNameIdentifier idpNameIdenifier =
                new IDPProvidedNameIdentifier(remoteIdentifier.getName(), 
                    remoteIdentifier.getNameQualifier(), 
                    remoteIdentifier.getFormat());
            nameRegReq.setIDPProvidedNameIdentifier(idpNameIdenifier);
            nameRegReq.setOldProvidedNameIdentifier( 
                new OldProvidedNameIdentifier(remoteIdentifier.getName(), 
                    remoteIdentifier.getNameQualifier(),
                    remoteIdentifier.getFormat()));
            nameRegReq.setRequestID("123456789");
            nameRegReq.setMinorVersion(2);

            assert (nameRegReq.getMajorVersion() == 1) : "wrong Major Version";
            assert (nameRegReq.getMinorVersion() == 2) : "Wrong Minor Version";
            assert (nameRegReq.getRequestID().equals("123456789")) : 
                "Wrong Request ID";
            assert (nameRegReq.getProviderId().equals("http://www.sun.com")) :
                "Wrong Provider ID";
            assert (nameRegReq.getIDPProvidedNameIdentifier() != null) :
                "Null IDPProvidedNameIdentifier";
            assert (nameRegReq.getSPProvidedNameIdentifier() != null) :
                "Null SPProvidedNameIdentifier";
            assert (nameRegReq.getOldProvidedNameIdentifier() != null) :
                "Null OldProvidedNameIdentifier";
            assert (nameRegReq.toXMLString(true, true) != null) : 
                "Error in outputing XML string for the NameRegistrationRequest";
            log(Level.INFO, "createNameRegistrationRequest",
                nameRegReq.toXMLString(true, true));
        } finally {
            exiting("createNameRegistrationRequest");
        }
    }

    @Test(groups = {"idff-message"})
    public void createNameRegistrationResponse()
        throws FSMsgException, SAMLException
    {
        entering("createNameRegistrationResponse", null);
        try {
            StatusCode code = new StatusCode("samlp:Success");
            Status status = new Status(code);
            FSNameRegistrationResponse nameRegResp = 
                new FSNameRegistrationResponse("987654321", "123456789", 
                    status, "http://www.sun.com", 
                    "http://www.sun.com/amserver/home.jsp");
            nameRegResp.setMinorVersion(2);

            assert (nameRegResp.getMajorVersion() == 1) : "wrong Major Version";
            assert (nameRegResp.getMinorVersion() == 2) : "Wrong Minor Version";
            assert (nameRegResp.getStatus() != null) : "Null Status";
            assert (nameRegResp.getResponseID().equals("987654321")) : 
                "Wrong Response ID";
            assert (nameRegResp.getInResponseTo().equals("123456789")) : 
                "Wrong InResponseTo Attribute";
            assert (nameRegResp.getProviderId().equals("http://www.sun.com")) :
                "Wrong Provider ID";
            assert (nameRegResp.getRelayState().equals(
                "http://www.sun.com/amserver/home.jsp")) : "Wrong Relay State";
            assert (nameRegResp.toXMLString(true, true) != null) : 
                "Error in outputing XML string for NameRegistrationResponse";
            log(Level.INFO, "createNameRegistrationResponse",
                nameRegResp.toXMLString(true, true));
        } finally {
            exiting("createNameRegistrationResponse");
        }
    }
    
    @Test(groups = {"idff-message"})
    public void createTerminationNotification()
        throws FSMsgException, SAMLException
    {
        entering("createTerminationNotification", null);
        try {
            NameIdentifier nameIdentifier = new NameIdentifier(
                "uid=amadmin,ou=people,dc=sun,dc=com", "http://www.sun.com");
            FSFederationTerminationNotification notification = 
                new FSFederationTerminationNotification("987654321", 
                    "http://www.sun.com", nameIdentifier); 
            notification.setMinorVersion(2);

            assert (notification.getMajorVersion() == 1) : "wrong Major Version";
            assert (notification.getMinorVersion() == 2) : "Wrong Minor Version";
            assert (notification.getNameIdentifier() != null) : 
                "Null Name Identifier";
            assert (notification.getRequestID().equals("987654321")) : 
                "Wrong Response ID";
            assert (notification.getProviderId().equals("http://www.sun.com")) :
                "Wrong Provider ID";
            assert (notification.toXMLString(true, true) != null) : 
                "Error in outputing XML string for NameRegistrationResponse";
            log(Level.INFO, "createTerminationNotification",
                notification.toXMLString(true, true));
        } finally {
            exiting("createTerminationNotification");
        }
    }
}
