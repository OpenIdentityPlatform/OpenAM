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
 * $Id: IDPArtifactResolution.java,v 1.13 2009/11/20 21:41:16 exu Exp $
 *
 * Portions Copyrighted 2012-2015 ForgeRock AS.
 */

package com.sun.identity.saml2.profile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import org.w3c.dom.Element;

import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.protocol.Artifact;
import com.sun.identity.saml2.protocol.ArtifactResolve;
import com.sun.identity.saml2.protocol.ArtifactResponse;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.saml2.protocol.Status;
import com.sun.identity.saml2.protocol.StatusCode;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class handles the artifact resolution request 
 * from a service provider. It processes the artifact 
 * resolution request sent by the service provider and 
 * sends a proper SOAPMessage that contains an Assertion.
 */

public class IDPArtifactResolution {

    static MessageFactory messageFactory = null;
    static {
        try {
            messageFactory = MessageFactory.newInstance();
        } catch (SOAPException se) {
            SAML2Utils.debug.error("Unable to obtain SOAPFactory.", se);
        }
    }

    private IDPArtifactResolution() {
    }

    /**
     * This method processes the artifact resolution request coming 
     * from a service provider. It processes the artifact
     * resolution request sent by the service provider and 
     * sends back a proper SOAPMessage that contains an Assertion.
     *
     * @param request the <code>HttpServletRequest</code> object
     * @param response the <code>HttpServletResponse</code> object
     */

    public static void doArtifactResolution(
                        HttpServletRequest request,
                        HttpServletResponse response) {

        String classMethod = "IDPArtifactResolution.doArtifactResolution: ";
        try {
            String idpMetaAlias = request.getParameter(
                              SAML2MetaManager.NAME_META_ALIAS_IN_URI);
            if ((idpMetaAlias == null)
                || (idpMetaAlias.trim().length() == 0)) {
                idpMetaAlias = SAML2MetaUtils.getMetaAliasByUri(
                                              request.getRequestURI());
            }
            if ((idpMetaAlias == null) 
                || (idpMetaAlias.trim().length() == 0)) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod +
                        "unable to get IDP meta alias from request.");
                }
                String[] data = { idpMetaAlias };
                LogUtil.error(Level.INFO,
                    LogUtil.IDP_METADATA_ERROR, data, null);
                SAMLUtils.sendError(request, response, 
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "nullIDPMetaAlias",
                    SAML2Utils.bundle.getString("nullIDPMetaAlias"));
                return;
            }
          
            // retrieve IDP entity id from meta alias
            String idpEntityID = null;
            String realm = null;
            try {
                idpEntityID = IDPSSOUtil.metaManager.getEntityByMetaAlias(
                                                         idpMetaAlias);
                if ((idpEntityID == null) 
                    || (idpEntityID.trim().length() == 0)) {
                    SAMLUtils.debug.error(classMethod +
                        "Unable to get IDP Entity ID from meta.");
                    String[] data = { idpEntityID };
                    LogUtil.error(Level.INFO, 
                        LogUtil.INVALID_IDP, data, null);
                    SAMLUtils.sendError(request, response, 
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "nullIDPEntityID",
                        SAML2Utils.bundle.getString("nullIDPEntityID"));
                    return;
                }
                realm = SAML2MetaUtils.getRealmByMetaAlias(idpMetaAlias);
            } catch (SAML2MetaException sme) {
                SAML2Utils.debug.error(classMethod +
                    "Unable to get IDP Entity ID from meta.");
                String[] data = { idpMetaAlias };
                LogUtil.error(Level.INFO,
                    LogUtil.IDP_METADATA_ERROR, data, null);
                SAMLUtils.sendError(request, response, 
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "metaDataError",
                    SAML2Utils.bundle.getString("metaDataError"));
                return;
            }
            if (!SAML2Utils.isIDPProfileBindingSupported(
                realm, idpEntityID, SAML2Constants.ARTIFACT_RESOLUTION_SERVICE,
                SAML2Constants.SOAP)) 
            {
                SAML2Utils.debug.error(classMethod +
                    "Artifact Resolution Service binding: Redirect is not " +
                    "supported for " + idpEntityID);
                String[] data = { idpEntityID, SAML2Constants.SOAP };
                LogUtil.error(
                    Level.INFO, LogUtil.BINDING_NOT_SUPPORTED, data, null);
                SAMLUtils.sendError(request, response,
                   HttpServletResponse.SC_BAD_REQUEST, "unsupportedBinding",
                   SAML2Utils.bundle.getString("unsupportedBinding"));
                return;
            }
    
            try {
                // Get all the headers from the HTTP request
                MimeHeaders headers = getHeaders(request);
                // Get the body of the HTTP request
                InputStream is = request.getInputStream();
                SOAPMessage msg = 
                             messageFactory.createMessage(headers, is);
                SOAPMessage reply = null;
                reply = onMessage(msg, request, response, realm, idpEntityID);
                if (reply != null) {
                    /* Need to call saveChanges because we're
                     * going to use the MimeHeaders to set HTTP
                     * response information. These MimeHeaders
                     * are generated as part of the save. */
                    if (reply.saveRequired()) {
                        reply.saveChanges();
                    }
    
                    response.setStatus(HttpServletResponse.SC_OK);
                    putHeaders(reply.getMimeHeaders(), response);
        
                    // Write out the message on the response stream
                    OutputStream outputStream = response.getOutputStream();
                    reply.writeTo(outputStream);
                    outputStream.flush();
                } else {
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
            } catch (SOAPException ex) {
                SAML2Utils.debug.error(classMethod + "SOAP error", ex);
                String[] data = { idpEntityID };
                LogUtil.error(Level.INFO,
                    LogUtil.INVALID_SOAP_MESSAGE, data, null);
                SAMLUtils.sendError(request, response, 
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "invalidSOAPMessage",
                    SAML2Utils.bundle.getString("invalidSOAPMessage") +
                    " " + ex.getMessage());
                return;
            } catch (SAML2Exception se) {
                SAML2Utils.debug.error(classMethod + "SAML2 error", se);
                SAMLUtils.sendError(request, response, 
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "unableToCreateArtifactResponse",
                    SAML2Utils.bundle.getString(
                        "unableToCreateArtifactResponse") +
                    " " + se.getMessage());
                return;
            }
        } catch (IOException ioe) {
            SAML2Utils.debug.error(classMethod + "I/O rrror", ioe);
        } 
    }


    /**
     * This method generates a <code>SOAPMessage</code> containing the
     * <code>ArtifactResponse</code> that is corresponding to the
     * <code>ArtifactResolve</code> contained in the 
     * <code>SOAPMessage</code> passed in.
     *
     * @param message <code>SOAPMessage</code> contains a
     *             <code>ArtifactResolve</code> 
     * @param request the <code>HttpServletRequest</code> object
     * @param realm the realm to where the identity provider belongs
     * @param idpEntityID the entity id of the identity provider 
     * 
     * @return <code>SOAPMessage</code> contains the 
     *             <code>ArtifactResponse</code>
     * @exception SAML2Exception if the operation is not successful
     */

    public static SOAPMessage onMessage(SOAPMessage message, 
                                        HttpServletRequest request,
                                        HttpServletResponse response,
                                        String realm,
                                        String idpEntityID) 
        throws SAML2Exception {
    
        String classMethod = "IDPArtifactResolution.onMessage: ";
    
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(classMethod + "Entering onMessage().");
        }
    
        Element reqElem = SOAPCommunicator.getInstance().getSamlpElement(message,
                "ArtifactResolve");
        ArtifactResolve artResolve = 
            ProtocolFactory.getInstance().createArtifactResolve(reqElem);

        if (artResolve == null) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod +
                    "no valid ArtifactResolve node found in SOAP body.");
            }
            return SOAPCommunicator.getInstance().createSOAPFault(SAML2Constants.CLIENT_FAULT,
                    "noArtifactResolve", null);
        }

        String spEntityID = artResolve.getIssuer().getValue();
        if (!SAML2Utils.isSourceSiteValid(
            artResolve.getIssuer(), realm, idpEntityID)) 
        {
            SAML2Utils.debug.error(classMethod + spEntityID +
                " is not trusted issuer.");
            String[] data = { idpEntityID, realm, artResolve.getID() };
            LogUtil.error(
                Level.INFO, LogUtil.INVALID_ISSUER_REQUEST, data, null);
            return SOAPCommunicator.getInstance().createSOAPFault(SAML2Constants.CLIENT_FAULT,
                    "invalidIssuerInRequest", null);
        }
        SPSSODescriptorElement spSSODescriptor = null;
        try {
            spSSODescriptor = IDPSSOUtil.metaManager.
                      getSPSSODescriptor(realm, spEntityID);
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error(classMethod, sme);
            spSSODescriptor = null;
        }
        if (spSSODescriptor == null) {
            SAML2Utils.debug.error(classMethod +
                "Unable to get SP SSO Descriptor from meta.");
            return SOAPCommunicator.getInstance().createSOAPFault(SAML2Constants.SERVER_FAULT,
                    "metaDataError", null);
        }
        OrderedSet acsSet = SPSSOFederate.getACSUrl(spSSODescriptor,
            SAML2Constants.HTTP_ARTIFACT);
        String acsURL = (String) acsSet.get(0);
        //String protocolBinding = (String) acsSet.get(1);

        String isArtifactResolveSigned = 
           SAML2Utils.getAttributeValueFromSSOConfig(
               realm, idpEntityID, SAML2Constants.IDP_ROLE, 
               SAML2Constants.WANT_ARTIFACT_RESOLVE_SIGNED);
        if ((isArtifactResolveSigned != null) 
            && (isArtifactResolveSigned.equals(SAML2Constants.TRUE))) {
            if (!artResolve.isSigned()) {
                SAML2Utils.debug.error(classMethod +
                    "The artifact resolve is not signed " +
                    "when it is expected to be signed.");
                return SOAPCommunicator.getInstance().createSOAPFault(SAML2Constants.CLIENT_FAULT,
                        "ArtifactResolveNotSigned", null);
            }
            Set<X509Certificate> verificationCerts = KeyUtil.getVerificationCerts(spSSODescriptor, spEntityID,
                    SAML2Constants.SP_ROLE);
            if (!artResolve.isSignatureValid(verificationCerts)) {
                SAML2Utils.debug.error(classMethod +
                    "artifact resolve verification failed.");
                return SOAPCommunicator.getInstance().createSOAPFault(SAML2Constants.CLIENT_FAULT,
                        "invalidArtifact", null);
            }
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod +
                    "artifact resolve signature verification is successful.");
            }
        }
                
        Artifact art = artResolve.getArtifact();
        if (art == null) {
            SAML2Utils.debug.error(classMethod +
                "Unable to get an artifact from ArtifactResolve.");
            return SOAPCommunicator.getInstance().createSOAPFault(SAML2Constants.CLIENT_FAULT,
                    "invalidArtifactSignature", null);
        }

        String artStr = art.getArtifactValue();
        Response res = 
            (Response)IDPCache.responsesByArtifacts.remove(artStr);
        String remoteArtURL = null;

        boolean saml2FailoverEnabled = SAML2FailoverUtils.isSAML2FailoverEnabled();

        if (res == null) {
            // in LB case, artifact may reside on the other server.

            String targetServerID = SAML2Utils.extractServerId(art
                    .getMessageHandle());

            if (targetServerID == null) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod
                            + "target serverID is null");
                }
                return SOAPCommunicator.getInstance().createSOAPFault(SAML2Constants.CLIENT_FAULT,
                        "InvalidArtifactId", null);
            }

            String localServerID = SAML2Utils.getLocalServerID();
            boolean localTarget = localServerID.equals(targetServerID);

            if (!localTarget) {
                if (!SystemConfigurationUtil.isValidServerId(targetServerID)) {
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message(classMethod
                                + "target serverID is not valid: "
                                + targetServerID);
                    }
                    return SOAPCommunicator.getInstance().createSOAPFault(
                            SAML2Constants.CLIENT_FAULT, "InvalidArtifactId",
                            null);
                }
                try {
                    String remoteServiceURL = SystemConfigurationUtil
                            .getServerFromID(targetServerID);
                    remoteArtURL = remoteServiceURL
                            + SAML2Utils.removeDeployUri(request
                                    .getRequestURI());
                    SOAPConnection con = SOAPCommunicator.getInstance().openSOAPConnection();
                    SOAPMessage resMsg = con.call(message, remoteArtURL);
                    return resMsg;
                } catch (Exception ex) {
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug
                                .message(
                                        classMethod
                                                + "unable to forward request to remote server. "
                                                + "remote url = "
                                                + remoteArtURL, ex);
                    }
                    if (!saml2FailoverEnabled) {
                        return SOAPCommunicator.getInstance().createSOAPFault(
                                SAML2Constants.SERVER_FAULT,
                                "RemoteArtifactResolutionFailed", null);
                    }
                    // when the target server is running but the remote call was
                    // failed to this server (due to a network error)
                    // and the saml2failover is enabled, we can still find the
                    // artifact in the SAML2 repository.
                    // However the cached entry in the target server will not be
                    // deleted this way.
                }
            }

            if (saml2FailoverEnabled) {
                // Check the SAML2 Token Repository
                try {
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message("Artifact=" + artStr);
                    }
                    String tmp = (String) SAML2FailoverUtils.retrieveSAML2Token(artStr);
                    res = ProtocolFactory.getInstance().createResponse(tmp);
                } catch (SAML2Exception e) {
                    SAML2Utils.debug.error(classMethod + " SAML2 ERROR!!!", e);
                    return SOAPCommunicator.getInstance().createSOAPFault(
                            SAML2Constants.CLIENT_FAULT,
                            "UnableToFindResponseInRepo", null);
                } catch (SAML2TokenRepositoryException se) {
                    SAML2Utils.debug.error(classMethod + " There was a problem reading the response "
                            + "from the SAML2 Token Repository using artStr:" + artStr, se);
                    return SOAPCommunicator.getInstance().createSOAPFault(
                            SAML2Constants.CLIENT_FAULT,
                            "UnableToFindResponseInRepo", null);
                }
            }
        }

        if (res == null) {
            return SOAPCommunicator.getInstance().createSOAPFault(SAML2Constants.CLIENT_FAULT,
                    saml2FailoverEnabled ? "UnableToFindResponseInRepo"
                            : "UnableToFindResponse", null);
        }

        // Remove Response from SAML2 Token Repository
        try {
            if (saml2FailoverEnabled) {
                SAML2FailoverUtils.deleteSAML2Token(artStr);
            }
        } catch (SAML2TokenRepositoryException e) {
            SAML2Utils.debug.error(classMethod + 
                    " Error deleting the response from the SAML2 Token Repository using artStr:" + artStr, e);
        }

        Map props = new HashMap();
        String nameIDString = SAML2Utils.getNameIDStringFromResponse(res);
        if (nameIDString != null) {
            props.put(LogUtil.NAME_ID, nameIDString);
        }
        
        // check if need to sign the assertion
        boolean signAssertion = spSSODescriptor.isWantAssertionsSigned();
        if (signAssertion) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod +
                    "signing the assertion.");
            }
        }
        // encrypt the assertion or its NameID and/or Attribute based
        // on SP config setting and sign the assertion.
        IDPSSOUtil.signAndEncryptResponseComponents(realm, spEntityID, idpEntityID, res, signAssertion);

        ArtifactResponse artResponse = ProtocolFactory.getInstance().createArtifactResponse();

        Status status = ProtocolFactory.getInstance().createStatus();

        StatusCode statusCode = ProtocolFactory.getInstance().createStatusCode();
        statusCode.setValue(SAML2Constants.SUCCESS);
        status.setStatusCode(statusCode);
        
        // set the idp entity id as the response issuer
        Issuer issuer = AssertionFactory.getInstance().createIssuer();
        issuer.setValue(idpEntityID);
 
        artResponse.setStatus(status);
        artResponse.setID(SAML2Utils.generateID());
        artResponse.setInResponseTo(artResolve.getID());
        artResponse.setVersion(SAML2Constants.VERSION_2_0);
        artResponse.setIssueInstant(new Date());
        artResponse.setAny(res.toXMLString(true,true));
        artResponse.setIssuer(issuer);
        artResponse.setDestination(XMLUtils.escapeSpecialCharacters(acsURL)); 
        
        String wantArtifactResponseSigned = 
           SAML2Utils.getAttributeValueFromSSOConfig(realm, spEntityID, SAML2Constants.SP_ROLE,
               SAML2Constants.WANT_ARTIFACT_RESPONSE_SIGNED);
        if ((wantArtifactResponseSigned != null) && (wantArtifactResponseSigned.equals(SAML2Constants.TRUE))) {
            KeyProvider kp = KeyUtil.getKeyProviderInstance();
            if (kp == null) {
                SAML2Utils.debug.error(classMethod + "Unable to get a key provider instance.");
                return SOAPCommunicator.getInstance().createSOAPFault(SAML2Constants.SERVER_FAULT, "nullKeyProvider", null);
            }
            String idpSignCertAlias = SAML2Utils.getSigningCertAlias(realm, idpEntityID, SAML2Constants.IDP_ROLE);
            if (idpSignCertAlias == null) {
                SAML2Utils.debug.error(classMethod + "Unable to get the hosted IDP signing certificate alias.");
                return SOAPCommunicator.getInstance().createSOAPFault(SAML2Constants.SERVER_FAULT, "missingSigningCertAlias", null);
            }
            String encryptedKeyPass =
                    SAML2Utils.getSigningCertEncryptedKeyPass(realm, idpEntityID, SAML2Constants.IDP_ROLE);
            PrivateKey key;
            if (encryptedKeyPass == null || encryptedKeyPass.isEmpty()) {
                key = kp.getPrivateKey(idpSignCertAlias);
            } else {
                key = kp.getPrivateKey(idpSignCertAlias, encryptedKeyPass);
            }

            artResponse.sign(key, kp.getX509Certificate(idpSignCertAlias));
        }

        String str = artResponse.toXMLString(true,true);
        String[] logdata = {idpEntityID, artStr, str};
        LogUtil.access(Level.INFO, LogUtil.ARTIFACT_RESPONSE, logdata, null, props);
        if (str != null) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod + "ArtifactResponse message:\n"+ str);
            }
        } else {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod + "Unable to print ArtifactResponse message.");
            }
        }        

        SOAPMessage msg = null;
        try {
            msg = SOAPCommunicator.getInstance().createSOAPMessage(str, false);
        } catch (SOAPException se) {
            SAML2Utils.debug.error(classMethod + "Unable to create a SOAPMessage and add a document ", se);
            return SOAPCommunicator.getInstance().createSOAPFault(SAML2Constants.SERVER_FAULT, "unableToCreateSOAPMessage", null);
        }

        return msg;
    }

    // gets the MIME headers from a HTTPRequest
    private static MimeHeaders getHeaders(HttpServletRequest req) {
        Enumeration enumerator = req.getHeaderNames();
        MimeHeaders headers = new MimeHeaders();
        while (enumerator.hasMoreElements()) {
            String headerName = (String)enumerator.nextElement();
            String headerValue = req.getHeader(headerName);
            StringTokenizer values =
            new StringTokenizer(headerValue, ",");
            while (values.hasMoreTokens()) {
            headers.addHeader(
                headerName, values.nextToken().trim());
            }
        }
        return headers;
    }

    // puts MIME headers into a HTTPResponse
    private static void putHeaders(MimeHeaders headers, 
                                  HttpServletResponse res) {
        Iterator it = headers.getAllHeaders();
        while (it.hasNext()) {
            MimeHeader header = (MimeHeader)it.next();
            String[] values = headers.getHeader(header.getName());
            if (values.length == 1) {
            res.setHeader(header.getName(), header.getValue());
            } else {
            StringBuilder concat = new StringBuilder();
            int i = 0;
            while (i < values.length) {
                if (i != 0) {
                concat.append(',');
                }
                concat.append(values[i++]);
            }
            res.setHeader(header.getName(), concat.toString());
            }
        }
    }
} 
