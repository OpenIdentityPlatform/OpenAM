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
 * $Id: ProtocolTest.java,v 1.5 2008/06/25 05:48:26 qcheng Exp $
 *
 */

package com.sun.identity.saml2.protocol;

import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml2.assertion.Conditions;
import com.sun.identity.saml2.assertion.AudienceRestriction;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import com.sun.identity.shared.test.UnitTestBase;
import java.util.Iterator;
import java.util.List;



/**
 * Unit Test Cases to test
 * <code>com.sun.identity.saml2.assertion.Assertion</code> class.
 */

public class ProtocolTest extends UnitTestBase {
    
    public ProtocolTest() {
        super("FedLibrary-SAML2");
    }
    
    /**
     * Validates <code>AuthnRequest</code> by passing in the XML File containing
     * the request XML String.
     *
     * @param xmlFile the file containing the <code>AuthnRequest</code> XML.
     * @throws SAML2Exception if there is creating the <code>Assertion</code>
     *         object or the XML String does not conform to the XML Schema
     * @throws ParserConfigurationException if there is an error parsing the
     *         Assertion XML string.
     * @throws IOException if there is an error reading the file.
     * @throws SAXException if there is an error during XML parsing.
     */
    @Parameters({"authnrequest-filename"})
    @Test(groups = {"saml2"})
    public void validateAuthnRequest(String xmlFile)throws SAML2Exception,
            ParserConfigurationException, IOException,SAXException {
        entering("createAuthnRequest", null);
        try {
            log(Level.INFO, "createAuthnRequest",xmlFile);
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element elt = doc.getDocumentElement();
            AuthnRequest req =
                    ProtocolFactory.getInstance().createAuthnRequest(elt);
            
            String xmlString = req.toXMLString(true,true);
            assert (xmlString != null) :
                "Error creating XML String from SAML2 AuthnRequest Object";
            log(Level.INFO, "createAuthnRequest",xmlString);
            
            assert (req.getVersion().equals("2.0")) : "Incorrect Version";
            
            assert (req.getID().equals("d2b7c388cec36fa7c39"))
            : "Incorrect RequestID";
            
            assert(DateUtils.toUTCDateFormat(req.getIssueInstant()).
                    equals("2006-11-30T22:32:20Z")) : "Invalid IssueInstant";
            
            assert(req.getDestination().equals("http://www.sp.com/sp"))
            : "Invalid Destination URI";
            
            assert(req.getConsent().equals("http://www.sp.com/consent"))
            : "Invalid Consent URI";
            
            assert(req.isForceAuthn().toString().equals("false"))
            : "Invalid ForceAuthn";
            
            assert(req.isPassive().toString().equals("false"))
            : " Invalid isPassive Value";
            
            assert(req.getProtocolBinding().equals(
                    SAML2Constants.HTTP_ARTIFACT))
                    : "Invalid ProtocolBinding Value";
            
            assert(req.getAssertionConsumerServiceIndex()
            .toString().equals("2"))
            :"Invalid Assertion Consumer Service Index" ;
            
            assert(req.getAssertionConsumerServiceURL()
            .equals("http://sp.sun.com/sp/assertionConsumer.jsp"))
            : "Incorrect AssertionConsumerService URL";
            
            assert(req.getAttributeConsumingServiceIndex()
            .toString().equals("4"))
            : "Incorrect AttributeConsumingServiceIndex Value";
            
            assert(req.getProviderName().equals("TEST SAML2"))
            : "Invalid ProviderName Value" ;
            
            
            Issuer issuer = req.getIssuer();
            assert(issuer.getFormat().equals(SAML2Constants.PERSISTENT)) :
                "Incorrect Format";
            
            assert(issuer.getNameQualifier().equals("test.com")) :
                "Incorrect Name Qualifier";
            
            assert(issuer.getSPNameQualifier().equals("sp.test.com")) :
                "Incorrect SP Name Qualifier";
            
            assert(issuer.getSPProvidedID().equals("sp1.test.com")) :
                "Incorrect SP Provider Identifier ";
            
            assert(issuer.getValue().equals("oCd/Q2KH7rgOSdX8KiIW54t4EUbC")) :
                "Incorrect Issuer Value";
            
            // validate extensions
            Extensions extensions = req.getExtensions();
            List extValues = extensions.getAny();
            Iterator i = extValues.iterator();
            while (i.hasNext()) {
                String extValue = (String)i.next();
                assert ((extValue.equals("<SAML1Element name=\"Extension1\"/>"))
                || (extValue.equals("<SAML2Element name=\"Extension2\"/>")))
                : "Invalid Extensions";
            }
            
            NameIDPolicy nameIDPolicy = req.getNameIDPolicy();
            assert (nameIDPolicy.getFormat().
                    equals("urn:oasis:names:tc:SAML:2.0:nameid-format:entity"))
                    : "Incorrect Format in AuthnRequest";
            
            assert(nameIDPolicy.getSPNameQualifier().equals(
                    "https://sp.example.org/sunsaml"))
                    : "Incorrect SP Name Qualifier";
            
            assert(!nameIDPolicy.isAllowCreate()) : "Invalid AllowCreate value";
            
            Conditions conditions = req.getConditions();
            
            assert(DateUtils.toUTCDateFormat(conditions.getNotBefore())
            .equals("2006-12-13T02:22:30Z"))
            : "Invalid NotBefore Attribute Value";
            
            assert(DateUtils.toUTCDateFormat(conditions.getNotOnOrAfter())
            .equals("2006-12-13T02:32:30Z"))
            : "Invalid NotBefore Attribute Value";
            
            List audResList = conditions.getAudienceRestrictions();
            AudienceRestriction audRes =
                    (AudienceRestriction) audResList.iterator().next();
            List audList = audRes.getAudience();
            String audience = (String)audList.iterator().next();
            
            assert(audience.equals("sp.test.sun.com")) : "Incorrect Audience";
            
            RequestedAuthnContext reqAuthnCtx = req.getRequestedAuthnContext();
            
            assert(
              ((String)reqAuthnCtx.getAuthnContextClassRef().iterator().next())
            .equals(SAML2Constants.CLASSREF_PASSWORD_PROTECTED_TRANSPORT))
            : "Invalid Authentication Class Reference";
            
            assert(reqAuthnCtx.getComparison().equals("exact"))
            : "Invalid Auth Comparison";
            
            Scoping scoping = req.getScoping();
            assert(scoping.getProxyCount().toString().equals("1"))
            : "Invalid Proxy Count";
            
            assert(scoping.getIDPList() != null) : "Invalid IDP List";
            
            List reqIDList = scoping.getRequesterIDs() ;
            assert(reqIDList != null && reqIDList.size() >0)
            : "Invalid RequesterID";
            
            List reqIDs = scoping.getRequesterIDs();
        } finally {
            exiting("createAuthnRequest");
        }
    }
   
    /**
     * Validates an <code>ArtifactResponse</code> by passing in the XML File
     * containing the Artifact Response message.
     *
     * @param xmlFile the file containing the Artifact Response.
     * @throws SAML2Exception if there is creating the <code>Assertion</code>
     *         object or the XML String does not conform to the XML Schema
     * @throws ParserConfigurationException if there is an error parsing the
     *         Assertion XML string.
     * @throws IOException if there is an error reading the file.
     * @throws SAXException if there is an error during XML parsing.
     */
    @Parameters({"artifactResponse-filename"})
    @Test(groups = {"saml2"})
    public void validateArtifactResponse(String xmlFile) throws SAML2Exception,
            ParserConfigurationException, IOException,SAXException {
        entering("validateArtifactResponse", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element elt = doc.getDocumentElement();
            ArtifactResponse artResp =
                    ProtocolFactory.getInstance().createArtifactResponse(elt);
            
            String xmlString = artResp.toXMLString(true,true);
            assert (xmlString != null) :
                "Error creating XML String from SAML2 AuthnRequest Object";
            log(Level.INFO, "validateArtifactResponse",xmlString);
            
            assert(artResp.getID().
                    equals("s2bce4ff9358784da21bb21eb966521cedba45e290"))
                    : "Incorrect ID";
            
            assert(artResp.getInResponseTo()
            .equals("s25df5dafe64c471dfc0d111b54efae0a1abeb9b2a"))
            : "Incorrect InResponseTo";
            
            assert(artResp.getVersion().equals("2.0")) : "Incorrect Version";
            
            assert(DateUtils.toUTCDateFormat(artResp.getIssueInstant()).
                    equals("2005-12-13T02:22:30Z")) : "Invalid IssueInstant";
            
            // validate issuer
            Issuer issuer = artResp.getIssuer();
            assert(issuer.getFormat().equals(SAML2Constants.PERSISTENT)) :
                "Incorrect Format";
            
            assert(issuer.getNameQualifier().equals("test.com")) :
                "Incorrect Name Qualifier";
            
            assert(issuer.getSPNameQualifier().equals("sp.test.com")) :
                "Incorrect SP Name Qualifier";
            
            assert(issuer.getSPProvidedID().equals("sp1.test.com")) :
                "Incorrect SP Provider Identifier ";
            
            assert(issuer.getValue().equals("oCd/Q2KH7rgOSdX8KiIW54t4EUbC")) :
                "Incorrect Issuer value";
            // end validate issuer
            
            // validate extensions
            Extensions extensions = artResp.getExtensions();
            List extValues = extensions.getAny();
            Iterator i = extValues.iterator();
            while (i.hasNext()) {
                String extValue = (String)i.next();
                assert ((extValue.equals("<SAML1Element name=\"Extension1\"/>"))
                || (extValue.equals("<SAML2Element name=\"Extension2\"/>")))
                : "Invalid Extensions";
            }
            
            //validate Status Code
            Status  sc = artResp.getStatus();
            assert (sc.getStatusCode().getValue()
            .equals(SAML2Constants.SUCCESS)):"Invalid Status";
            
            assert(sc.getStatusMessage().equals("Artifact Response Successful"))
            : "Invalid Status Message";
            
            validateStatusDetail(sc);
        } finally {
            exiting("validateArtifactResponse");
        }
    }
    
    /**
     * Validates an <code>ArtifactResolve</code> by passing in the XML File
     * containing the Artifact Resolve message.
     *
     * @param xmlFile the file containing the Artifact Resolve.
     * @throws SAML2Exception if there is creating the <code>Assertion</code>
     *         object or the XML String does not conform to the XML Schema
     * @throws ParserConfigurationException if there is an error parsing the
     *         Assertion XML string.
     * @throws IOException if there is an error reading the file.
     * @throws SAXException if there is an error during XML parsing.
     */
    @Parameters({"artifactResolve-filename"})
    @Test(groups = {"saml2"})
    public void validateArtifactResolve(String xmlFile)
    throws SAML2Exception, ParserConfigurationException, IOException,
            SAXException {
        entering("validateArtifactResolve", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element elt = doc.getDocumentElement();
            
            ArtifactResolve artResolve =
                    ProtocolFactory.getInstance().createArtifactResolve(elt);
            
            // object to xml string
            String xmlString = artResolve.toXMLString(true,true);
            assert (xmlString != null) :
                "Error creating XML String from Artifact object";
            log(Level.INFO, "validateArtifactResolve",xmlString);
            
            assert(artResolve.getID()
            .equals("s2bce4ff9358784da21bb21eb966521cedba45e290"))
            : "Incorrect Identifier";
            assert(artResolve.getVersion().equals("2.0")) : "Incorrect Version";
            
            assert(DateUtils.toUTCDateFormat(artResolve.getIssueInstant()).
                    equals("2005-12-13T02:22:30Z")) : "Invalid IssueInstant";
            
            assert(artResolve.getDestination()
            .equals("http://sp.sun.com/saml2/ArtifactResolution"))
            : "Incorrect Destination URI";
            
            assert(artResolve.getConsent()
            .equals("http://sp.sun.com/saml2/Consent"))
            : "Incorrect Consent URI";
            
            
            // validate issuer
            Issuer issuer = artResolve.getIssuer();
            assert(issuer.getFormat().equals(SAML2Constants.PERSISTENT)) :
                "Incorrect Format";
            
            assert(issuer.getNameQualifier().equals("test.com")) :
                "Incorrect Name Qualifier";
            
            assert(issuer.getSPNameQualifier().equals("sp.test.com")) :
                "Incorrect SP Name Qualifier";
            
            assert(issuer.getSPProvidedID().equals("sp1.test.com")) :
                "Incorrect SP Provider Identifier ";
            
            assert(issuer.getValue().equals("oCd/Q2KH7rgOSdX8KiIW54t4EUbC")) :
                "Incorrect Issuer value";
            // end validate issuer
            
            // validate extensions
            Extensions extensions = artResolve.getExtensions();
            List extValues = extensions.getAny();
            Iterator i = extValues.iterator();
            while (i.hasNext()) {
                String extValue = (String)i.next();
                assert ((extValue.equals("<SAML1Element name=\"Extension1\"/>"))
                || (extValue.equals("<SAML2Element name=\"Extension2\"/>")))
                : "Invalid Extensions";
            }
        } finally {
            exiting("validateArtifactResolve");
        }
    }
    
    /**
     * Validates the <code>ManageNameIDRequest</code> object.
     *
     * @param xmlFile the file containing the <code>ManageNameIDRequest</code>.
     * @throws SAML2Exception if there is creating the <code>Assertion</code>
     *         object or the XML String does not conform to the XML Schema
     * @throws ParserConfigurationException if there is an error parsing the
     *         Assertion XML string.
     * @throws IOException if there is an error reading the file.
     * @throws SAXException if there is an error during XML parsing.
     */
    @Parameters({"manageNameIDRequest-filename"})
    @Test(groups = {"saml2"})
    public void validateManageNameIDRequest(String xmlFile)
    throws SAML2Exception, ParserConfigurationException, IOException,
            SAXException {
        entering("validateManageNameIDRequest", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element elt = doc.getDocumentElement();
            
            ManageNameIDRequest mngNameIDReq =
                  ProtocolFactory.getInstance().createManageNameIDRequest(elt);
            
             // object to xml string
            String xmlString = mngNameIDReq.toXMLString(true,true);
            assert (xmlString != null) :
                "Error creating XML String from ManageNameIDRequest object";
            log(Level.INFO, "validateManageNameIDRequest",xmlString);
            
            assert(mngNameIDReq.getID()
            .equals("s2bce4ff9358784da21bb21eb966521cedba45e290"))
            : "Incorrect Identifier";
            
            String version = mngNameIDReq.getVersion();
            assert(mngNameIDReq.getVersion().equals("2.0"))
            : "Incorrect Version";
            
            assert(DateUtils.toUTCDateFormat(mngNameIDReq.getIssueInstant()).
                    equals("2006-12-13T02:22:30Z")) : "Invalid IssueInstant";
            
            assert(mngNameIDReq.getDestination()
            .equals("http://sp.sun.com/saml2/ArtifactResolution"))
            : "Incorrect Destination URI";
            
            assert(mngNameIDReq.getConsent()
            .equals("http://sp.sun.com/saml2/Consent"))
            : " Incorrect Consent URI";
            
            // validate issuer
            Issuer issuer = mngNameIDReq.getIssuer();
            assert(issuer.getFormat().equals(SAML2Constants.PERSISTENT)) :
                "Incorrect Format";
            
            assert(issuer.getNameQualifier().equals("test.com")) :
                "Incorrect Name Qualifier";
            
            assert(issuer.getSPNameQualifier().equals("sp.test.com")) :
                "Incorrect SP Name Qualifier";
            
            assert(issuer.getSPProvidedID().equals("sp1.test.com")) :
                "Incorrect SP Provider Identifier ";
            
            assert(issuer.getValue().equals("oCd/Q2KH7rgOSdX8KiIW54t4EUbC")) :
                "Incorrect Issuer value";
            // end validate issuer
            
            // validate extensions
            Extensions extensions = mngNameIDReq.getExtensions();
            List extValues = extensions.getAny();
            Iterator i = extValues.iterator();
            while (i.hasNext()) {
                String extValue = (String)i.next();
                assert ((extValue.equals("<SAML1Element name=\"Extension1\"/>"))
                || (extValue.equals("<SAML2Element name=\"Extension2\"/>")))
                : "Invalid Extensions";
            }
            
            // validate NameID
            NameID nameID = mngNameIDReq.getNameID();
            assert(nameID.getFormat().equals(SAML2Constants.PERSISTENT))
            : "Incorrect Format";
            
            assert(nameID.getNameQualifier().equals("test.com"))
            : "Incorrect Name Qualifier";
            
            assert(nameID.getSPNameQualifier().equals("sp.test.com"))
            : "Incorrect SP Name Qualifier";
            
            assert(nameID.getSPProvidedID().equals("sp1.test.com"))
            : "Incorrect SP Provider Identifier ";
            
            assert(nameID.getValue().equals("oCd/Q2KH7rgOSdX8KiIW54t4EUbC"))
            : "Incorrect NameID  value";
            
            //end validate NameID
            assert(mngNameIDReq.getTerminate()) : " Incorrect Terminate Value";
        } finally {
            exiting("validateManageNameIDRequest");
        }
    }
    
    
    /**
     * Validates <code>ManageNameIDResponse</code> object.
     *
     * @param xmlFile the file containing the <code>ManageNameIDResponse</code>.
     * @throws SAML2Exception if there is creating the <code>Assertion</code>
     *         object or the XML String does not conform to the XML Schema
     * @throws ParserConfigurationException if there is an error parsing the
     *         Assertion XML string.
     * @throws IOException if there is an error reading the file.
     * @throws SAXException if there is an error during XML parsing.
     */
    @Parameters({"manageNameIDResponse-filename"})
    @Test(groups = {"saml2"})
    public void validateManageNameIDResponse(String xmlFile)
    throws SAML2Exception, ParserConfigurationException, IOException,
            SAXException {
        entering("validateManageNameIDResponse", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element elt = doc.getDocumentElement();
            ManageNameIDResponse mngNameIDRes =
                  ProtocolFactory.getInstance().createManageNameIDResponse(elt);
            
            // object to xml string
            String xmlString = mngNameIDRes.toXMLString(true,true);
            assert (xmlString != null) :
                "Error creating XML String from ManageNameIDResponse object";
            log(Level.INFO, "validateManageNameIDResponse",xmlString);
            
            assert(mngNameIDRes.getID().equals("d2b7c388cec36fa7c39"))
            : "Incorrect ID";
            
            assert(mngNameIDRes.getInResponseTo()
            .equals("s25df5dafe64c471dfc0d111b54efae0a1abeb9b2a"))
            : "Incorrect InResponseTo";
            
            assert(mngNameIDRes.getVersion().equals("2.0"))
            : "Incorrect Version";
            
            assert(DateUtils.toUTCDateFormat(mngNameIDRes.getIssueInstant()).
                    equals("2006-12-13T02:22:30Z")) : "Invalid IssueInstant";
            
            // validate issuer
            Issuer issuer = mngNameIDRes.getIssuer();
            assert(issuer.getFormat().equals(SAML2Constants.PERSISTENT)) :
                "Incorrect Format";
            
            assert(issuer.getNameQualifier().equals("test.com")) :
                "Incorrect Name Qualifier";
            
            assert(issuer.getSPNameQualifier().equals("sptest.com")) :
                "Incorrect SP Name Qualifier";
            
            assert(issuer.getSPProvidedID().equals("sp1.test.com")) :
                "Incorrect SP Provider Identifier ";
            
            log(Level.INFO, "createArtifactResponse",issuer.getSPProvidedID());
            assert(issuer.getValue().equals("oCd/Q2KH7rgOSdX8KiIW54t4EUbC")) :
                "Incorrect Issuer value";
            // end validate issuer
            
            // validate extensions
            Extensions extensions = mngNameIDRes.getExtensions();
            List extValues = extensions.getAny();
            Iterator i = extValues.iterator();
            while (i.hasNext()) {
                String extValue = (String)i.next();
                assert ((extValue.equals("<SAML1Element name=\"Extension1\"/>"))
                || (extValue.equals("<SAML2Element name=\"Extension2\"/>")))
                : "Invalid Extensions";
            }
            
            //validate Status Code
            Status  sc = mngNameIDRes.getStatus();
            assert (sc.getStatusCode().getValue()
            .equals(SAML2Constants.SUCCESS)):"Invalid Status";
            
            assert(sc.getStatusMessage()
            .equals("ManageNameIDResponse Successful"))
            : "Invalid Status Message";
            
            validateStatusDetail(sc);
        } finally {
            exiting("validateManageNameIDResponse");
        }
    }
   
    /**
     * Creates and validates an <code>LogoutRequest</code> in the XML File
     * containing <code>LogoutRequest</code>.
     *
     * @param xmlFile the file containing the LogoutRequest.
     * @throws SAML2Exception if there is creating  <code>LogoutRequest</code>
     *         object or the XML String does not conform to the XML Schema
     * @throws ParserConfigurationException if there is an error parsing the
     *         Assertion XML string.
     * @throws IOException if there is an error reading the file.
     * @throws SAXException if there is an error during XML parsing.
     */
    @Parameters({"logoutRequest-filename"})
    @Test(groups = {"saml2"})
    public void validateLogoutRequest(String xmlFile)
    throws SAML2Exception, ParserConfigurationException, IOException,
            SAXException {
        entering("validateLogoutRequest", null);
        try {
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element elt = doc.getDocumentElement();
            LogoutRequest logoutReq =
                    ProtocolFactory.getInstance().createLogoutRequest(elt);
            
            // object to xml string
            String xmlString = logoutReq.toXMLString(true,true);
            assert (xmlString != null) :
                "Error creating XML String from LogoutRequest object";
            log(Level.INFO, "validateLogoutRequest",xmlString);
            
            assert(logoutReq.getID()
            .equals("s2bce4ff9358784da21bb21eb966521cedba45e290"))
            : "Incorrect Identifier";
            assert(logoutReq.getVersion().equals("2.0")) : "Incorrect Version";
            
            assert(DateUtils.toUTCDateFormat(logoutReq.getIssueInstant()).
                    equals("2006-12-13T02:22:30Z")) : "Invalid IssueInstant";
            assert(logoutReq.getDestination()
            .equals("http://sp.sun.com/saml2/ArtifactResolution"))
            : "Incorrect Destination URI";
            assert(logoutReq.getConsent()
            .equals("http://sp.sun.com/saml2/Consent"))
            : " Incorrect Consent URI";
            
            assert(DateUtils.toUTCDateFormat(logoutReq.getNotOnOrAfter())
            .equals("2007-12-13T02:22:30Z")) : "Invalid NotOnOrAfter Value";
            
            assert(logoutReq.getReason().equals("Session Expired"))
            : "Incorrect Reason";
            
            // validate issuer
            Issuer issuer = logoutReq.getIssuer();
            assert(issuer.getFormat().equals(SAML2Constants.PERSISTENT)) :
                "Incorrect Format";
            
            assert(issuer.getNameQualifier().equals("test.com")) :
                "Incorrect Name Qualifier";
            
            assert(issuer.getSPNameQualifier().equals("sp.test.com")) :
                "Incorrect SP Name Qualifier";
            
            assert(issuer.getSPProvidedID().equals("sp1.test.com")) :
                "Incorrect SP Provider Identifier ";
            
            log(Level.INFO, "createArtifactResponse",issuer.getSPProvidedID());
            assert(issuer.getValue().equals("oCd/Q2KH7rgOSdX8KiIW54t4EUbC")) :
                "Incorrect Issuer value";
            // end validate issuer
            
            // validate extensions
            Extensions extensions = logoutReq.getExtensions();
            List extValues = extensions.getAny();
            Iterator i = extValues.iterator();
            while (i.hasNext()) {
                String extValue = (String)i.next();
                assert ((extValue.equals("<SAML1Element name=\"Extension1\"/>"))
                || (extValue.equals("<SAML2Element name=\"Extension2\"/>")))
                : "Invalid Extensions";
            }
            
            // validate NameID
            NameID nameID = logoutReq.getNameID();
            assert(nameID.getFormat().equals(SAML2Constants.PERSISTENT)) :
                "Incorrect Format";
            
            assert(nameID.getNameQualifier().equals("test.com")) :
                "Incorrect Name Qualifier";
            
            assert(nameID.getSPNameQualifier().equals("sp.test.com")) :
                "Incorrect SP Name Qualifier";
            
            assert(nameID.getSPProvidedID().equals("sp1.test.com")) :
                "Incorrect SP Provider Identifier ";
            
            assert(nameID.getValue().equals("oCd/Q2KH7rgOSdX8KiIW54t4EUbC")) :
                "Incorrect NameID  value";
            //end validate NameID
        } finally {
            exiting("validateLogoutRequest");
        }
    }
    
    
    /**
     * Validates an <code>LogoutResponse</code> object.
     *
     * @param xmlFile the file containing the <code>LogoutResponse</code>.
     * @throws SAML2Exception if there is creating  <code>LogoutResponse</code>
     *         object or the XML String does not conform to the XML Schema
     * @throws ParserConfigurationException if there is an error parsing the
     *         Assertion XML string.
     * @throws IOException if there is an error reading the file.
     * @throws SAXException if there is an error during XML parsing.
     */
    @Parameters({"logoutResponse-filename"})
    @Test(groups = {"saml2"})
    public void validateLogoutResponse(String xmlFile)
    throws SAML2Exception, ParserConfigurationException, IOException,
            SAXException {
        entering("validateLogoutResponse", null);
        try {
            
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element elt = doc.getDocumentElement();
            LogoutResponse logoutResponse =
                    ProtocolFactory.getInstance().createLogoutResponse(elt);
            
            // object to xml string
            String xmlString = logoutResponse.toXMLString(true,true);
            assert (xmlString != null) :
                "Error creating XML String from LogoutResponse object";
            log(Level.INFO, "createLogoutResponse",xmlString);
            
            assert(logoutResponse.getID().
                    equals("s2bce4ff9358784da21bb21eb966521cedba45e290"))
                    : "Incorrect ID";
            
            assert(logoutResponse.getInResponseTo()
            .equals("s25df5dafe64c471dfc0d111b54efae0a1abeb9b2a"))
            : "Incorrect InResponseTo";
            
            assert(logoutResponse.getVersion().equals("2.0")) 
                   : "Incorrect Version";
            
            assert(DateUtils.toUTCDateFormat(logoutResponse.getIssueInstant()).
                    equals("2005-12-13T02:22:30Z")) : "Invalid IssueInstant";
            
            // validate issuer
            Issuer issuer = logoutResponse.getIssuer();
            assert(issuer.getFormat().equals(SAML2Constants.PERSISTENT)) :
                "Incorrect Format";
            
            assert(issuer.getNameQualifier().equals("test.com")) :
                "Incorrect Name Qualifier";
            
            assert(issuer.getSPNameQualifier().equals("sptest.com")) :
                "Incorrect SP Name Qualifier";
            
            assert(issuer.getSPProvidedID().equals("sp1.test.com")) :
                "Incorrect SP Provider Identifier ";

            assert(issuer.getValue().equals("oCd/Q2KH7rgOSdX8KiIW54t4EUbC")) :
                "Incorrect Issuer value";
            // end validate issuer
            
            // validate extensions
            Extensions extensions = logoutResponse.getExtensions();
            List extValues = extensions.getAny();
            Iterator i = extValues.iterator();
            while (i.hasNext()) {
                String extValue = (String)i.next();
                assert ((extValue.equals("<SAML1Element name=\"Extension1\"/>"))
                || (extValue.equals("<SAML2Element name=\"Extension2\"/>")))
                : "Invalid Extensions";
            }
            
            //validate Status Code
            Status  sc = logoutResponse.getStatus();
            assert (sc.getStatusCode().getValue()
            .equals(SAML2Constants.SUCCESS)):"Invalid Status";
            
            assert(sc.getStatusMessage().equals("Logout Response Successful"))
            : "Invalue Status Message";
            
            validateStatusDetail(sc); 
        } finally {
            exiting("validateLogoutResponse");
        }
    }
    
    /**
     * Validates the <code>StatusDetail</code> .
     *
     * @param sc the <code>Status</code> object.
     */
    private void validateStatusDetail(Status sc) {
        List values = sc.getStatusDetail().getAny();
        
        String val = (String)values.iterator().next();
        assert ((val.equals("<SAML1Detail name=\"Detail1\"/>")))
        :  "Incorrect Status Detail";
        
    }
}
