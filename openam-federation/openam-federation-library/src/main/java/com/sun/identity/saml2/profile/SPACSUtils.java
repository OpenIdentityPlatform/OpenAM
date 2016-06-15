/*
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
 * $Id: SPACSUtils.java,v 1.48 2009/11/20 21:41:16 exu Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 */
package com.sun.identity.saml2.profile;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.liberty.ws.soapbinding.Message;
import com.sun.identity.liberty.ws.soapbinding.SOAPBindingException;
import com.sun.identity.liberty.ws.soapbinding.SOAPFaultException;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.monitoring.FedMonAgent;
import com.sun.identity.plugin.monitoring.FedMonSAML2Svc;
import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.assertion.Advice;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.AttributeStatement;
import com.sun.identity.saml2.assertion.EncryptedAttribute;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.common.AccountUtils;
import com.sun.identity.saml2.common.NameIDInfo;
import com.sun.identity.saml2.common.NameIDInfoKey;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.saml2.ecp.ECPFactory;
import com.sun.identity.saml2.ecp.ECPRelayState;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.AffiliationDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.ArtifactResolutionServiceElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.plugins.SAML2PluginsUtils;
import com.sun.identity.saml2.plugins.SAML2ServiceProviderAdapter;
import com.sun.identity.saml2.plugins.SPAccountMapper;
import com.sun.identity.saml2.plugins.SPAttributeMapper;
import com.sun.identity.saml2.protocol.Artifact;
import com.sun.identity.saml2.protocol.ArtifactResolve;
import com.sun.identity.saml2.protocol.ArtifactResponse;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.saml2.protocol.Status;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.saml2.audit.SAML2EventLogger;
import org.forgerock.openam.utils.ClientUtils;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class is used by a service provider (SP) to process the response from  
 * an identity provider for the SP's Assertion Consumer Service.
 *
 * @supported.api
 */
public class SPACSUtils {

    private static FedMonAgent agent = MonitorManager.getAgent();
    private static FedMonSAML2Svc saml2Svc = MonitorManager.getSAML2Svc();

    private SPACSUtils() {}

    /**
     * Retrieves <code>SAML</code> <code>Response</code> from http request.
     * It handles three cases:
     * <pre>
     * 1. using http method get using request parameter "resID".
     *    This is the case after local login is done.
     * 2. using http method get using request parameter "SAMLart".
     *    This is the case for artifact profile.
     * 3. using http method post. This is the case for post profile.
     * </pre>
     * 
     * @param request http servlet request
     * @param response http servlet response
     * @param orgName realm or organization name the service provider resides in
     * @param hostEntityId Entity ID of the hosted service provider
     * @param metaManager <code>SAML2MetaManager</code> instance.
     * @return <code>ResponseInfo</code> instance.
     * @throws SAML2Exception,IOException if it fails in the process.
     */
    public static ResponseInfo getResponse(
                                HttpServletRequest request,
                                HttpServletResponse response,
                                String orgName,
                                String hostEntityId,
                                SAML2MetaManager metaManager)
                throws SAML2Exception,IOException
    {
        ResponseInfo respInfo = null;

        String method = request.getMethod();
        if (method.equals("GET")) {
            if (!SAML2Utils.isSPProfileBindingSupported(
                orgName, hostEntityId, SAML2Constants.ACS_SERVICE,
                SAML2Constants.HTTP_ARTIFACT))
            {
                SAMLUtils.sendError(request, response, 
                    response.SC_BAD_REQUEST,
                    "unsupportedBinding", 
                    SAML2Utils.bundle.getString("unsupportedBinding"));
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("unsupportedBinding"));
            }
            respInfo = getResponseFromGet(request, response, orgName,
                                hostEntityId, metaManager);
        } else if (method.equals("POST")) {
            String pathInfo = request.getPathInfo();
            if ((pathInfo != null) && (pathInfo.startsWith("/ECP"))) {
                if (!SAML2Utils.isSPProfileBindingSupported(
                    orgName, hostEntityId, SAML2Constants.ACS_SERVICE,
                    SAML2Constants.PAOS))
                {
                SAMLUtils.sendError(request, response, 
                    response.SC_BAD_REQUEST,
                    "unsupportedBinding", 
                    SAML2Utils.bundle.getString("unsupportedBinding"));
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("unsupportedBinding"));
            }
                respInfo = getResponseFromPostECP(request, response, orgName,
                    hostEntityId, metaManager);
            } else {
                if (!SAML2Utils.isSPProfileBindingSupported(
                    orgName, hostEntityId, SAML2Constants.ACS_SERVICE,
                    SAML2Constants.HTTP_POST))
                {
                    SAMLUtils.sendError(request, response, 
                        response.SC_BAD_REQUEST,
                        "unsupportedBinding", 
                        SAML2Utils.bundle.getString("unsupportedBinding"));
                    throw new SAML2Exception(
                        SAML2Utils.bundle.getString("unsupportedBinding"));
                }
                respInfo = getResponseFromPost(request, response, orgName,
                    hostEntityId, metaManager);
            }
        } else {
            // not supported
            SAMLUtils.sendError(request, response, 
                response.SC_METHOD_NOT_ALLOWED,
                "notSupportedHTTPMethod", 
                SAML2Utils.bundle.getString("notSupportedHTTPMethod"));
            throw new SAML2Exception(
                        SAML2Utils.bundle.getString("notSupportedHTTPMethod"));
        }
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("SPACSUtils.getResponse: got response="
                    + respInfo.getResponse().toXMLString(true, true));
        }
        return respInfo;
    }

    /**
     * Retrieves <code>SAML Response</code> from http Get. 
     * It first uses parameter resID to retrieve <code>Response</code>. This is
     * the case after local login;
     * If resID is not defined, it then uses <code>SAMLart</code> http 
     * parameter to retrieve <code>Response</code>.
     */
    private static ResponseInfo getResponseFromGet(
                                HttpServletRequest request,
                                HttpServletResponse response,
                                String orgName,
                                String hostEntityId,
                                SAML2MetaManager metaManager)
                throws SAML2Exception,IOException
    {
        ResponseInfo respInfo = null;
        String resID = request.getParameter("resID");
        if (resID != null && resID.length() != 0) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("SPACSUtils.getResponseFromGet: resID="
                        + resID);
            }
            synchronized (SPCache.responseHash) {
                respInfo = (ResponseInfo) SPCache.responseHash.remove(resID);
            }
            if (respInfo == null) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("SPACSUtils.getResponseFromGet: "
                        + "couldn't find Response from resID.");
                }
                String[] data = {resID};
                LogUtil.error(Level.INFO,
                                LogUtil.RESPONSE_NOT_FOUND_FROM_CACHE,
                                data,
                                null);
                SAMLUtils.sendError(request, response, 
                    response.SC_INTERNAL_SERVER_ERROR, "SSOFailed",
                    SAML2Utils.bundle.getString("SSOFailed"));
                throw new SAML2Exception(
                        SAML2Utils.bundle.getString("SSOFailed"));
            }
            return respInfo;
        }

        String samlArt = request.getParameter(SAML2Constants.SAML_ART);
        if (samlArt == null || samlArt.trim().length() == 0) {
            SAML2Utils.debug.error("SPACSUtils.getResponseFromGet: Artifact "
                + "string is empty.");
            LogUtil.error(Level.INFO,
                        LogUtil.MISSING_ARTIFACT,
                        null,
                        null);
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                    "missingArtifact",
                    SAML2Utils.bundle.getString("missingArtifact"));
            throw new SAML2Exception(
                        SAML2Utils.bundle.getString("missingArtifact"));
        }

        return new ResponseInfo(getResponseFromArtifact(samlArt, hostEntityId,
            request, response, orgName, metaManager), 
            SAML2Constants.HTTP_ARTIFACT, null);
    }

    // Retrieves response using artifact profile.
    private static Response getResponseFromArtifact(String samlArt,
        String hostEntityId, HttpServletRequest request,
        HttpServletResponse response, String orgName,
        SAML2MetaManager sm) throws SAML2Exception,IOException
    {

        // Try to get source ID and endpointIndex, and then
        // decide which IDP and which artifact resolution service
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("SPACSUtils.getResponseFromArtifact: " +
                "samlArt = " + samlArt);
        }

        Artifact art = null;
        try {
            art = ProtocolFactory.getInstance().createArtifact(samlArt.trim());
            String[] data = {samlArt.trim()};
            LogUtil.access(Level.INFO,
                        LogUtil.RECEIVED_ARTIFACT,
                        data,
                        null);
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error("SPACSUtils.getResponseFromArtifact: "
                 + "Unable to decode and parse artifact string:" + samlArt);
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                "errorObtainArtifact",
                SAML2Utils.bundle.getString("errorObtainArtifact"));
            throw se;
        }

        String idpEntityID = getIDPEntityID(art, request, response, orgName, sm);
        IDPSSODescriptorElement idp = null;
        try {
            idp = sm.getIDPSSODescriptor(orgName, idpEntityID);
        } catch (SAML2MetaException se) {
            String[] data = {orgName, idpEntityID};
            LogUtil.error(Level.INFO,
                        LogUtil.IDP_META_NOT_FOUND,
                        data,
                        null);
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR,
                "failedToGetIDPSSODescriptor", se.getMessage());
            throw se;
        }

        String location = getIDPArtifactResolutionServiceUrl(
            art.getEndpointIndex(), idpEntityID, idp, request, response);

        // create ArtifactResolve message
        ArtifactResolve resolve = null;
        SOAPMessage resMsg = null;
        try {
            resolve = ProtocolFactory.getInstance().createArtifactResolve();
            resolve.setID(SAML2Utils.generateID());
            resolve.setVersion(SAML2Constants.VERSION_2_0);
            resolve.setIssueInstant(newDate());
            resolve.setArtifact(art);
            resolve.setDestination(XMLUtils.escapeSpecialCharacters(location));
            Issuer issuer = AssertionFactory.getInstance().createIssuer();
            issuer.setValue(hostEntityId);
            resolve.setIssuer(issuer);
            String needArtiResolveSigned =
                SAML2Utils.getAttributeValueFromSSOConfig(
                                orgName,
                                idpEntityID,
                                SAML2Constants.IDP_ROLE,
                                SAML2Constants.WANT_ARTIFACT_RESOLVE_SIGNED);
                                                        
            if (needArtiResolveSigned != null &&
                needArtiResolveSigned.equals("true")) {
                // or save it somewhere?
                String signAlias = getAttributeValueFromSPSSOConfig(
                                orgName,
                                hostEntityId,
                                sm,
                                SAML2Constants.SIGNING_CERT_ALIAS);
                if (signAlias == null) {
                    throw new SAML2Exception(
                        SAML2Utils.bundle.getString("missingSigningCertAlias"));
                }
                KeyProvider kp = KeyUtil.getKeyProviderInstance();
                if (kp == null) {
                    throw new SAML2Exception(
                        SAML2Utils.bundle.getString("nullKeyProvider"));
                }
                resolve.sign(kp.getPrivateKey(signAlias),
                                kp.getX509Certificate(signAlias));
            }

            String resolveString = resolve.toXMLString(true, true);
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("SPACSUtils.getResponseFromArtifact: "
                    + "ArtifactResolve=" + resolveString);
            }

            SOAPConnection con = SOAPCommunicator.getInstance().openSOAPConnection();
            SOAPMessage msg = SOAPCommunicator.getInstance().createSOAPMessage(resolveString, true);

            IDPSSOConfigElement config = null;
            config = sm.getIDPSSOConfig(orgName, idpEntityID);
            location = SAML2Utils.fillInBasicAuthInfo(
                config, location);
            resMsg = con.call(msg, location);
        } catch (SAML2Exception s2e) {
            SAML2Utils.debug.error("SPACSUtils.getResponseFromArtifact: "
                + "couldn't create ArtifactResolve:", s2e);
            String[] data = {hostEntityId, art.getArtifactValue()};
            LogUtil.error(Level.INFO,
                        LogUtil.CANNOT_CREATE_ARTIFACT_RESOLVE,
                        data,
                        null);
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR,
                "errorCreateArtifactResolve",
                SAML2Utils.bundle.getString("errorCreateArtifactResolve"));
            throw s2e;
        } catch (SOAPException se) {
            SAML2Utils.debug.error("SPACSUtils.getResponseFromGet: "
                + "couldn't get ArtifactResponse. SOAP error:",se);
            String[] data = {hostEntityId, location};
            LogUtil.error(Level.INFO,
                        LogUtil.CANNOT_GET_SOAP_RESPONSE,
                        data,
                        null);
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR,
                "errorInSOAPCommunication",
                SAML2Utils.bundle.getString("errorInSOAPCommunication"));
            throw new SAML2Exception(se.getMessage());
        }

        Response result = getResponseFromSOAP(resMsg, resolve, request, 
            response, idpEntityID, idp, orgName, hostEntityId, sm);
        String[] data = {hostEntityId, idpEntityID,
                        art.getArtifactValue(), ""};
        if (LogUtil.isAccessLoggable(Level.FINE)) {
            data[3] = result.toXMLString();
        }
        LogUtil.access(Level.INFO,
                        LogUtil.GOT_RESPONSE_FROM_ARTIFACT,
                        data,
                        null);
        return result;
    }

    // Finds the IDP who sends the artifact;
    private static String getIDPEntityID(
                Artifact art,
                HttpServletRequest request,
                HttpServletResponse response,
                String orgName,
                SAML2MetaManager metaManager)
                throws SAML2Exception,IOException
    {
        String sourceID = art.getSourceID();
        // find the idp
        String idpEntityID = null;
        try {
            Iterator iter =
                metaManager.getAllRemoteIdentityProviderEntities(orgName).
                        iterator();
            String tmpSourceID = null;
            while (iter.hasNext()) {
                idpEntityID = (String) iter.next();
                tmpSourceID = SAML2Utils.generateSourceID(idpEntityID);
                if (sourceID.equals(tmpSourceID)) {
                    break;
                }
                idpEntityID = null;
            }
            if (idpEntityID == null) {
                SAML2Utils.debug.error("SPACSUtils.getResponseFromGet: Unable "
                    + "to find the IDP based on the SourceID in the artifact");
                String[] data = {art.getArtifactValue(), orgName};
                LogUtil.error(Level.INFO,
                        LogUtil.IDP_NOT_FOUND,
                        data,
                        null);
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("cannotFindIDP"));
            }
        } catch (SAML2Exception se) {
            String[] data = {art.getArtifactValue(), orgName};
            LogUtil.error(Level.INFO,
                        LogUtil.IDP_NOT_FOUND,
                        data,
                        null);
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR,
                "cannotFindIDP", se.getMessage());
            throw se;
        }
        return idpEntityID;
    }

    // Retrieves the ArtifactResolutionServiceURL for an IDP.
    private static String getIDPArtifactResolutionServiceUrl(
                int endpointIndex,
                String idpEntityID,
                IDPSSODescriptorElement idp,
                HttpServletRequest request,
                HttpServletResponse response)
                throws SAML2Exception,IOException
    {
        // find the artifact resolution service url
        List arsList=idp.getArtifactResolutionService();
        ArtifactResolutionServiceElement ars = null;
        String location = null;
        String defaultLocation = null;
        String firstLocation = null;
        int index;
        boolean isDefault = false;
        for (int i=0; i<arsList.size(); i++) {
            ars = (ArtifactResolutionServiceElement)arsList.get(i);
            location = ars.getLocation();
            //String binding = ars.getBinding();
            index = ars.getIndex();
            isDefault = ars.isIsDefault();
            if (index == endpointIndex) {
                break;
            }
            if (isDefault) {
                defaultLocation = location;
            }
            if (i==0) {
                firstLocation = location;
            }
            location = null;
        }
        if (location == null || location.length() == 0) {
            location = defaultLocation;
            if (location == null || location.length() == 0) {
                location = firstLocation;
                if (location == null || location.length() == 0) {
                    SAML2Utils.debug.error("SPACSUtils: Unable to get the "
                        + "location of artifact resolution service for "
                        + idpEntityID);
                    String[] data = {idpEntityID};
                    LogUtil.error(Level.INFO,
                                LogUtil.ARTIFACT_RESOLUTION_URL_NOT_FOUND,
                                data,
                                null);
                    SAMLUtils.sendError(request, response, 
                        response.SC_INTERNAL_SERVER_ERROR,
                        "cannotFindArtifactResolutionUrl",
                        SAML2Utils.bundle.getString(
                            "cannotFindArtifactResolutionUrl"));
                    throw new SAML2Exception(
                        SAML2Utils.bundle.getString(
                            "cannotFindArtifactResolutionUrl"));
                }
            }
        }
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("SPACSUtils: IDP artifact resolution "
                    + "service url =" + location);
        }
        return location;
    }

    /**
     * Obtains <code>SAML Response</code> from <code>SOAPBody</code>.
     * Used by Artifact profile.
     */
    private static Response getResponseFromSOAP(SOAPMessage resMsg,
                                                ArtifactResolve resolve,
                                                HttpServletRequest request,
                                                HttpServletResponse response,
                                                String idpEntityID,
                                                IDPSSODescriptorElement idp,
                                                String orgName,
                                                String hostEntityId,
                                                SAML2MetaManager sm)
                throws SAML2Exception,IOException
    {
        String method = "SPACSUtils.getResponseFromSOAP:";
        Element resElem = null;
        try {
            resElem = SOAPCommunicator.getInstance().getSamlpElement(resMsg, "ArtifactResponse");
        } catch (SAML2Exception se) {
            String[] data = {idpEntityID};
            LogUtil.error(Level.INFO,
                        LogUtil.SOAP_ERROR,
                        data,
                        null);
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR,
                "soapError", se.getMessage());
            throw se; 
        }
        ArtifactResponse artiResp = null;
        try {
            artiResp = ProtocolFactory.getInstance().
                createArtifactResponse(resElem);
        } catch (SAML2Exception se) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(method + "Couldn't create "
                        + "ArtifactResponse:", se);
            }
            String[] data = {idpEntityID};
            LogUtil.error(Level.INFO,
                        LogUtil.CANNOT_INSTANTIATE_ARTIFACT_RESPONSE,
                        data,
                        null);
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR,
                "failedToCreateArtifactResponse", se.getMessage());
            throw se;
        }

        if (artiResp == null) {
            String[] data = {idpEntityID};
            LogUtil.error(Level.INFO,
                        LogUtil.MISSING_ARTIFACT_RESPONSE,
                        data,
                        null);
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR,
                "missingArtifactResponse",
                SAML2Utils.bundle.getString("missingArtifactResponse"));
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("missingArtifactResponse"));
        } else {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(method + "Received ArtifactResponse:"
                        + artiResp.toXMLString(true, true));
            }
        }

        // verify ArtifactResponse
        String wantArtiRespSigned = getAttributeValueFromSPSSOConfig(
                                orgName,
                                hostEntityId,
                                sm,
                                SAML2Constants.WANT_ARTIFACT_RESPONSE_SIGNED);
        if (wantArtiRespSigned != null && wantArtiRespSigned.equals("true")) {
            Set<X509Certificate> verificationCerts = KeyUtil.getVerificationCerts(idp, idpEntityID,
                    SAML2Constants.IDP_ROLE);
            if (!artiResp.isSigned() || !artiResp.isSignatureValid(verificationCerts)) {
                if (SAML2Utils.debug.messageEnabled()) {
                   SAML2Utils.debug.message(method 
                        + "ArtifactResponse's signature is invalid.");
                }
                String[] data = {idpEntityID};
                LogUtil.error(Level.INFO,
                        LogUtil.ARTIFACT_RESPONSE_INVALID_SIGNATURE,
                        data,
                        null);
                SAMLUtils.sendError(request, response, 
                    response.SC_INTERNAL_SERVER_ERROR, "invalidSignature",
                    SAML2Utils.bundle.getString("invalidSignature"));
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("invalidSignature"));
            }
        }

        String inResponseTo = artiResp.getInResponseTo();
        if (inResponseTo == null || !inResponseTo.equals(resolve.getID())) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(method 
                    + "ArtifactResponse's InResponseTo is invalid.");
            }
            String[] data = {idpEntityID};
            LogUtil.error(Level.INFO,
                        LogUtil.ARTIFACT_RESPONSE_INVALID_INRESPONSETO,
                        data,
                        null);
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR, "invalidInResponseTo",
                SAML2Utils.bundle.getString("invalidInResponseTo"));
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("invalidInResponseTo"));
        }

        Issuer idpIssuer = artiResp.getIssuer();
        if (idpIssuer == null || !idpIssuer.getValue().equals(idpEntityID)) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(method 
                    + "ArtifactResponse's Issuer is invalid.");
            }
            String[] data = {idpEntityID};
            LogUtil.error(Level.INFO,
                        LogUtil.ARTIFACT_RESPONSE_INVALID_ISSUER,
                        data,
                        null);
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR, "invalidIssuer",
                SAML2Utils.bundle.getString("invalidIssuer"));
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("invalidIssuer"));
        }

        // check time?

        Status status = artiResp.getStatus();
        if (status == null || !status.getStatusCode().getValue().equals(
                                        SAML2Constants.SUCCESS))
        {
            String statusCode =
                (status == null)?"":status.getStatusCode().getValue();
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(method 
                    + "ArtifactResponse's status code is not success."
                    + statusCode);
            }
            String[] data = {idpEntityID, ""};
            if (LogUtil.isErrorLoggable(Level.FINE)) {
                data[1] = statusCode;
            }
            LogUtil.error(Level.INFO,
                        LogUtil.ARTIFACT_RESPONSE_INVALID_STATUS_CODE,
                        data,
                        null);
            SAMLUtils.sendError(request, response,
                    response.SC_INTERNAL_SERVER_ERROR, "invalidStatusCode",
                    SAML2Utils.bundle.getString("invalidStatusCode"));
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("invalidStatusCode"));
        } 

        try {
            return ProtocolFactory.getInstance().createResponse(
                                artiResp.getAny());
        } catch (SAML2Exception se) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(method 
                    + "couldn't instantiate Response:", se);
            }
            String[] data = {idpEntityID};
            LogUtil.error(Level.INFO,
                        LogUtil.CANNOT_INSTANTIATE_RESPONSE_ARTIFACT,
                        data,
                        null);
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR, 
                "failedToCreateResponse", se.getMessage());
            throw se;
        }
    }

    /**
     * Obtains <code>SAML Response</code> from <code>SOAPBody</code>.
     * Used by ECP profile.
     */
    private static ResponseInfo getResponseFromPostECP(
        HttpServletRequest request, HttpServletResponse response,
        String orgName, String hostEntityId, SAML2MetaManager metaManager)
            throws SAML2Exception,IOException
    {
        Message message = null;
        try {
            message = new Message(SOAPCommunicator.getInstance().getSOAPMessage(request));
        } catch (SOAPException soapex) {
            String[] data = { hostEntityId } ;
            LogUtil.error(Level.INFO,
                LogUtil.CANNOT_INSTANTIATE_SOAP_MESSAGE_ECP, data, null);
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR,
                "failedToCreateSOAPMessage", soapex.getMessage());
            throw new SAML2Exception(soapex.getMessage()); 
        } catch (SOAPBindingException soapex) {
            String[] data = { hostEntityId } ;
            LogUtil.error(Level.INFO,
                LogUtil.CANNOT_INSTANTIATE_SOAP_MESSAGE_ECP, data, null);
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR,
                "failedToCreateSOAPMessage", soapex.getMessage());
            throw new SAML2Exception(soapex.getMessage()); 
        } catch(SOAPFaultException sfex) {
            String[] data = { hostEntityId } ;
            LogUtil.error(Level.INFO, LogUtil.RECEIVE_SOAP_FAULT_ECP,
                data, null);
            String faultString =
                sfex.getSOAPFaultMessage().getSOAPFault().getFaultString();
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR, 
                "failedToCreateSOAPMessage", faultString);
            throw new SAML2Exception(faultString);
        }

        List soapHeaders = message.getOtherSOAPHeaders();
        ECPRelayState ecpRelayState = null;
        if ((soapHeaders != null) && (!soapHeaders.isEmpty())) {
            for(Iterator iter = soapHeaders.iterator(); iter.hasNext();) {
                Element headerEle = (Element)iter.next();
                try {
                    ecpRelayState =
                        ECPFactory.getInstance().createECPRelayState(headerEle);
                    break;
                } catch (SAML2Exception saml2ex) {
                    // not ECP RelayState
                }
            }
        }
        String relayState = null;
        if (ecpRelayState != null) {
            relayState = ecpRelayState.getValue();
        }

        List soapBodies = message.getBodies();
        if ((soapBodies == null) || (soapBodies.isEmpty())) {
            String[] data = { hostEntityId } ;
            LogUtil.error(Level.INFO,
                LogUtil.CANNOT_INSTANTIATE_SAML_RESPONSE_FROM_ECP, data, null);
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                "missingSAMLResponse",
                SAML2Utils.bundle.getString("missingSAMLResponse"));
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("missingSAMLResponse"));
        }

        Element resElem = (Element)soapBodies.get(0);

        Response resp = null;
        try {
            resp = ProtocolFactory.getInstance().createResponse(resElem);
        } catch (SAML2Exception se) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("SPACSUtils.getResponseFromPostECP:" +
                    "Couldn't create Response:", se);
            }
            String[] data = { hostEntityId } ;
            LogUtil.error(Level.INFO,
                LogUtil.CANNOT_INSTANTIATE_SAML_RESPONSE_FROM_ECP, data, null);
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR,
                "failedToCreateResponse", se.getMessage());
            throw se;
        }

        String idpEntityID = resp.getIssuer().getValue();
        IDPSSODescriptorElement idpDesc = null;
        try {
            idpDesc = metaManager.getIDPSSODescriptor(orgName, idpEntityID);
        } catch (SAML2MetaException se) {
            String[] data = { orgName, idpEntityID };
            LogUtil.error(Level.INFO, LogUtil.IDP_META_NOT_FOUND, data, null);
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR,
                "failedToGetIDPSSODescriptor", se.getMessage());
            throw se;
        }

        Set<X509Certificate> certificates = KeyUtil.getVerificationCerts(idpDesc, idpEntityID, SAML2Constants.IDP_ROLE);
        List assertions = resp.getAssertion();
        if ((assertions != null) && (!assertions.isEmpty())) {
            for(Iterator iter = assertions.iterator(); iter.hasNext(); ) {
                Assertion assertion = (Assertion)iter.next();
                if (!assertion.isSigned()) {
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message(
                            "SPACSUtils.getResponseFromPostECP: " + 
                            " Assertion is not signed.");
                    }
                    String[] data = { idpEntityID };
                    LogUtil.error(Level.INFO,
                        LogUtil.ECP_ASSERTION_NOT_SIGNED, data, null);
                    SAMLUtils.sendError(request, response, 
                        response.SC_INTERNAL_SERVER_ERROR,
                        "assertionNotSigned",
                        SAML2Utils.bundle.getString("assertionNotSigned"));
                    throw new SAML2Exception(
                        SAML2Utils.bundle.getString("assertionNotSigned"));
                } else if (!assertion.isSignatureValid(certificates)) {
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message(
                            "SPACSUtils.getResponseFromPostECP: " + 
                            " Assertion signature is invalid.");
                    }
                    String[] data = { idpEntityID };
                    LogUtil.error(Level.INFO,
                        LogUtil.ECP_ASSERTION_INVALID_SIGNATURE, data, null);
                    SAMLUtils.sendError(request, response, 
                        response.SC_INTERNAL_SERVER_ERROR,
                        "invalidSignature",
                        SAML2Utils.bundle.getString("invalidSignature"));
                    throw new SAML2Exception(
                        SAML2Utils.bundle.getString("invalidSignature"));
                }
            }
        }

        return new ResponseInfo(resp, SAML2Constants.PAOS, relayState);
    }

    // Obtains SAML Response from POST.
    private static ResponseInfo getResponseFromPost(HttpServletRequest request,
        HttpServletResponse response, String orgName, String hostEntityId,
        SAML2MetaManager metaManager) throws SAML2Exception,IOException
    {
        String classMethod = "SPACSUtils:getResponseFromPost";
        SAML2Utils.debug.message("SPACSUtils:getResponseFromPost");

        String samlArt = request.getParameter(SAML2Constants.SAML_ART);
        if ((samlArt != null) && (samlArt.trim().length() != 0)) {
            return new ResponseInfo(getResponseFromArtifact(samlArt,
                hostEntityId, request, response, orgName, metaManager),
                SAML2Constants.HTTP_ARTIFACT, null);
        }

        String samlResponse = request.getParameter(
                        SAML2Constants.SAML_RESPONSE);
        if (samlResponse == null) {
            LogUtil.error(Level.INFO,
                        LogUtil.MISSING_SAML_RESPONSE_FROM_POST,
                        null,
                        null);
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                "missingSAMLResponse",
                SAML2Utils.bundle.getString("missingSAMLResponse"));
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("missingSAMLResponse"));
        }

        // Get Response back
        // decode the Response
        Response resp = null;
        ByteArrayInputStream bis = null;
        try {
            byte[] raw = Base64.decode(samlResponse);
            if (raw != null) {
                bis = new ByteArrayInputStream(raw);
                Document doc = XMLUtils.toDOMDocument(bis, SAML2Utils.debug);
                if (doc != null) {
                    resp = ProtocolFactory.getInstance().
                        createResponse(doc.getDocumentElement());
                }
            }
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error("SPACSUtils.getResponse: Exception "
                + "when instantiating SAMLResponse:", se);
            LogUtil.error(Level.INFO,
                        LogUtil.CANNOT_INSTANTIATE_RESPONSE_POST,
                        null,
                        null);
            SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                "errorObtainResponse",
                SAML2Utils.bundle.getString("errorObtainResponse"));
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("errorObtainResponse"));

        } catch (Exception e) {
            SAML2Utils.debug.error("SPACSUtils.getResponse: Exception "
                + "when decoding SAMLResponse:", e);
            LogUtil.error(Level.INFO,
                        LogUtil.CANNOT_DECODE_RESPONSE,
                        null,
                        null);
            SAMLUtils.sendError(request, response, 
                response.SC_INTERNAL_SERVER_ERROR, "errorDecodeResponse",
                SAML2Utils.bundle.getString("errorDecodeResponse"));
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("errorDecodeResponse"));
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (Exception ie) {
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message("SPACSUtils.getResponse: "
                            + "Exception when close the input stream:", ie);
                    }
                }
            }
        }

        if (resp != null) {
            String[] data = {""};
            if (LogUtil.isAccessLoggable(Level.FINE)) {
                data[0] = resp.toXMLString();
            }
            LogUtil.access(Level.INFO,
                           LogUtil.GOT_RESPONSE_FROM_POST,
                           data,
                           null);
            return (new ResponseInfo(resp, SAML2Constants.HTTP_POST, null));
        }
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("SPACSUtils.getResponse: Decoded response, " +
                                     "resp is null");
        }
        return null;
    }

    /**
     * Authenticates user with <code>Response</code>.
     * Auth session upgrade will be called if input session is
     * not null.
     * Otherwise, saml2 auth module is called. The name of the auth module
     * is retrieved from <code>SPSSOConfig</code>. If not found, "SAML2" will
     * be used.
     *
     * @param request HTTP Servlet request
     * @param response HTTP Servlet response.
     * @param out the print writer for writing out presentation
     * @param metaAlias metaAlias for the service provider
     * @param session input session object. It could be null.
     * @param respInfo <code>ResponseInfo</code> to be verified.
     * @param realm realm or organization name of the service provider.
     * @param hostEntityId hosted service provider Entity ID.
     * @param metaManager <code>SAML2MetaManager</code> instance for meta operation.
     * @param auditor a <code>SAML2EventLogger</code> auditor object to hook into
     *                tracking information for the saml request
     * @return <code>Object</code> which holds result of the session.
     * @throws SAML2Exception if the processing failed.
     */
    public static Object processResponse(
        HttpServletRequest request, HttpServletResponse response, PrintWriter out,
        String metaAlias, Object session, ResponseInfo respInfo,
        String realm, String hostEntityId, SAML2MetaManager metaManager, SAML2EventLogger auditor
    ) throws SAML2Exception {

        String classMethod = "SPACSUtils.processResponse: ";
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(classMethod + "Response : " +
                                     respInfo.getResponse());
        }        
        Map smap = null;
        try {
            // check Response/Assertion and get back a Map of relevant data
            smap = SAML2Utils.verifyResponse(request, response,
                respInfo.getResponse(), realm, hostEntityId,
                respInfo.getProfileBinding());
        } catch (SAML2Exception se) {
            // invoke SPAdapter for failure
            invokeSPAdapterForSSOFailure(hostEntityId, realm,
                request, response, smap, respInfo, 
                SAML2ServiceProviderAdapter.INVALID_RESPONSE, se);
            throw se;
        }
        
        com.sun.identity.saml2.assertion.Subject assertionSubject =
            (com.sun.identity.saml2.assertion.Subject)
            smap.get(SAML2Constants.SUBJECT);
        NameID nameId = assertionSubject.getNameID();
        EncryptedID encId = assertionSubject.getEncryptedID();
        Assertion authnAssertion =
            (Assertion) smap.get(SAML2Constants.POST_ASSERTION);
        String sessionIndex = (String)smap.get(SAML2Constants.SESSION_INDEX);
        respInfo.setSessionIndex(sessionIndex);
        Integer authLevel = (Integer) smap.get(SAML2Constants.AUTH_LEVEL);
        Long maxSessionTime = (Long) smap.get(SAML2Constants.MAX_SESSION_TIME);
        String inRespToResp = (String) smap.get(SAML2Constants.IN_RESPONSE_TO);
        List assertions = (List) smap.get(SAML2Constants.ASSERTIONS);
        
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(classMethod + "Assertions : " +
                                     assertions);
        }
       
        SPSSOConfigElement spssoconfig =
            metaManager.getSPSSOConfig(realm, hostEntityId);

        // get mappers
        SPAccountMapper acctMapper = SAML2Utils.getSPAccountMapper(realm, hostEntityId);
        SPAttributeMapper attrMapper = SAML2Utils.getSPAttributeMapper(realm, hostEntityId);

        boolean needAssertionEncrypted =
                Boolean.parseBoolean(SAML2Utils.getAttributeValueFromSPSSOConfig(spssoconfig,
                        SAML2Constants.WANT_ASSERTION_ENCRYPTED));

        boolean needAttributeEncrypted = getNeedAttributeEncrypted(needAssertionEncrypted, spssoconfig);
        boolean needNameIDEncrypted = getNeedNameIDEncrypted(needAssertionEncrypted, spssoconfig);

        Set<PrivateKey> decryptionKeys = KeyUtil.getDecryptionKeys(spssoconfig);
        if (needNameIDEncrypted && encId == null) {
            SAML2Utils.debug.error(classMethod +
                                   "process: NameID was not encrypted.");
            SAML2Exception se = new SAML2Exception(SAML2Utils.bundle.getString(
                "nameIDNotEncrypted"));
            // invoke SPAdapter for failure
            invokeSPAdapterForSSOFailure(hostEntityId, realm,
                request, response, smap, respInfo,
                SAML2ServiceProviderAdapter.INVALID_RESPONSE, se);
            throw se;
        }
        if (encId != null) {
            try {
                nameId = encId.decrypt(decryptionKeys);
            } catch (SAML2Exception se) {
                // invoke SPAdapter for failure
                invokeSPAdapterForSSOFailure(hostEntityId, realm,
                    request, response, smap, respInfo,
                    SAML2ServiceProviderAdapter.INVALID_RESPONSE, se);
                throw se;
            }
        }
        respInfo.setNameId(nameId);

        SPSSODescriptorElement spDesc = null;
        try {
            spDesc = metaManager.getSPSSODescriptor(realm, hostEntityId);
        } catch (SAML2MetaException ex) {
            SAML2Utils.debug.error(classMethod, ex);
        }
        if (spDesc == null) {
            SAML2Exception se = new SAML2Exception(SAML2Utils.bundle.getString(
                "metaDataError"));
            invokeSPAdapterForSSOFailure(hostEntityId, realm, request,
                response, smap, respInfo,
                SAML2ServiceProviderAdapter.SSO_FAILED_META_DATA_ERROR, se);
            throw se;
        }
        String nameIDFormat = nameId.getFormat();
        if (nameIDFormat != null) {
            List spNameIDFormatList = spDesc.getNameIDFormat();

            if ((spNameIDFormatList != null) && (!spNameIDFormatList.isEmpty())
                && (!spNameIDFormatList.contains(nameIDFormat))) {

                Object[] args = { nameIDFormat };
                SAML2Exception se = new SAML2Exception(SAML2Utils.BUNDLE_NAME,
                    "unsupportedNameIDFormatSP", args);

                invokeSPAdapterForSSOFailure(hostEntityId, realm, request,
                    response, smap, respInfo,
                    SAML2ServiceProviderAdapter.INVALID_RESPONSE, se);
                throw se;
            }
        }

        boolean isTransient = SAML2Constants.NAMEID_TRANSIENT_FORMAT.equals(nameIDFormat);
        boolean ignoreProfile = SAML2PluginsUtils.isIgnoredProfile(realm);
        String existUserName = null;
        SessionProvider sessionProvider = null;
        try {
            sessionProvider = SessionManager.getProvider();
        } catch (SessionException se) {
            // invoke SPAdapter for failure
            SAML2Exception se2 = new SAML2Exception(se);
            invokeSPAdapterForSSOFailure(hostEntityId, realm,
                request, response, smap, respInfo, 
                SAML2ServiceProviderAdapter.SSO_FAILED_SESSION_ERROR, se2);
            throw se2;
        }
        if (session != null) {
            try {
                existUserName = sessionProvider.getPrincipalName(session);
            } catch (SessionException se) {
                // invoke SPAdapter for failure
                SAML2Exception se2 = new SAML2Exception(se);
                invokeSPAdapterForSSOFailure(hostEntityId, realm, request, response, smap, respInfo,
                        SAML2ServiceProviderAdapter.SSO_FAILED_SESSION_ERROR, se2);
                throw se2;
            }
        }

        String remoteHostId = authnAssertion.getIssuer().getValue();
        String userName = null;
        boolean isNewAccountLink = false;
        boolean shouldPersistNameID = !isTransient && !ignoreProfile
                && acctMapper.shouldPersistNameIDFormat(realm, hostEntityId, remoteHostId, nameIDFormat);
        try {
            if (shouldPersistNameID) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod + "querying data store for existing federation links: realm = "
                            + realm + " hostEntityID = " + hostEntityId + " remoteEntityID = " + remoteHostId);
                }

                try {
                    userName = SAML2Utils.getDataStoreProvider().getUserID(realm, SAML2Utils.getNameIDKeyMap(
                            nameId, hostEntityId, remoteHostId, realm, SAML2Constants.SP_ROLE));
                } catch (DataStoreProviderException dse) {
                    SAML2Utils.debug.error(classMethod + "DataStoreProviderException whilst retrieving NameID " +
                            "information", dse);
                    throw new SAML2Exception(dse.getMessage());
                }
            }
            if (userName == null) {
                userName = acctMapper.getIdentity(authnAssertion, hostEntityId, realm);
                isNewAccountLink = true;
            }
        } catch (SAML2Exception se) {
            // invoke SPAdapter for failure
            invokeSPAdapterForSSOFailure(hostEntityId, realm, request, response, smap, respInfo,
                    SAML2ServiceProviderAdapter.SSO_FAILED_NO_USER_MAPPING, se);
            throw se;
        }

        if (userName == null && respInfo.isLocalLogin()) {
            // In case we just got authenticated locally, we should accept the freshly authenticated session's principal
            // as the username corresponding to the received assertion.
            userName = existUserName;
        }
        if (null != auditor) {
            auditor.setUserId(userName);
        }
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod + "process: userName =[" + userName + "]");
        }
        List attrs = null;
        for (Iterator it = assertions.iterator(); it.hasNext(); ) {
            Assertion assertion = (Assertion)it.next();
            List origAttrs = getSAMLAttributes(assertion, needAttributeEncrypted, decryptionKeys);
            if (origAttrs != null && !origAttrs.isEmpty()) {
                if (attrs == null) {
                    attrs = new ArrayList();
                }
                attrs.addAll(origAttrs);
            }
        }
        Map attrMap = null;
        if (attrs != null) {
            try {
                attrMap = attrMapper.getAttributes(attrs, userName,
                    hostEntityId, remoteHostId, realm);
            } catch (SAML2Exception se) {
                // invoke SPAdapter for failure
                invokeSPAdapterForSSOFailure(hostEntityId, realm,
                    request, response, smap, respInfo, 
                    SAML2ServiceProviderAdapter.SSO_FAILED_ATTRIBUTE_MAPPING, 
                    se);
                throw se;
            }
        }
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod + "process: remoteHostId = " + remoteHostId);
            SAML2Utils.debug.message(
                classMethod + "process: attrMap = " + attrMap);
        }
        respInfo.setAttributeMap(attrMap);

        // return error code for local user login
        if (StringUtils.isEmpty(userName)) {
            // If we couldn't determine the username based on the incoming assertion, then we shouldn't automatically
            // map the user to the existing session.
            if (session != null) {
                try {
                    sessionProvider.invalidateSession(session, request, response);
                } catch (SessionException se) {
                    SAML2Utils.debug.error("An error occurred while trying to invalidate session", se);
                }
            }
            throw new SAML2Exception(SAML2Utils.bundle.getString("noUserMapping"));
        }

        boolean writeFedInfo = isNewAccountLink && shouldPersistNameID;

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                classMethod + "userName : " + userName);
            SAML2Utils.debug.message(
                classMethod + "writeFedInfo : " + writeFedInfo);
        }
        AuthnRequest authnRequest = null;
        if (smap != null) {
            authnRequest = (AuthnRequest) 
                smap.get(SAML2Constants.AUTHN_REQUEST);
        }
        if (inRespToResp != null && inRespToResp.length() != 0) {
            SPCache.requestHash.remove(inRespToResp);
        }
        Map sessionInfoMap = new HashMap();
        sessionInfoMap.put(SessionProvider.REALM, realm);
        sessionInfoMap.put(SessionProvider.PRINCIPAL_NAME, userName);
        // set client info. always use client IP address to prevent
        // reverse host lookup
        String clientAddr = ClientUtils.getClientIPAddress(request);
        sessionInfoMap.put(SessionProvider.HOST, clientAddr);
        sessionInfoMap.put(SessionProvider.HOST_NAME, clientAddr);
        sessionInfoMap.put(SessionProvider.AUTH_LEVEL, 
            String.valueOf(authLevel));
        request.setAttribute(SessionProvider.ATTR_MAP, attrMap);
        try {
            session = sessionProvider.createSession(
                sessionInfoMap, request, response, null);
        } catch (SessionException se) {
            // invoke SPAdapter for failure
            int failureCode = 
                SAML2ServiceProviderAdapter.SSO_FAILED_SESSION_GENERATION;
            int sessCode =  se.getErrCode();
            if (sessCode == SessionException.AUTH_USER_INACTIVE) {
                failureCode =
                    SAML2ServiceProviderAdapter.SSO_FAILED_AUTH_USER_INACTIVE;
            } else if (sessCode == SessionException.AUTH_USER_LOCKED) {
                failureCode =
                    SAML2ServiceProviderAdapter.SSO_FAILED_AUTH_USER_LOCKED;
            } else if (sessCode == SessionException.AUTH_ACCOUNT_EXPIRED) {
                failureCode =
                    SAML2ServiceProviderAdapter.SSO_FAILED_AUTH_ACCOUNT_EXPIRED;
            }
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "SPACSUtils.processResponse : error code=" + sessCode, se);
            }
            SAML2Exception se2 = new SAML2Exception(se);
            invokeSPAdapterForSSOFailure(hostEntityId, realm,
                request, response, smap, respInfo, failureCode, se2);
            throw se2;
        }

        // set metaAlias
        String[] values = { metaAlias };
        try {
            setAttrMapInSession(sessionProvider, attrMap, session);
            setDiscoBootstrapCredsInSSOToken(sessionProvider, authnAssertion,
                session);
            sessionProvider.setProperty(
                session, SAML2Constants.SP_METAALIAS, values);
        } catch (SessionException se) {
            // invoke SPAdapter for failure
            SAML2Exception se2 = new SAML2Exception(se);
            invokeSPAdapterForSSOFailure(hostEntityId, realm,
                request, response, smap, respInfo, 
                SAML2ServiceProviderAdapter.SSO_FAILED_SESSION_ERROR, se2);
            throw se2;
        }

        NameIDInfo info = null;
        String affiID = nameId.getSPNameQualifier();
        boolean isDualRole = SAML2Utils.isDualRole(hostEntityId, realm);
        AffiliationDescriptorType affiDesc = null;
        if (affiID != null && !affiID.isEmpty()) {
            affiDesc = metaManager.getAffiliationDescriptor(realm, affiID);
        }

        if (affiDesc != null) {
            if (!affiDesc.getAffiliateMember().contains(hostEntityId)) {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                    "spNotAffiliationMember"));
            }
            if (isDualRole) {
                info = new NameIDInfo(affiID, remoteHostId, nameId,
                    SAML2Constants.DUAL_ROLE, true);
            } else {
                info = new NameIDInfo(affiID, remoteHostId, nameId,
                    SAML2Constants.SP_ROLE, true);
            }
        } else {
            if (isDualRole) {
                info = new NameIDInfo(hostEntityId, remoteHostId, nameId,
                    SAML2Constants.DUAL_ROLE, false);
            } else {
                info = new NameIDInfo(hostEntityId, remoteHostId, nameId,
                    SAML2Constants.SP_ROLE, false);
            }
        }
        Map props = new HashMap();
        String nameIDValueString = info.getNameIDValue();
        props.put(LogUtil.NAME_ID, info.getNameIDValue());
        try {
            userName = sessionProvider.getPrincipalName(session);
        } catch (SessionException se) {
            // invoke SPAdapter for failure
            SAML2Exception se2 = new SAML2Exception(se);
            invokeSPAdapterForSSOFailure(hostEntityId, realm,
                request, response, smap, respInfo, 
                SAML2ServiceProviderAdapter.SSO_FAILED_SESSION_ERROR, se2);
            throw se2;
        }
        String[] data1 = {userName, nameIDValueString};
        LogUtil.access(Level.INFO, LogUtil.SUCCESS_FED_SSO, data1, session,
            props);
        // write fed info into data store
        if (writeFedInfo) {
            try {
                AccountUtils.setAccountFederation(info, userName);
            } catch (SAML2Exception se) {
                // invoke SPAdapter for failure
                invokeSPAdapterForSSOFailure(hostEntityId, realm,
                    request, response, smap, respInfo, 
                    SAML2ServiceProviderAdapter.FEDERATION_FAILED_WRITING_ACCOUNT_INFO, se);
               throw se;
            }
            String[] data = {userName, ""};
            if (LogUtil.isAccessLoggable(Level.FINE)) {
                data[1] = info.toValueString();
            }
            LogUtil.access(Level.INFO,
                           LogUtil.FED_INFO_WRITTEN,
                           data,
                           session,
                           props);
        }
        String requestID = respInfo.getResponse().getInResponseTo();
        // save info in memory for logout
        saveInfoInMemory(sessionProvider, session, sessionIndex, metaAlias,
            info, IDPProxyUtil.isIDPProxyEnabled(requestID), isTransient);

        // invoke SP Adapter
        SAML2ServiceProviderAdapter spAdapter =
            SAML2Utils.getSPAdapterClass(hostEntityId, realm);
        if (spAdapter != null) {
            boolean redirected = spAdapter.postSingleSignOnSuccess(
                hostEntityId, realm, request, 
                response, out, session, authnRequest, respInfo.getResponse(),
                respInfo.getProfileBinding(), writeFedInfo);
            String[] value = null;
            if (redirected) {
                value = new String[] {"true"};
            } else {
                value = new String[] {"false"};
            }
            try {
                sessionProvider.setProperty(session, 
                    SAML2Constants.RESPONSE_REDIRECTED, value);
            } catch (SessionException ex) {
                SAML2Utils.debug.warning("SPSingleLogout.processResp", ex);
            } catch (UnsupportedOperationException ex) {
                SAML2Utils.debug.warning("SPSingleLogout.processResp", ex);
            }
        }
     
        String assertionID=authnAssertion.getID();
        if (respInfo.getProfileBinding().equals(SAML2Constants.HTTP_POST)) {
            SPCache.assertionByIDCache.put(assertionID, SAML2Constants.ONETIME);
            try {
                if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                    SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(
                            assertionID,
                            SAML2Constants.ONETIME,
                            ((Long) smap.get(SAML2Constants.NOTONORAFTER)).longValue() / 1000);
                }
            } catch (SAML2TokenRepositoryException se) {
                SAML2Utils.debug.error(classMethod +
                        "There was a problem saving the assertionID to the SAML2 Token Repository for assertionID:"
                        + assertionID, se);
            }
        }
        respInfo.setAssertion(authnAssertion);
 
        return session;
    }

    private static boolean getNeedNameIDEncrypted(boolean needAssertionEncrypted, SPSSOConfigElement spssoconfig) {
        if (!needAssertionEncrypted) {
            return Boolean.parseBoolean(SAML2Utils.getAttributeValueFromSPSSOConfig(spssoconfig,
                    SAML2Constants.WANT_NAMEID_ENCRYPTED));
        }

        return false;
    }

    public static boolean getNeedAttributeEncrypted(boolean needAssertionEncrypted, SPSSOConfigElement spssoconfig) {
        if (!needAssertionEncrypted) {
            return Boolean.parseBoolean(SAML2Utils.getAttributeValueFromSPSSOConfig(spssoconfig,
                    SAML2Constants.WANT_ATTRIBUTE_ENCRYPTED));
        }

        return false;
    }

    private static void invokeSPAdapterForSSOFailure(String hostEntityId,
        String realm, HttpServletRequest request, HttpServletResponse response,
        Map smap, ResponseInfo respInfo, int errorCode, 
        SAML2Exception se) { 
        SAML2ServiceProviderAdapter spAdapter = null;
        try {
            spAdapter = SAML2Utils.getSPAdapterClass(hostEntityId, realm);
        } catch (SAML2Exception e) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "SPACSUtils.invokeSPAdapterForSSOFailure", e);
            }
        }
        if (spAdapter != null) {
            AuthnRequest authnRequest = null;
            if (smap != null) {
                authnRequest = (AuthnRequest) 
                    smap.get(SAML2Constants.AUTHN_REQUEST);
            }
            boolean redirected = spAdapter.postSingleSignOnFailure(
                hostEntityId, realm, request, response, authnRequest,
                respInfo.getResponse(), respInfo.getProfileBinding(),
                errorCode);
            se.setRedirectionDone(redirected);
        }
    }

    public static void saveInfoInMemory(SessionProvider sessionProvider,
        Object session, String sessionIndex, String metaAlias,
        NameIDInfo info, boolean isIDPProxy, boolean isTransient)
        throws SAML2Exception {
        
        String infoKeyString = (new NameIDInfoKey(
            info.getNameIDValue(),
            info.getHostEntityID(),
            info.getRemoteEntityID())).toValueString();
        String infoKeyAttribute =
            AccountUtils.getNameIDInfoKeyAttribute();
        String[] fromToken = null;
        try {
            fromToken = sessionProvider.
                    getProperty(session, infoKeyAttribute);
            if (fromToken == null || fromToken.length == 0 ||
                fromToken[0] == null || fromToken[0].length() == 0) {
                String[] values = { infoKeyString };
                sessionProvider.setProperty(
                    session, infoKeyAttribute, values);
            } else {
                if (fromToken[0].indexOf(infoKeyString) == -1) {
                    String[] values = { fromToken[0] +
                                        SAML2Constants.SECOND_DELIM +
                                        infoKeyString }; 
                    sessionProvider.setProperty(
                        session, infoKeyAttribute, values);
                }
            }
            if (isTransient) {
                String nameIDInfoStr = info.toValueString();
                String infoAttribute = AccountUtils.getNameIDInfoAttribute();
                String[] nameIDInfoStrs = sessionProvider.getProperty(session,
                    infoAttribute);
                if (nameIDInfoStrs == null) {
                    nameIDInfoStrs = new String[1];
                    nameIDInfoStrs[0] = nameIDInfoStr;
                } else {
                    Set nameIDInfoStrSet = new HashSet();
                    for(int i=0; i<nameIDInfoStrs.length; i++) {
                        nameIDInfoStrSet.add(nameIDInfoStrs[i]);
                    }
                    nameIDInfoStrSet.add(nameIDInfoStr);
                    nameIDInfoStrs = (String[])nameIDInfoStrSet.toArray(
                        new String[nameIDInfoStrSet.size()]);
                }
                sessionProvider.setProperty(session, infoAttribute,
                    nameIDInfoStrs);
            }
        } catch (SessionException sessE) {
            throw new SAML2Exception(sessE);
        }
        String tokenID = sessionProvider.getSessionID(session);
        if (!SPCache.isFedlet) {
            List fedSessions = (List)
                SPCache.fedSessionListsByNameIDInfoKey.get(infoKeyString);
            if (fedSessions == null) {
                synchronized (SPCache.fedSessionListsByNameIDInfoKey) {
                    fedSessions = (List)
                    SPCache.fedSessionListsByNameIDInfoKey.get(infoKeyString);
                    if (fedSessions == null) {
                        fedSessions = new ArrayList();
                    }
                }  
                synchronized (fedSessions) {
                    fedSessions.add(new SPFedSession(sessionIndex, tokenID,
                        info, metaAlias));
                    SPCache.fedSessionListsByNameIDInfoKey.put(
                        infoKeyString, fedSessions);
                }
                if ((agent != null) && agent.isRunning() && (saml2Svc != null)){
                    saml2Svc.setFedSessionCount(
		        (long)SPCache.fedSessionListsByNameIDInfoKey.size());
                }

                if (isIDPProxy) {
                    //IDP Proxy 
                    IDPSession idpSess = (IDPSession)
                        IDPCache.idpSessionsBySessionID.get(
                        tokenID);
                    if (idpSess == null) {
                        idpSess = new IDPSession(session);
                        IDPCache.idpSessionsBySessionID.put(
                            tokenID, idpSess);
                    }
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message("Add Session Partner: " +
                            info.getRemoteEntityID());
                    } 
                    idpSess.addSessionPartner(new SAML2SessionPartner(
                        info.getRemoteEntityID(), true));
                    // end of IDP Proxy        
                }
            } else {
                synchronized (fedSessions) {
                    Iterator iter = fedSessions.iterator();
                    boolean found = false;
                    while (iter.hasNext()) {
                        SPFedSession temp = (SPFedSession) iter.next();
                        String idpSessionIndex = null;
                        if(temp != null) {
                           idpSessionIndex = temp.idpSessionIndex; 
                        }
                        if ((idpSessionIndex != null) &&
                                (idpSessionIndex.equals(sessionIndex))) {
                            temp.spTokenID = tokenID;
                            temp.info = info;
                            found = true;
                            break;
                        }
                    }    
                    if (!found) {
                        fedSessions.add(
                            new SPFedSession(sessionIndex, tokenID, info,
                                             metaAlias));
                        SPCache.fedSessionListsByNameIDInfoKey.put(
                            infoKeyString, fedSessions);
                        if ((agent != null) &&
                            agent.isRunning() &&
                            (saml2Svc != null))
                        {
                            saml2Svc.setFedSessionCount(
		                (long)SPCache.fedSessionListsByNameIDInfoKey.
				    size());
                        }
                    }
               }    
            }
            SPCache.fedSessionListsByNameIDInfoKey.put(infoKeyString,
                    fedSessions);
            if ((agent != null) && agent.isRunning() && (saml2Svc != null)) {
                saml2Svc.setFedSessionCount(
		    (long)SPCache.fedSessionListsByNameIDInfoKey.size());
            }
        }
        try {
            sessionProvider.addListener(
                session, new SPSessionListener(infoKeyString, tokenID));
        } catch (SessionException e) {
            SAML2Utils.debug.error(
                "SPACSUtils.saveInfoInMemory: "+
                "Unable to add session listener.");
        }
    }
    
    /** Sets the attribute map in the session
     *
     *  @param sessionProvider Session provider
     *  @param attrMap the Attribute Map
     *  @param session the valid session object
     *  @throws com.sun.identity.plugin.session.SessionException 
     */
    public static void setAttrMapInSession(
        SessionProvider sessionProvider,
        Map attrMap, Object session)
        throws SessionException {
        if (attrMap != null && !attrMap.isEmpty()) {
            Set entrySet = attrMap.entrySet();
            for(Iterator iter = entrySet.iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry)iter.next();
                String attrName = (String)entry.getKey();
                Set attrValues = (Set)entry.getValue();
                if(attrValues != null && !attrValues.isEmpty()) {
                   sessionProvider.setProperty(
                       session, attrName,
                       (String[]) attrValues.toArray(
                       new String[attrValues.size()]));
                   if (SAML2Utils.debug.messageEnabled()) {
                       SAML2Utils.debug.message(
                           "SPACSUtils.setAttrMapInSessioin: AttrMap:" +
                           attrName + " , " + attrValues);
                   }
                }
            }
        }
    }

    /** Sets Discovery bootstrap credentials in the SSOToken
     *
     *  @param sessionProvider session provider.
     *  @param assertion assertion.
     *  @param session the valid session object.
     */
    private static void setDiscoBootstrapCredsInSSOToken(
        SessionProvider sessionProvider, Assertion assertion, Object session)
        throws SessionException {

        if (assertion == null) {
            return;
        }

        Set discoBootstrapCreds = null;
        Advice advice = assertion.getAdvice();
        if (advice != null) {
            List creds = advice.getAdditionalInfo();
            if ((creds != null) && !creds.isEmpty()) {
                if (discoBootstrapCreds == null) {
                    discoBootstrapCreds = new HashSet();
                }
                discoBootstrapCreds.addAll(creds);
            }
        }

        if (discoBootstrapCreds != null) {
            sessionProvider.setProperty(session,
                SAML2Constants.DISCOVERY_BOOTSTRAP_CREDENTIALS,
                (String[])discoBootstrapCreds.toArray(
                new String[discoBootstrapCreds.size()]));
        }
    }

    /**
     * Obtains relay state. Retrieves the relay state from relay state cache.
     * If input relay state is null, retrieve it from <code>SPSSOConfig</code>.
     *
     * @param relayStateID relay state value received from http request.
     * @param orgName realm or organization name the service provider resides in
     * @param hostEntityId Entity ID of the hosted service provider
     * @param sm <code>SAML2MetaManager</code> instance.
     * @return final relay state. Or <code>null</code> if the input 
     *         relayStateID is null and no default relay state is configured.
     */
    public static String getRelayState(
        String relayStateID,
        String orgName,
        String hostEntityId,
        SAML2MetaManager sm
    ) {
        String relayStateUrl = null;

        if ((relayStateID != null) && (relayStateID.trim().length() != 0)) {
            CacheObject cache = (CacheObject)SPCache.relayStateHash.remove(
                relayStateID);

            if (cache != null) {
                relayStateUrl = (String)cache.getObject();
            } else if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                // The key is this way to make it unique compared to when
                // the same key is used to store a copy of the AuthnRequestInfo
                String key = relayStateID + relayStateID;
                try {
                    // Try and retrieve the value from the SAML2 repository
                    String relayState = (String) SAML2FailoverUtils.retrieveSAML2Token(key);
                    if (relayState != null) {
                        // Get back the relayState
                        relayStateUrl = relayState;
                        if (SAML2Utils.debug.messageEnabled()) {
                            SAML2Utils.debug.message("SPACUtils.getRelayState: relayState"
                                + " retrieved from SAML2 repository for key: " + key);
                        }
                    }
                } catch (SAML2TokenRepositoryException se) {
                    SAML2Utils.debug.error("SPACUtils.getRelayState: Unable to retrieve relayState for key "
                            + key, se);
                }
            } else {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("SPACUtils.getRelayState: relayState"
                        + " is null for relayStateID: " + relayStateID + ", SAML2 failover is disabled");
                }
            }
            
            if (relayStateUrl == null || relayStateUrl.trim().length() == 0) {
                relayStateUrl = relayStateID;
            }
        }
        
        if (relayStateUrl == null || relayStateUrl.trim().length() == 0) {
            relayStateUrl = getAttributeValueFromSPSSOConfig(
                orgName, hostEntityId, sm, SAML2Constants.DEFAULT_RELAY_STATE);
        }
        
        return relayStateUrl;
    }

    /**
     * Retrieves intermediate redirect url from SP sso config. This url is used
     * if you want to goto some place before the final relay state.
     *
     * @param orgName realm or organization name the service provider resides in
     * @param hostEntityId Entity ID of the hosted service provider
     * @param sm <code>SAML2MetaManager</code> instance.
     * @return intermediate redirect url; or <code>null</code> if the url is
     *                is not configured or an error occured during the retrieval
     *                process.
     */
    public static String getIntermediateURL(String orgName,
                                        String hostEntityId,
                                        SAML2MetaManager sm)
    {
        return getAttributeValueFromSPSSOConfig(orgName, hostEntityId, sm,
                                        SAML2Constants.INTERMEDIATE_URL);
    }

    /**
     * Saves response for later retrieval and retrieves local auth url from
     * <code>SPSSOConfig</code>.
     * If the url does not exist, generate one from request URI.
     * If still cannot get it, (shouldn't happen), get it from
     * <code>AMConfig.properties</code>.
     *
     * @param orgName realm or organization name the service provider resides in
     * @param hostEntityId Entity ID of the hosted service provider
     * @param sm <code>SAML2MetaManager</code> instance to perform meta
     *                operation.
     * @param respInfo to be cached <code>ResponseInfo</code>.
     * @param requestURI http request URI.
     * @return local login url.
     */
    public static String prepareForLocalLogin(
                                        String orgName,
                                        String hostEntityId,
                                        SAML2MetaManager sm,
                                        ResponseInfo respInfo,
                                        String requestURI)
    {
        String localLoginUrl = getAttributeValueFromSPSSOConfig(
                orgName, hostEntityId, sm, SAML2Constants.LOCAL_AUTH_URL);
        if ((localLoginUrl == null) || (localLoginUrl.length() == 0)) {
            // get it from request
            try {
                int index = requestURI.indexOf("Consumer/metaAlias");
                if (index != -1) {
                    localLoginUrl = requestURI.substring(0, index)
                        + "UI/Login?org="
                        + orgName;
                }
            } catch (IndexOutOfBoundsException e) {
                localLoginUrl = null;
            }
            if ((localLoginUrl == null) || (localLoginUrl.length() == 0)) {
                // shouldn't be here, but in case
                localLoginUrl =
                        SystemConfigurationUtil.getProperty(SAMLConstants.SERVER_PROTOCOL)
                        + "://"
                        + SystemConfigurationUtil.getProperty(SAMLConstants.SERVER_HOST)
                        + SystemConfigurationUtil.getProperty(SAMLConstants.SERVER_PORT)
                        + "/UI/Login?org="
                        + orgName;
            }
        }

        respInfo.setIsLocalLogin(true);
        synchronized (SPCache.responseHash) {
           SPCache.responseHash.put(respInfo.getResponse().getID(), 
               respInfo);
        }   
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("SPACSUtils:prepareForLocalLogin: " +
                "localLoginUrl = " + localLoginUrl);
        }
        return localLoginUrl;
    }

    /**
     * Retrieves attribute value for a given attribute name from 
     * <code>SPSSOConfig</code>.
     * @param orgName realm or organization name the service provider resides in
     * @param hostEntityId hosted service provider's Entity ID.
     * @param sm <code>SAML2MetaManager</code> instance to perform meta
     *                operations.
     * @param attrName name of the attribute whose value ot be retrived.
     * @return value of the attribute; or <code>null</code> if the attribute
     *                if not configured, or an error occured in the process.
     */ 
    private static String getAttributeValueFromSPSSOConfig(String orgName,
                                                        String hostEntityId,
                                                        SAML2MetaManager sm,
                                                        String attrName)
    {
        String result = null;
        try {
            SPSSOConfigElement config = sm.getSPSSOConfig(orgName,
                                                        hostEntityId);
            if (config == null) {
                return null;
            }
            Map attrs = SAML2MetaUtils.getAttributes(config);
            List value = (List) attrs.get(attrName);
            if (value != null && value.size() != 0) {
                result = ((String) value.iterator().next()).trim();
            }
        } catch (SAML2MetaException sme) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("SPACSUtils.getAttributeValueFromSPSSO"
                        + "Config:", sme);
            }
            result = null;
        }
        return result;
    }

    /**
     * Gets the attributes from an assert's AttributeStates.
     *
     * @param assertion The assertion from which to pull the AttributeStates.
     * @param needAttributeEncrypted Whether attributes must be encrypted (or else rejected).
     * @param privateKeys Private keys used to decrypt those encrypted attributes.
     * @return a list of attributes pulled from the provided assertion.
     */
    public static List<Attribute> getSAMLAttributes(Assertion assertion, boolean needAttributeEncrypted,
                                                     Set<PrivateKey> privateKeys) {
        List<Attribute> attrList = null;
        if (assertion != null) {
            List<AttributeStatement> statements = assertion.getAttributeStatements();
            if (CollectionUtils.isNotEmpty(statements)) {
                for (AttributeStatement statement : statements) {
                    List<Attribute> attributes = statement.getAttribute();
                    if (needAttributeEncrypted && attributes != null && !attributes.isEmpty()) {
                        SAML2Utils.debug.error("Attribute not encrypted.");
                        return null;
                    }
                    if (attributes != null) {
                        if (attrList == null) {
                            attrList = new ArrayList<>();
                        }
                        attrList.addAll(attributes);
                    }
                    List<EncryptedAttribute> encAttrs = statement.getEncryptedAttribute();
                    if (encAttrs != null) {
                        for (EncryptedAttribute encAttr : encAttrs) {
                            if (attrList == null) {
                                attrList = new ArrayList<>();
                            }
                            try {
                                attrList.add((encAttr).decrypt(privateKeys));
                            } catch (SAML2Exception se) {
                                SAML2Utils.debug.error("Decryption error:", se);
                                return null;
                            }
                        }
                    }
                }
            }
        }
        return attrList;
    }

    /**
     * Processes response from Identity Provider to Fedlet (SP).
     * This will do all required protocol processing, include signature,
     * issuer and audience validation etc. A map containing processing
     * result will be returned. <br>
     * Here is a list of keys and values for the returned map: <br>
     * SAML2Constants.ATTRIBUTE_MAP -- Attribute map containing all attributes
     *                                 passed down from IDP inside the 
     *                                 Assertion. The value is a 
     *                                 <code>java.util.Map</code> whose keys 
     *                                 are attribute names and values are 
     *                                 <code>java.util.Set</code> of string 
     *                                 values for the attributes. <br>
     * SAML2Constants.RELAY_STATE -- Relay state, value is a string <br>
     * SAML2Constants.IDPENTITYID -- IDP entity ID, value is a string<br>
     * SAML2Constants.RESPONSE    -- Response object, value is an instance of 
     *                               com.sun.identity.saml2.protocol.Response
     * SAML2Constants.ASSERTION   -- Assertion object, value is an instance of 
     *                               com.sun.identity.saml2.assertion.Assertion
     * SAML2Constants.SUBJECT     -- Subject object, value is an instance of 
     *                               com.sun.identity.saml2.assertion.Subject
     * SAML2Constants.NAMEID      -- NameID object, value is an instance of 
     *                               com.sun.identity.saml2.assertion.NameID
     *
     * @param request HTTP Servlet request
     * @param response HTTP Servlet response.
     * @param out the print writer for writing out presentation
     *
     * @return <code>Map</code> which holds result of the processing.
     * @throws SAML2Exception if the processing failed due to server error.
     * @throws IOException if the processing failed due to IO error.
     * @throws SessionException if the processing failed due to session error.
     * @throws ServletException if the processing failed due to request error.
     *
     * @supported.api
     */  
    public static Map processResponseForFedlet (HttpServletRequest request,
        HttpServletResponse response, PrintWriter out) throws SAML2Exception, IOException,
        SessionException, ServletException {
        if (request == null) {
            String message =
                    MessageFormat.format(SAML2SDKUtils.bundle.getString("nullInputMessage"), new String[]{"request"});
            SAML2SDKUtils.debug.error("SPACSUtils.processResponseForFedlet: " + message);
            throw new ServletException(message);
        }
        if (response == null) {
            String message =
                    MessageFormat.format(SAML2SDKUtils.bundle.getString("nullInputMessage"), new String[]{"response"});
            SAML2SDKUtils.debug.error("SPACSUtils.processResponseForFedlet: " + message);
            throw new ServletException(message);
        }
        
        String requestURL = request.getRequestURL().toString();
        SAML2MetaManager metaManager = new SAML2MetaManager();
        if (metaManager == null) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("errorMetaManager"));
        }
        String metaAlias = SAML2MetaUtils.getMetaAliasByUri(requestURL);
        if ((metaAlias ==  null) || (metaAlias.length() == 0)) {
            // Check in case metaAlias has been supplied as a parameter
            metaAlias = request.getParameter(SAML2MetaManager.NAME_META_ALIAS_IN_URI);
            if (metaAlias == null || metaAlias.length() == 0) {
                // pick the first available one
                List spMetaAliases =
                        metaManager.getAllHostedServiceProviderMetaAliases("/");
                if ((spMetaAliases != null) && !spMetaAliases.isEmpty()) {
                    // get first one
                    metaAlias = (String) spMetaAliases.get(0);
                }
                if ((metaAlias ==  null) || (metaAlias.length() == 0)) {
                    throw new ServletException(
                            SAML2SDKUtils.bundle.getString("nullSPEntityID"));
                }
            }
        }
        String hostEntityId = null;
        try {
            hostEntityId = metaManager.getEntityByMetaAlias(metaAlias);
        } catch (SAML2MetaException sme) {
            SAML2SDKUtils.debug.error("SPACSUtils.processResponseForFedlet",
                sme);
            throw new SAML2Exception( 
                    SAML2SDKUtils.bundle.getString("metaDataError"));
        }
        if (hostEntityId == null) {
            // logging?
            throw new SAML2Exception( 
                    SAML2SDKUtils.bundle.getString("metaDataError"));
        }
        // organization is always root org
        String orgName = "/";
        String relayState = request.getParameter(SAML2Constants.RELAY_STATE);
        SessionProvider sessionProvider = null;
        ResponseInfo respInfo = null;
        try {
            sessionProvider = SessionManager.getProvider();
        } catch (SessionException se) {
            SAML2SDKUtils.debug.error("SPACSUtils.processResponseForFedlet",
                se);
            throw new SAML2Exception(se);
        }
        respInfo = SPACSUtils.getResponse(
                request, response, orgName, hostEntityId, metaManager);
        
        Object newSession = null;

        // Throws a SAML2Exception if the response cannot be validated
        // or contains a non-Success StatusCode, invoking the SPAdapter SPI
        // for taking action on the failed validation.
        // The resulting exception has its redirectionDone flag set if
        // the SPAdapter issued a HTTP redirect.
        newSession = SPACSUtils.processResponse(
                    request, response, out, metaAlias, null, respInfo,
                    orgName, hostEntityId, metaManager, null);
        
        SAML2SDKUtils.debug.message("SSO SUCCESS");
        String[] redirected = sessionProvider.getProperty(newSession,
                SAML2Constants.RESPONSE_REDIRECTED);
        if ((redirected != null) && (redirected.length != 0) &&
                redirected[0].equals("true")) {
            SAML2SDKUtils.debug.message("Already redirected in SPAdapter.");
            // response redirected already in SPAdapter
            return createMapForFedlet(respInfo, null, hostEntityId);
        }
        // redirect to relay state
        String finalUrl = SPACSUtils.getRelayState(
                relayState, orgName, hostEntityId, metaManager);
        String realFinalUrl = finalUrl;
        if (finalUrl != null && finalUrl.length() != 0) {
            try {
                realFinalUrl =
                    sessionProvider.rewriteURL(newSession, finalUrl);
            } catch (SessionException se) {
                SAML2SDKUtils.debug.message("SPACSUtils.processRespForFedlet",
                    se);
                realFinalUrl = finalUrl;
            }
        }
        String redirectUrl = SPACSUtils.getIntermediateURL(
                orgName, hostEntityId, metaManager);
        String realRedirectUrl = null;
        if (redirectUrl != null && redirectUrl.length() != 0) {
            if (realFinalUrl != null && realFinalUrl.length() != 0) {
                if (redirectUrl.indexOf("?") != -1) {
                    redirectUrl += "&goto=";
                } else {
                    redirectUrl += "?goto=";
                }
                redirectUrl += URLEncDec.encode(realFinalUrl);
                try {
                    realRedirectUrl = sessionProvider.rewriteURL(
                            newSession, redirectUrl);
                } catch (SessionException se) {
                    SAML2SDKUtils.debug.message(
                      "SPACSUtils.processRespForFedlet: rewriting failed.", se);
                    realRedirectUrl = redirectUrl;
                }
            } else {
                realRedirectUrl = redirectUrl;
            }
        } else {
            realRedirectUrl = finalUrl;
        }
        return createMapForFedlet(respInfo, realRedirectUrl, hostEntityId); 
    }

    private static Map createMapForFedlet(
        ResponseInfo respInfo, String relayUrl, String hostedEntityId) {
        Map map = new HashMap();
        if (relayUrl != null) {
            map.put(SAML2Constants.RELAY_STATE, relayUrl);
        }
        Response samlResp = respInfo.getResponse();
        map.put(SAML2Constants.RESPONSE, samlResp);
        Assertion assertion = respInfo.getAssertion();
        map.put(SAML2Constants.ASSERTION, assertion); 
        map.put(SAML2Constants.SUBJECT, assertion.getSubject());
        map.put(SAML2Constants.IDPENTITYID, assertion.getIssuer().getValue()); 
        map.put(SAML2Constants.SPENTITYID, hostedEntityId);
        map.put(SAML2Constants.NAMEID, respInfo.getNameId());
        map.put(SAML2Constants.ATTRIBUTE_MAP, respInfo.getAttributeMap());
        map.put(SAML2Constants.SESSION_INDEX, respInfo.getSessionIndex());
        return map;
    }

    /**
     * Returns the username if there was one from the Assertion we were able to map into a local user account. Returns
     * null if not. Should only be used from the SP side. Should only be called in conjuncture with the Auth Module.
     * In addition, it performs what attribute federation it can.
     *
     * This method is a picked apart version of the "processResponse" function.
     */
    public static String getPrincipalWithoutLogin(Subject assertionSubject, Assertion authnAssertion, String realm,
                                                  String spEntityId, SAML2MetaManager metaManager, String idpEntityId,
                                                  String storageKey)
            throws SAML2Exception {

        final EncryptedID encId = assertionSubject.getEncryptedID();
        final SPSSOConfigElement spssoconfig = metaManager.getSPSSOConfig(realm, spEntityId);
        final Set<PrivateKey> decryptionKeys = KeyUtil.getDecryptionKeys(spssoconfig);
        final SPAccountMapper acctMapper = SAML2Utils.getSPAccountMapper(realm, spEntityId);

        boolean needNameIDEncrypted = false;
        NameID nameId = assertionSubject.getNameID();

        String assertionEncryptedAttr =
                SAML2Utils.getAttributeValueFromSPSSOConfig(spssoconfig, SAML2Constants.WANT_ASSERTION_ENCRYPTED);
        if (!Boolean.parseBoolean(assertionEncryptedAttr)) {
            String idEncryptedStr =
                    SAML2Utils.getAttributeValueFromSPSSOConfig(spssoconfig, SAML2Constants.WANT_NAMEID_ENCRYPTED);
            needNameIDEncrypted = Boolean.parseBoolean(idEncryptedStr);
        }

        if (needNameIDEncrypted && encId == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString("nameIDNotEncrypted"));
        }
        if (encId != null) {
            nameId = encId.decrypt(decryptionKeys);
        }

        SPSSODescriptorElement spDesc = null;
        try {
            spDesc = metaManager.getSPSSODescriptor(realm, spEntityId);
        } catch (SAML2MetaException ex) {
            SAML2Utils.debug.error("Unable to read SPSSODescription", ex);
        }

        if (spDesc == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
        }

        final String nameIDFormat = nameId.getFormat();

        if (nameIDFormat != null) {
            List spNameIDFormatList = spDesc.getNameIDFormat();

            if (CollectionUtils.isNotEmpty(spNameIDFormatList) && !spNameIDFormatList.contains(nameIDFormat)) {
                Object[] args = {nameIDFormat};
                throw new SAML2Exception(SAML2Utils.BUNDLE_NAME, "unsupportedNameIDFormatSP", args);
            }
        }

        final boolean isTransient = SAML2Constants.NAMEID_TRANSIENT_FORMAT.equals(nameIDFormat);
        final boolean ignoreProfile = SAML2PluginsUtils.isIgnoredProfile(realm);

        final boolean shouldPersistNameID = !isTransient && !ignoreProfile
                && acctMapper.shouldPersistNameIDFormat(realm, spEntityId, idpEntityId, nameIDFormat);

        String userName = null;
        boolean isNewAccountLink = false;

        try {
            if (shouldPersistNameID) {
                try {
                    userName = SAML2Utils.getDataStoreProvider().getUserID(realm, SAML2Utils.getNameIDKeyMap(
                            nameId, spEntityId, idpEntityId, realm, SAML2Constants.SP_ROLE));
                } catch (DataStoreProviderException dse) {
                    throw new SAML2Exception(dse.getMessage());
                }
            }

            //if we can't get an already linked account, see if we'll be generating a new one based on federated data
            if (userName == null) {
                userName = acctMapper.getIdentity(authnAssertion, spEntityId, realm);
                isNewAccountLink = true; //we'll use this later to inform us
            }
        } catch (SAML2Exception se) {
            return null;
        }

        //if we're new and we're persistent, store the federation data in the user pref
        if (isNewAccountLink && shouldPersistNameID) {
            try {
                writeFedData(nameId, spEntityId, realm, metaManager, idpEntityId, userName, storageKey);
            } catch (SAML2Exception se) {
                return userName;
            }
        }

        return userName;
    }

    private static void writeFedData(NameID nameId, String spEntityId, String realm, SAML2MetaManager metaManager,
                                     String idpEntityId, String userName, String storageKey) throws SAML2Exception {
        final NameIDInfo info;
        final String affiID = nameId.getSPNameQualifier();
        boolean isDualRole = SAML2Utils.isDualRole(spEntityId, realm);
        AffiliationDescriptorType affiDesc = null;

        if (affiID != null && !affiID.isEmpty()) {
            affiDesc = metaManager.getAffiliationDescriptor(realm, affiID);
        }

        if (affiDesc != null) {
            if (!affiDesc.getAffiliateMember().contains(spEntityId)) {
                throw new SAML2Exception("Unable to locate SP Entity ID in the affiliate descriptor.");
            }
            if (isDualRole) {
                info = new NameIDInfo(affiID, idpEntityId, nameId, SAML2Constants.DUAL_ROLE, true);
            } else {
                info = new NameIDInfo(affiID, idpEntityId, nameId, SAML2Constants.SP_ROLE, true);
            }
        } else {
            if (isDualRole) {
                info = new NameIDInfo(spEntityId, idpEntityId, nameId, SAML2Constants.DUAL_ROLE, false);
            } else {
                info = new NameIDInfo(spEntityId, idpEntityId, nameId, SAML2Constants.SP_ROLE, false);
            }
        }

        // write fed info into data store
        SPCache.fedAccountHash.put(storageKey, "true");
        AccountUtils.setAccountFederation(info, userName);
    }

    /**
     * Gets the attributes for this assertion in a new List.
     * @param authnAssertion Assertion from which to reead the attributes.
     * @param needAttributeEncrypted Whether the attributes must be encrypted.
     * @param decryptionKeys The keys used to decrypt the attributes, if they're encrypted.
     * @return a List of the attributes in this assertion.
     */
    public static List<Attribute> getAttrs(Assertion authnAssertion, boolean needAttributeEncrypted,
                                           Set<PrivateKey> decryptionKeys) {
        final List<Attribute> origAttrs = getSAMLAttributes(authnAssertion, needAttributeEncrypted, decryptionKeys);

        List<Attribute> attrs = null;
        if (origAttrs != null && !origAttrs.isEmpty()) {
            attrs = new ArrayList<>();
            attrs.addAll(origAttrs);
        }

        return attrs;
    }

}
