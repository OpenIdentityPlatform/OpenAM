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
 * $Id: DiscoveryClient.java,v 1.5 2008/12/16 01:48:31 exu Exp $
 *
 */

package com.sun.identity.liberty.ws.disco;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.w3c.dom.Element;

import com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken;
import com.sun.identity.liberty.ws.soapbinding.Message;
import com.sun.identity.liberty.ws.soapbinding.ProviderHeader;
import com.sun.identity.liberty.ws.soapbinding.Client;
import com.sun.identity.liberty.ws.soapbinding.SOAPBindingConstants;
import com.sun.identity.liberty.ws.soapbinding.SOAPBindingException;
import com.sun.identity.liberty.ws.soapbinding.Utils;
import com.sun.identity.liberty.ws.disco.common.*;
import com.sun.identity.liberty.ws.security.*;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * The class <code>DiscoveryClient</code> provides methods to send  
 * Discovery Service query and modify.
 * Note: Current implementation uses <code>JAXB</code> objects and no wrapper
 * classes are used.
 * @supported.all.api
 */
public class DiscoveryClient {

    private String connectTo = null;
    private int clientMech = Message.ANONYMOUS;
    private ResourceID resID = null;
    private EncryptedResourceID encResID = null;
    private String certAlias = null;
    private String providerID = null;
    private boolean clientAuth = false;
    private SecurityAssertion assertion = null;
    private List assertions = null;
    private BinarySecurityToken token = null;
    private ResourceOffering offering = null;
    private boolean processed = true;
    private String soapAction = null;
    private Object session = null;
    private String wsfVersion = Utils.getDefaultWSFVersion();

    /**
     * Constructor, connects to Discovery Service without web service security
     * token.
     *
     * @param soapURI URI of the SOAP end point for this discovery 
     *                       service instance
     * @param providerID ID of the web service client.
     */
    public DiscoveryClient (String soapURI, String providerID) {
        connectTo = soapURI;
        this.providerID = providerID;
    }

    /**
     * Constructor, connects to Discovery Service  using <code>WSS</code> SAML
     * Token.
     *
     * @param assertion <code>WSS</code> SAML Token
     * @param soapURI URI of the SOAP end point for this discovery 
     *                       service instance
     * @param providerID ID of the web service client.
     */
    public DiscoveryClient (SecurityAssertion assertion,
                            String soapURI,
                            String providerID)
    {
        connectTo = soapURI;
        if ((assertion != null) && (assertion.isBearer())) {
            clientMech = Message.BEARER_TOKEN;
        } else {
            clientMech = Message.SAML_TOKEN;
        }
        this.assertion = assertion;
        this.providerID = providerID;
    }

    /**
     * Constructor, connects to Discovery Service using <code>WSS X509</code>
     * Token.
     * @param token <code>WSS X.509</code> Certificate Token
     * @param soapURI URI of the SOAP end point for this discovery 
     *        service instance.
     * @param providerID ID of the web service client.
     */
    public DiscoveryClient (BinarySecurityToken token,
                            String soapURI,
                            String providerID)
    {
        connectTo = soapURI;
        clientMech = Message.X509_TOKEN;
        this.token = token;
        this.providerID = providerID;
    }

    /**
     * Constructor, connects to Discovery Service specified by the resource
     * offering, security mechanism/SOAP endpoint defined in the 
     * <code>ResourceOffering</code> will be used.
     *
     * @param resourceOffering resource offering for this 
     *        discovery service instance 
     * @param session session of the  <code>WSC</code>
     * @param providerID ID of the web service client.
     */
    public DiscoveryClient(ResourceOffering resourceOffering,
                            Object session,
                            String providerID)
    {
        offering = resourceOffering;
        processed = false;
        this.session = session;
        this.providerID = providerID;
    }

    /**
     * Constructor, connects to Discovery Service specified by the resource
     * offering, security mechanism/SOAP endpoint defined in the 
     * <code>ResourceOffering</code> will be used.
     *
     * @param resourceOffering resource offering for this 
     *        discovery service instance 
     * @param session session of the  <code>WSC</code>
     * @param providerID ID of the web service client.
     * @param assertions List of assertions.
     */
    public DiscoveryClient(ResourceOffering resourceOffering,
                            Object session,
                            String providerID,
                            List assertions)
    {
        offering = resourceOffering;
        processed = false;
        this.session = session;
        this.providerID = providerID;
        this.assertions = assertions;
    }

    private void processResourceOffering() throws DiscoveryException {
        ServiceInstance instance = offering.getServiceInstance();
        if (!(instance.getServiceType().equals(DiscoConstants.DISCO_NS))) {
            DiscoSDKUtils.debug.error("DiscoveryClient.processResourceOffering: " +
            "ServiceType in ResourceOffering is not discovery service type.");
            throw new DiscoveryException(
                        DiscoSDKUtils.bundle.getString("notDiscoServiceType"));
        }
        resID = offering.getResourceID();
        encResID = offering.getEncryptedResourceID();
        List descriptions = instance.getDescription();
        /*
         * Iterate through supported security profiles until we find one
         * that we support (and we should always do so if the spec is
         * being complied with).  They should be in decreasing order of
         * preference...
         */
        // TODO: support wsdl form
        Iterator i = descriptions.iterator();
        while (i.hasNext()) {
            Description desc = (Description) i.next();
            connectTo = desc.getEndpoint();
            soapAction = desc.getSoapAction();
            Iterator j = desc.getSecurityMechID().iterator();
            while (j.hasNext()) {
                String mech = (String) j.next();
                if ((mech.equals(Message.NULL_NULL)) ||
                    (mech.equals(Message.TLS_NULL)) ||
                    (mech.equals(Message.CLIENT_TLS_NULL)))
                {
                    clientMech = Message.ANONYMOUS;
                    DiscoSDKUtils.debug.message("DiscoClient: null");
                    if (mech.equals(Message.CLIENT_TLS_NULL)) {
                        clientAuth = true;
                        DiscoSDKUtils.debug.message("DiscoClient: clientAuth on");
                    }
                    return;
                } else if ((mech.equals(Message.NULL_X509)) ||
                    (mech.equals(Message.TLS_X509)) ||
                    (mech.equals(Message.CLIENT_TLS_X509)) ||
                    (mech.equals(Message.NULL_X509_WSF11)) ||
                    (mech.equals(Message.TLS_X509_WSF11)) ||
                    (mech.equals(Message.CLIENT_TLS_X509_WSF11)))
                {
                    clientMech = Message.X509_TOKEN;
                    if (mech.equals(Message.NULL_X509) ||
                        mech.equals(Message.TLS_X509) ||
                        mech.equals(Message.CLIENT_TLS_X509)) {
                        wsfVersion = SOAPBindingConstants.WSF_10_VERSION;
                    } else {
                        wsfVersion = SOAPBindingConstants.WSF_11_VERSION;
                    }
                    DiscoSDKUtils.debug.message("DiscoClient: x509");
                    try {
                        SecurityTokenManagerClient stm =
                            new SecurityTokenManagerClient(session);
                        if (certAlias == null) {
                            certAlias = SystemPropertiesManager.get(
                                "com.sun.identity.liberty.ws.wsc.certalias");
                        }
                        stm.setCertAlias(certAlias);
                        token = stm.getX509CertificateToken();
                        token.setWSFVersion(wsfVersion);
                    } catch (Exception e) {
                        DiscoSDKUtils.debug.error("DiscoveryClient.processResource"
                            + "Offering: couldn't generate X509 token: ", e);
                        throw new DiscoveryException(e.getMessage());
                    }
                    if (mech.equals(Message.CLIENT_TLS_X509) ||
                        mech.equals(Message.CLIENT_TLS_X509_WSF11)) {
                        clientAuth = true;
                        DiscoSDKUtils.debug.message("DiscoClient: clientAuth on");
                    }
                    return;
                } else if ((mech.equals(Message.NULL_SAML)) ||
                    (mech.equals(Message.TLS_SAML)) ||
                    (mech.equals(Message.CLIENT_TLS_SAML)) ||
                    (mech.equals(Message.NULL_SAML_WSF11)) ||
                    (mech.equals(Message.TLS_SAML_WSF11)) ||
                    (mech.equals(Message.CLIENT_TLS_SAML_WSF11)))
                {
                    clientMech = Message.SAML_TOKEN;
                    if (mech.equals(Message.NULL_SAML) ||
                        mech.equals(Message.TLS_SAML) ||
                        mech.equals(Message.CLIENT_TLS_SAML)) {
                        wsfVersion = SOAPBindingConstants.WSF_10_VERSION;
                    } else {
                        wsfVersion = SOAPBindingConstants.WSF_11_VERSION;
                    }
                    DiscoSDKUtils.debug.message("DiscoClient: saml token");
                    List credRefs = desc.getCredentialRef();
                    if ((credRefs == null) || (credRefs.size() == 0)) {
                        throw new DiscoveryException(
                                DiscoSDKUtils.bundle.getString("noCredential"));
                    } else {
                        String credID = (String) credRefs.get(0);
                        if (assertions == null) {
                            throw new DiscoveryException(
                                DiscoSDKUtils.bundle.getString("noCredential"));
                        } else {
                            Iterator iter1 = assertions.iterator();
                            while (iter1.hasNext()) {
                                SecurityAssertion sassert = (SecurityAssertion)
                                                iter1.next();
                                if (credID.equals(sassert.getAssertionID())) {
                                    assertion = sassert;
                                    break;
                                }
                            }
                            if (assertion == null) {
                                throw new DiscoveryException(
                                DiscoSDKUtils.bundle.getString("noCredential"));
                            }
                        }
                    }
                    if (mech.equals(Message.CLIENT_TLS_SAML) ||
                        mech.equals(Message.CLIENT_TLS_SAML_WSF11)) {
                        clientAuth = true;
                        DiscoSDKUtils.debug.message("DiscoClient: clientAuth on");
                    }
                    return;
                } else if ((mech.equals(Message.NULL_BEARER)) ||
                    (mech.equals(Message.TLS_BEARER)) ||
                    (mech.equals(Message.CLIENT_TLS_BEARER)) ||
                    (mech.equals(Message.NULL_BEARER_WSF11)) ||
                    (mech.equals(Message.TLS_BEARER_WSF11)) ||
                    (mech.equals(Message.CLIENT_TLS_BEARER_WSF11)))
                {
                    clientMech = Message.BEARER_TOKEN;
                    if (mech.equals(Message.NULL_BEARER) ||
                        mech.equals(Message.TLS_BEARER) ||
                        mech.equals(Message.CLIENT_TLS_BEARER)) {
                        wsfVersion = SOAPBindingConstants.WSF_10_VERSION;
                    } else {
                        wsfVersion = SOAPBindingConstants.WSF_11_VERSION;
                    }
                    DiscoSDKUtils.debug.message("DiscoClient: bearer token");
                    List credRefs = desc.getCredentialRef();
                    if ((credRefs == null) || (credRefs.size() == 0)) {
                        throw new DiscoveryException(
                                DiscoSDKUtils.bundle.getString("noCredential"));
                    } else {
                        String credID = (String) credRefs.get(0);
                        if (credID == null || assertions == null) {
                            throw new DiscoveryException(
                                DiscoSDKUtils.bundle.getString("noCredential"));
                        } else {
                            Iterator iter2 = assertions.iterator();
                            while (iter2.hasNext()) {
                                SecurityAssertion sassert = (SecurityAssertion)
                                                iter2.next();
                                if (credID.equals(sassert.getAssertionID())) {
                                    assertion = sassert;
                                    break;
                                }
                            }
                            if (assertion == null) {
                                throw new DiscoveryException(
                                DiscoSDKUtils.bundle.getString("noCredential"));
                            }
                        }
                    }
                    if (mech.equals(Message.CLIENT_TLS_BEARER) ||
                        mech.equals(Message.CLIENT_TLS_BEARER_WSF11)) {
                        clientAuth = true;
                        DiscoSDKUtils.debug.message("DiscoClient: clientAuth on");
                    }
                    return;
                }
            }
        }
        // still here? couldn't find supported mech id
        
        DiscoSDKUtils.debug.error("DiscoveryClient.processResourceOffering: " +
            "Couldn't find supported SecurityMechID from ResourceOffering.");
        throw new DiscoveryException(
                DiscoSDKUtils.bundle.getString("noSupportedSecuMechID"));
    }
 
    /**
     * Sets the alias for the client certificate. If none is set, a default
     * client certificate will be used.
     * @param certAlias certificate alias name
     */ 
    public void setClientCert(String certAlias) {
        this.certAlias = certAlias;
    }

    /**
     * Sets flag to indicate whether the connection is SSL/TLS with client
     * authentication. When this flag is set to true, the message will not be
     * signed according to the spec. If you want to sign the message always,
     * do not set this flag to true, even when the connection is SSL/TLS with
     * client authentication.
     *
     * @param value The flag value to be set
     */
    public void setClientAuthentication(boolean value) {
        clientAuth = value;
    }

    /**
     * Sets the resource ID to be accessed.
     * @param resourceID resource ID
     */
    public void setResourceID(String resourceID) {
        resID = new ResourceID(resourceID);
    }

    /**
     * Sets the encrypted resource ID to be accessed.
     *
     * @param resourceID encrypted resource ID.
     */
    public void setResourceID(EncryptedResourceID resourceID) {
        encResID = resourceID;
    }
 
    /**
     * Sets the provider ID.
     *
     * @param providerID ID of the web service client.
     */
    public void setProviderID(String providerID) {
        this.providerID = providerID;
    }

    /**
     * Queries discovery service for <code>ResourceOffering</code> given list of
     * service types.
     * 
     * @param serviceTypes List of <code>serviceTypes</code> as
     *        <code>java.lang.String</code> to be queried 
     * @return Query response Element corresponding to the query
     * @exception DiscoveryException if error occurs
     */
    public QueryResponse getResourceOffering(java.util.List serviceTypes) 
        throws DiscoveryException 
    {
        if (!processed) {
            processResourceOffering();
            processed = true;
        }
        Query query = null;
            Iterator i = serviceTypes.iterator();
            List serviceList = new ArrayList();
            while (i.hasNext()) {
                serviceList.add(new RequestedService(null, (String) i.next()));
            }
        if (resID != null) {
            query = new Query(resID, serviceList);
        } else {
            query = new Query(encResID, serviceList);
        }

        return getResourceOffering(query);
    }

    /**
     * Queries discovery service for resource offering.
     * @param query discovery query object 
     * @return Query response Element corresponding to the query
     * @exception DiscoveryException if error occurs
     */
    public QueryResponse getResourceOffering(Query query) 
        throws DiscoveryException 
    {
        Message req = createRequest();
        req.setSOAPBody(DiscoSDKUtils.parseXML(query.toString()));
        return new QueryResponse(getResponse(req));
    }


    private Message createRequest() throws DiscoveryException {
        if (!processed) {
            processResourceOffering();
            processed = true;
        }
        // create new Message according to different secuMechID
        Message req = null;
        ProviderHeader provH = null;
        if (providerID != null) {
            try {
                provH = new ProviderHeader(providerID);
            } catch (SOAPBindingException sbe) {
                throw new DiscoveryException(sbe.getMessage());
            }
        }
        if (clientMech == Message.X509_TOKEN) {
            DiscoSDKUtils.debug.message(
                "DiscoveryClient.createRequest: mech=x509");
            try {
                 req = new Message(provH, token);
            } catch (SOAPBindingException sbe) {
                throw new DiscoveryException(sbe.getMessage());
            }
        } else if ((clientMech == Message.SAML_TOKEN) ||
                    (clientMech == Message.BEARER_TOKEN)) {
            if (DiscoSDKUtils.debug.messageEnabled()) {
                DiscoSDKUtils.debug.message("DiscoveryClient.createRequest: "
                        + "mech=saml or bearer");
            }
            try {
                req = new Message(provH, assertion);
            } catch (SOAPBindingException sbe) {
                throw new DiscoveryException(sbe.getMessage());
            }
        } else {
            if (DiscoSDKUtils.debug.messageEnabled()) {
                DiscoSDKUtils.debug.message("DiscoveryClient.createRequest: "
                    + "mech=anon");
            }
            try {
                req = new Message(provH);
            } catch (SOAPBindingException sbe) {
                throw new DiscoveryException(sbe.getMessage());
            }
        }
        if (clientAuth) {
            req.setClientAuthentication(clientAuth);
        }
        req.setWSFVersion(wsfVersion);
        return req;
    }

    private Element getResponse(Message req) throws DiscoveryException {
        Message resp = null;
        try {
            resp = Client.sendRequest(req, connectTo, certAlias, soapAction);
        } catch (Exception e) {
            DiscoSDKUtils.debug.error("DiscoveryClient.getResponse:", e);
            throw new DiscoveryException(e.getMessage());
        }
        List bodies = resp.getBodies();
        if (!(bodies.size() == 1)) {
            DiscoSDKUtils.debug.error("DiscoveryClient.getResponse: SOAP Response "
                + "didn't contain one SOAPBody.");
            throw new DiscoveryException(
                                DiscoSDKUtils.bundle.getString("oneBody"));
        }
        return ((Element) bodies.iterator().next());

    }

    /**
     * Modifies discovery resource offering.
     * @param modify List of Modify object
     * @return List of <code>ModifyResponse</code> object
     * @exception DiscoveryException if error occurs
     */
    public ModifyResponse modify(Modify modify) 
        throws DiscoveryException
    {
        Message req = createRequest();
        req.setSOAPBody(DiscoSDKUtils.parseXML(modify.toString()));

        return new ModifyResponse(getResponse(req));
    }

    /**
     * Sets the web services version.
     *
     * @param wsfVersion the web services version that should be used.
     */
    public void setWSFVersion(String wsfVersion) {
        this.wsfVersion = wsfVersion;
    }
}
