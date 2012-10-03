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
 * $Id: DSTClient.java,v 1.5 2008/12/16 01:48:32 exu Exp $
 *
 */

package com.sun.identity.liberty.ws.dst;

import com.sun.identity.liberty.ws.security.SecurityAssertion;
import com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken;
import com.sun.identity.liberty.ws.disco.EncryptedResourceID;
import com.sun.identity.liberty.ws.disco.ResourceOffering;
import com.sun.identity.liberty.ws.disco.ServiceInstance;
import com.sun.identity.liberty.ws.disco.Description;
import com.sun.identity.liberty.ws.disco.ResourceID;
import com.sun.identity.liberty.ws.soapbinding.Message;
import com.sun.identity.liberty.ws.soapbinding.Client;
import com.sun.identity.liberty.ws.soapbinding.ProviderHeader;
import com.sun.identity.liberty.ws.soapbinding.SOAPFaultException;
import com.sun.identity.liberty.ws.soapbinding.SOAPBindingException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import org.w3c.dom.Element;
import com.sun.identity.liberty.ws.security.SecurityTokenManagerClient;
import com.sun.identity.liberty.ws.interaction.InteractionRedirectException;
import com.sun.identity.liberty.ws.interaction.InteractionException;
import com.sun.identity.liberty.ws.interaction.InteractionManager;
import com.sun.identity.liberty.ws.soapbinding.ServiceInstanceUpdateHeader;
import com.sun.identity.liberty.ws.soapbinding.SOAPBindingConstants;
import com.sun.identity.liberty.ws.soapbinding.Utils;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The class <code>DSTClient</code> provides methods for Liberty
 * Data Service Template.
 *
 * @supported.all.api
 */
public class DSTClient {

     private String resourceID = null;
     private EncryptedResourceID encryptedResourceID = null;
     private String soapURI = null;
     private String certAlias = null;
     private SecurityAssertion assertion = null;
     private BinarySecurityToken token = null;
     private boolean isEncryptedResourceID = false;
     // default to anonymous
     private int securityProfile = Message.ANONYMOUS;
     private String soapAction = null;
     private String serviceType = null;
     private HttpServletRequest httpRequest = null;
     private HttpServletResponse httpResponse = null;
     private boolean clientAuthEnabled = false;
     private String providerID = null;
     private ServiceInstanceUpdateHeader serviceInstanceUpdateHeader = null;
     private String wsfVersion = Utils.getDefaultWSFVersion();


    /**
     * Constructor
     * The constructor connects to Data Service without <code>WSS</code> token 
     * @param soapURI URI of the SOAP end point for this Data service
     *                       instance
     * @param providerID ID of service provider. 
     */
    public DSTClient(String soapURI, String providerID) {
        this.soapURI = soapURI;
        this.providerID = providerID;
        this.securityProfile = Message.ANONYMOUS;
    }

    /**
     * Constructor
     * The constructor connects to Data Service using <code>WSS</code> 
     * SAML Token
     * @param assertion <code>WSS</code> SAML Token
     * @param soapURI URI of the SOAP end point for this data 
     *                       service instance
     * @param providerID ID of service provider. 
     */
    public DSTClient (SecurityAssertion assertion, 
                      String soapURI,
                      String providerID) {
        this.assertion = assertion;
        this.soapURI = soapURI;
        this.providerID = providerID;
        if(assertion != null && assertion.isBearer()) {
           this.securityProfile = Message.BEARER_TOKEN;
        } else {
           this.securityProfile = Message.SAML_TOKEN;
        }
    }

    /**
     * Constructor
     * The constructor connects to the data Service using <code>WSS X509</code>
     * Token.
     *
     * @param token  <code>WSS X.509</code> Certificate Token
     * @param soapURI URI of the SOAP end point for this Data 
     *                       service instance
     * @param providerID ID of service provider. 
     */
    public DSTClient(BinarySecurityToken token, 
                     String soapURI,
                     String providerID) {
        this.token = token;
        this.soapURI = soapURI;
        this.securityProfile = Message.X509_TOKEN;
        this.providerID = providerID;
    }

    /**
     * Constructor
     * The constructor connects to Data Service without <code>WSS</code> token, 
     * the <code>HttpServletRequest</code> and <code>HttpServletResponse</code>
     * object of the current request agent will be used for resource owner
     * interactions if needed.
     *
     * @param soapURI URI of the SOAP end point for this Data 
     *        service instance
     * @param providerID ID of service provider. 
     * @param httpRequest <code>HttpServletRequest</code> object of current
     *        user agent request.
     * @param httpResponse <code>HttpServletResponse</code> object of current
     *        user agent request.
     */ 
    public DSTClient(String soapURI,
                     String providerID,
                     HttpServletRequest httpRequest, 
                     HttpServletResponse httpResponse) {
        this.soapURI = soapURI;
        this.securityProfile = Message.ANONYMOUS;
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
        this.providerID = providerID;
    } 

    /**
     * Constructor
     * The constructor connects to Data Service with <code>WSS</code>
     * SAML token, the <code>HttpServletRequest</code> and
     * <code>HttpServletResponse</code> object of the current request
     * agent will be used for resource owner interactions if needed.
     * @param assertion <code>WSS</code> SAML Token.
     * @param soapURI URI of the SOAP end point for this Data service instance.
     * @param providerID ID of service provider. 
     * @param httpRequest <code>HttpServletRequest</code> object of current
     *        user agent request.
     * @param httpResponse <code>HttpServletResponse</code> object of current
     *        user agent request.
     */
    public DSTClient(SecurityAssertion assertion, 
                     String soapURI,
                     String providerID,
                     HttpServletRequest httpRequest,
                     HttpServletResponse httpResponse) {

        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
        this.assertion = assertion;
        this.soapURI = soapURI;
        this.securityProfile = Message.SAML_TOKEN;
        this.providerID = providerID;
    }

    /**
     * Constructor
     * The constructor connects to Data Service with <code>WSS</code> 
     * SAML token, the <code>HttpServletRequest</code> and
     * <code>HttpServletResponse</code> object of the current request
     * agent will be used for resource owner interactions if needed.
     * @param token <code>WSS X.509</code> Certificate Token
     * @param soapURI URI of the SOAP end point for this Data 
     *                service instance
     * @param providerID ID of service provider. 
     * @param httpRequest <code>HttpServletRequest</code> object of current
     *        user agent request.
     * @param httpResponse <code>HttpServletResponse</code> object of current
     *        user agent.
     */
    public DSTClient(BinarySecurityToken token, 
                     String soapURI,
                     String providerID,
                     HttpServletRequest httpRequest,
                     HttpServletResponse httpResponse) {
        this.token = token;
        this.soapURI = soapURI;
        this.securityProfile = Message.X509_TOKEN;
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
        this.providerID = providerID;
    }

    /**
     * Contructor
     * Creates a data service template client instance.
     * Connects to data Service specified by the resource offering.
     * <code>resourceID</code>, security mechanism and SOAP endpoint defined
     * in the <code>ResourceOffering</code> will be used.
     *
     * @param resourceOffering resource offering for this 
     *        discovery service instance 
     * @param providerID ID of this service provider. 
     * @param credential Credential of <code>WSC</code>
     * @param httpRequest <code>HttpServletRequest</code> object of current
     *        user agent request.
     * @param httpResponse <code>HttpServletResponse</code> object of current
     *        user agent.
     * @exception DSTException if the <code>resourceOffering</code> is not valid
     */
    public DSTClient(
        ResourceOffering resourceOffering,
        String providerID,
        Object credential,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) throws DSTException {
        if(resourceOffering == null) {
            DSTUtils.debug.error("DSTClient: resource offering is null");
            throw new DSTException(
                DSTUtils.bundle.getString("nullInputParams"));
        }
        parseResourceOffering(resourceOffering);
        if(securityProfile == Message.X509_TOKEN) {
            generateBinarySecurityToken(credential);
        }
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
        this.providerID = providerID;
    }

    /**
     * Constructor
     * Creates a data service template client instance.
     * Connects to data Service specified by the resource offering.
     * <code>resourceID</code>, security mechanism and SOAP endpoint defined
     * in the <code>ResourceOffering</code> will be used.
     *
     * @param resourceOffering resource offering for this 
     *        discovery service instance 
     * @param providerID ID of this service provider. 
     * @param credential Credential of <code>WSC</code>
     * @exception DSTException if the <code>resourceOffering</code> is not valid
     */
    public DSTClient(ResourceOffering resourceOffering,
                     String providerID,
                     Object credential)
    throws DSTException { 
        if(resourceOffering == null) {
           DSTUtils.debug.error("DSTClient: resource offering is null");
           throw new DSTException(DSTUtils.bundle.getString("nullInputParams"));
        }
        parseResourceOffering(resourceOffering);
        if(securityProfile == Message.X509_TOKEN) {
           generateBinarySecurityToken(credential);
        }
        this.providerID = providerID;
    }

    /**
     * Parses the given discovery resource offering for the Data service.
     * @param offering ResourceOffering 
     * @exception DSTException.
     */
    private void parseResourceOffering(ResourceOffering offering)
    throws DSTException {
        //Try for the encrypted resource offering first;
        encryptedResourceID = offering.getEncryptedResourceID(); 
        if(encryptedResourceID != null) {
           isEncryptedResourceID = true;
        } else {
           ResourceID resID = offering.getResourceID();
           if(resID == null) {
              DSTUtils.debug.error("DSTClient:parseResourceOffering: " +
              "No ResourceID");
              throw new DSTException(DSTUtils.bundle.getString("noResourceID"));
           }
           resourceID = resID.getResourceID();
        }

        ServiceInstance serviceInstance = offering.getServiceInstance();
        // providerID = serviceInstance.getProviderID();
        if(serviceInstance == null) {
           DSTUtils.debug.error("DSTClient:parseResourceOffering: " +
           "No service instance.");
           throw new DSTException(
           DSTUtils.bundle.getString("noServiceInstance"));
        }
        serviceType = serviceInstance.getServiceType();
        if(serviceType == null) {
           DSTUtils.debug.error("DSTClient:parseResourceOffering: " +
           "service type is null.");
           throw new DSTException(DSTUtils.bundle.getString("noServiceType"));
        }
      
        List descriptions = serviceInstance.getDescription();
        if(descriptions == null || descriptions.isEmpty()) {
           DSTUtils.debug.error("DSTClient:parseResourceOffering: " +
           "descriptions are null.");
           throw new DSTException(DSTUtils.bundle.getString("noDescriptions")); 
        }
        // A service instance can have mutiple descriptions. In this case,
        // we will try to use a valid description. 
        Iterator iter = descriptions.iterator(); 
        while(iter.hasNext()) {
           Description description = (Description)iter.next();
           soapAction = description.getSoapAction();
           soapURI = description.getEndpoint();
           if(soapURI == null || soapURI.length() == 0) {
              continue;
           }

           List secMechIDs = description.getSecurityMechID();
           if(secMechIDs == null || secMechIDs.isEmpty()) {
              continue;
           }
           boolean foundProfile = false;
           int size = secMechIDs.size();

           for(int i=0; i < size; i++) {
               String secProfile = (String)secMechIDs.get(i);
               secProfile = secProfile.trim();

               if(secProfile.equals(Message.NULL_NULL) ||
                  secProfile.equals(Message.TLS_NULL) ||
                  secProfile.equals(Message.CLIENT_TLS_NULL)) {

                  securityProfile = Message.ANONYMOUS;
                  if(secProfile.equals(Message.CLIENT_TLS_NULL)) {
                     clientAuthEnabled = true;
                  }
                  foundProfile = true;
                  break;

               } else if(secProfile.equals(Message.NULL_X509) ||
                  secProfile.equals(Message.TLS_X509) ||
                  secProfile.equals(Message.CLIENT_TLS_X509) ||
                  secProfile.equals(Message.NULL_X509_WSF11) ||
                  secProfile.equals(Message.TLS_X509_WSF11) ||
                  secProfile.equals(Message.CLIENT_TLS_X509_WSF11)) {

                  securityProfile = Message.X509_TOKEN;
                  if (secProfile.equals(Message.NULL_X509) ||
                      secProfile.equals(Message.TLS_X509) ||
                      secProfile.equals(Message.CLIENT_TLS_X509)) {
                      wsfVersion = SOAPBindingConstants.WSF_10_VERSION;
                  } else {
                      wsfVersion = SOAPBindingConstants.WSF_11_VERSION;
                  }

                  securityProfile = Message.X509_TOKEN;
                  if (secProfile.equals(Message.CLIENT_TLS_X509) ||
                      secProfile.equals(Message.CLIENT_TLS_X509_WSF11)) {
                      clientAuthEnabled = true;
                  }
                  foundProfile = true;
                  break;

               } else if(secProfile.equals(Message.NULL_SAML) ||
                  secProfile.equals(Message.TLS_SAML) ||
                  secProfile.equals(Message.CLIENT_TLS_SAML) ||
                  secProfile.equals(Message.NULL_SAML_WSF11) ||
                  secProfile.equals(Message.TLS_SAML_WSF11) ||
                  secProfile.equals(Message.CLIENT_TLS_SAML_WSF11)) {

                  securityProfile = Message.SAML_TOKEN;
                  if (secProfile.equals(Message.NULL_SAML) ||
                      secProfile.equals(Message.TLS_SAML) ||
                      secProfile.equals(Message.CLIENT_TLS_SAML)) {
                      wsfVersion = SOAPBindingConstants.WSF_10_VERSION;
                  } else {
                      wsfVersion = SOAPBindingConstants.WSF_11_VERSION;
                  }
                  if (secProfile.equals(Message.CLIENT_TLS_SAML) ||
                      secProfile.equals(Message.CLIENT_TLS_SAML_WSF11)) {
                      clientAuthEnabled = true;
                  }
                  foundProfile = true;
                  break;
               } else if(secProfile.equals(Message.NULL_BEARER) ||
                  secProfile.equals(Message.TLS_BEARER) ||
                  secProfile.equals(Message.CLIENT_TLS_BEARER) ||
                  secProfile.equals(Message.NULL_BEARER_WSF11) ||
                  secProfile.equals(Message.TLS_BEARER_WSF11) ||
                  secProfile.equals(Message.CLIENT_TLS_BEARER_WSF11)) {

                  securityProfile = Message.BEARER_TOKEN;
                  if (secProfile.equals(Message.NULL_BEARER) ||
                      secProfile.equals(Message.TLS_BEARER) ||
                      secProfile.equals(Message.CLIENT_TLS_BEARER)) {
                      wsfVersion = SOAPBindingConstants.WSF_10_VERSION;
                  } else {
                      wsfVersion = SOAPBindingConstants.WSF_11_VERSION;
                  }

                  if (secProfile.equals(Message.CLIENT_TLS_BEARER) ||
                      secProfile.equals(Message.CLIENT_TLS_BEARER_WSF11)) {
                      clientAuthEnabled = true;
                  }
                  foundProfile = true;
                  break;
               }
                 
           }
           if(foundProfile) {
              break;
           }
        }

        if(soapURI == null) {
           DSTUtils.debug.error("DSTClient:parseResourceOffering: " +
           "SOAP Endpoint or security profile is null");
           throw new DSTException(
           DSTUtils.bundle.getString("invalidResourceOffering"));
        }
        if(DSTUtils.debug.messageEnabled()) {
           DSTUtils.debug.message("DSTClient.parseResourceOffering:" +
           "soapURI = " + soapURI + "soapAction = " + soapAction + 
           "securityProfile = " + securityProfile);
        }
         
    }

    /**
     * Generates X509 security token for the WSC.
     * @param credential Credential of WSC
     * @exception DSTException
     */
    private void generateBinarySecurityToken(Object credential)
     throws DSTException {
        try {
            SecurityTokenManagerClient manager =
                new SecurityTokenManagerClient(credential);
            if (certAlias == null) {
                certAlias = SystemPropertiesManager.get(
                    "com.sun.identity.liberty.ws.wsc.certalias");
            }
            manager.setCertAlias(certAlias);
            token =  manager.getX509CertificateToken();
            token.setWSFVersion(wsfVersion);
        } catch (Exception e) {
            DSTUtils.debug.error("DSTClient:generateBinarySecurityToken:" +
            "Error in generating binary security token.", e);
            throw new DSTException(e);
        }
    }
 

    /**
     * Sets the resource ID to be accessed
     * @param resourceID resource ID String
     */
    public void setResourceID(String resourceID) {
        this.resourceID = resourceID;
        isEncryptedResourceID = false;
    }

    /**
     * Sets the encrypted resource ID to be accessed
     * @param encResourceID encrypted resource ID 
     */
    public void setResourceID(EncryptedResourceID encResourceID) {
        this.encryptedResourceID = encResourceID;
        isEncryptedResourceID = true;
    }

    /**
     * Sets the provider ID.
     * @param providerID Provider ID.
     */
    public void setProviderID(String providerID) {
        this.providerID = providerID;
    }

    /**
     * Sets the alias for the client certificate if the connection is TLS/SSL
     * with client authentication.
     *
     * @param certAlias certificate alias name
     */ 
    public void setClientCert(String certAlias) {
        this.certAlias = certAlias;
    }

    /**
     * Sets SOAP Action such as query or modify
     * @param action action for this soap request
     */
    public void setSOAPAction(String action) {
        this.soapAction = action;
    }

    /**
     * Sets the client authentication.
     *
     * @param value true value to enable client authentication.
     */
    public void setClientAuth(boolean value) {
        this.clientAuthEnabled = value;
    }

    /**
     * Sets the SOAP Endpoint.
     * @param endpoint SOAP Endpoint. 
     */
    public void setSOAPEndPoint(String endpoint) {
        this.soapURI = endpoint;
    }

    /**
     * Sets the Security Assertion.
     * @param secAssertion Security Assertion.
     */
    public void setSecurityAssertion(SecurityAssertion secAssertion) {
        this.assertion = secAssertion;
    }

    /**
     * Sets the binary security token.
     * @param binaryToken Binary Security Token.
     */
    public void setBinarySecurityToken(BinarySecurityToken binaryToken) {
        this.token = binaryToken;
    }

    /**
     * Sets the security mechanism.
     * @param secMech security mechanism.
     */
    public void setSecurityMech(String secMech) {
        if(secMech == null || secMech.endsWith("null")) {
           securityProfile = Message.ANONYMOUS;
        } else if(secMech.endsWith("X509")) {
           securityProfile = Message.X509_TOKEN;
        } else if(secMech.endsWith("SAML")) {
           securityProfile = Message.SAML_TOKEN;
        } else if(secMech.endsWith("Bearer")) {
           securityProfile = Message.BEARER_TOKEN;
        }
    }
 

    /**
     * Gets data for the specified query items.
     * @param items list of <code>DSTQueryItem</code> object 
     * @return List of <code>DSTData</code> objects which have one-to-one
     *         correspondence to the list of query items. If no response for
     *         one of the query item, the corresponding return
     *         <code>DSTData</code> object will be null.
     * @exception DSTException if error occurs when trying to get data
     * @exception InteractionRedirectException if user agent is redirected to
     *            Web Service Provider (<code>WSP</code>) for resource owner
     *            interactions 
     */
    public java.util.List getData(java.util.List items) 
      throws DSTException, InteractionRedirectException
    {
        DSTUtils.debug.message("DSTClient:getData:Init");

        if(items == null || items.size() == 0) {
           DSTUtils.debug.error("DSTUtils.getData:Query items are null.");
           throw new DSTException(DSTUtils.bundle.getString("nullInputParams"));
        }

        DSTQuery dstQuery = null;
        
        if(isEncryptedResourceID) {
           dstQuery = new DSTQuery(encryptedResourceID, items, null);
        } else {
           dstQuery = new DSTQuery(resourceID, items, null);
        }
 
        List query = new ArrayList();
        query.add(DSTUtils.parseXML(dstQuery.toString(true, true)));
       
        List  response = sendMessage(query);

        if(response == null || response.size() == 0) {
           DSTUtils.debug.message("DSTClient:getData: response is null");
           return null;
        }

        DSTQueryResponse queryResponse = 
            new DSTQueryResponse((Element)response.get(0));
        return queryResponse.getData();
    }

    /**
     * Gets <code>QueryResponse</code> for the specified query.
     * @param query <code>DSTQuery</code> object. 
     * @return <code>DSTDQueryResponse</code> Object.
     * @exception DSTException if error occurs when trying to get data
     * @exception InteractionRedirectException if user agent is redirected to
     *            Web Service Provider (<code>WSP</code>) for resource owner
     *            interactions 
     */
    public DSTQueryResponse query(DSTQuery query) 
    throws DSTException, InteractionRedirectException {

        DSTUtils.debug.message("DSTClient:query:Init");
        if(query == null) {
           DSTUtils.debug.message("DSTClient:query:null value");
           throw new DSTException(DSTUtils.bundle.getString("nullInputParams"));
        }

        List request = new ArrayList();
        request.add(DSTUtils.parseXML(query.toString(true, true)));
        List response = sendMessage(request);
        Element queryResponse = (Element)response.get(0);
        return new DSTQueryResponse(queryResponse); 
    }

    /**
     * Gets response for a list of <code>DST</code> Modifications.
     * @param items List of <code>DSTModification</code> objects.
     * @return <code>DSTModifyResponse</code>.
     * @exception DSTException if error occurs when trying to modify data
     * @exception InteractionRedirectException if user agent is redirected to
     *            Web Service Provider (<code>WSP</code>) for resource owner
     *            interactions.
     */
    public DSTModifyResponse modify(java.util.List items) 
    throws DSTException, InteractionRedirectException {

        DSTUtils.debug.message("DSTClient:modify:init:");
        if(items == null) {
           DSTUtils.debug.message("DSTClient:modify:null values");
           throw new DSTException(DSTUtils.bundle.getString("nullInputParams"));
        }

        DSTModify modify = null;

        if(isEncryptedResourceID) {
           modify = new DSTModify(encryptedResourceID, items, null);
        } else {
           modify = new DSTModify(resourceID, items, null);
        }

        List request = new ArrayList();
        request.add(DSTUtils.parseXML(modify.toString(true, true)));

        List response = sendMessage(request);
        if(response == null || response.size() == 0) {
           DSTUtils.debug.message("DSTClient:modify: response is null");
           return null;
        }

        return new DSTModifyResponse((Element)response.get(0));

    }

    /**
     * Gets modify response for the specified modify.
     * @param modify <code>DSTModify</code> object.
     * @return <code>DSTModifyResponse</code> object.
     * @exception DSTException if error occurs when trying to modify data
     * @exception InteractionRedirectException if user agent is redirected to
     *            Web Service Provider (<code>WSP</code>) for resource owner
     *            interactions 
     */ 
    public DSTModifyResponse modify(DSTModify modify) 
     throws DSTException, InteractionRedirectException {

        DSTUtils.debug.message("DSTClient:modify:init");
        if(modify == null) {
           DSTUtils.debug.message("DSTClient:modify:null values");
           throw new DSTException(DSTUtils.bundle.getString("nullInputParams"));
        }

        List request = new ArrayList();
        request.add(DSTUtils.parseXML(modify.toString(true, true)));

        List response = sendMessage(request);
        return new DSTModifyResponse((Element)response.get(0));
    } 

    /**
     * Gets query responses for a list of <code>DST</code> queries
     * @param queries list of <code>DSTQuery</code> objects
     * @return List of <code>DSTQueryResponse</code> objects, corresponding
     *         to each <code>DSTQuery</code> object.
     * @exception DSTException if error occurs when trying to get data
     * @exception InteractionRedirectException if interaction is required.
     */
    public java.util.List getQueryResponse(java.util.List queries) 
     throws DSTException,  InteractionRedirectException
    {
        DSTUtils.debug.message("DSTClient.getQueryResponse:Init");
        if(queries == null || queries.size() == 0) {
           DSTUtils.debug.error("DSTClient.getQueryResponse:null values");
           throw new DSTException(DSTUtils.bundle.getString("nullInputParams"));
        }
        Iterator iter = queries.iterator();
        List requests = new ArrayList();
        while(iter.hasNext()) {
           DSTQuery query = (DSTQuery)iter.next();
           requests.add(DSTUtils.parseXML(query.toString(true, true)));
        }
        List responses = sendMessage(requests); 
        if(responses == null || responses.size() == 0) {
           DSTUtils.debug.error("DSTClient.getQueryResponse:null responses");
           throw new DSTException(DSTUtils.bundle.getString("nullResponse"));
        }
        List queryResponses = new ArrayList();
        Iterator iter1 = responses.iterator();
        while(iter1.hasNext()) {
            queryResponses.add(new DSTQueryResponse((Element)iter1.next()));   
        }
        return queryResponses;
    }

    /**
     * Performs a list of modifications specified by a list of
     * <code>DSTModify</code> objects (possible on different resource ID).
     *
     * @param modifies list of <code>DSTModify</code> objects specifying
     *        resource and modifications to be made.
     * @return List of <code>DSTModifyResponse</code> object corresponding
     *         to each <code>DSTModify</code>.
     * @exception DSTException if error occurs when trying to modify data
     * @exception InteractionRedirectException if interaction is required.
     */
    public java.util.List getModifyResponse(java.util.List modifies) 
     throws DSTException , InteractionRedirectException
    {
        DSTUtils.debug.message("DSTClient.getModifyResponse:Init");
        if(modifies == null || modifies.size() == 0) {
           DSTUtils.debug.error("DSTClient.getModifyResponse:null values");
           throw new DSTException(DSTUtils.bundle.getString("nullInputParams"));
        }
        Iterator iter = modifies.iterator();
        List requests = new ArrayList();
        while(iter.hasNext()) {
           DSTModify modify = (DSTModify)iter.next();
           requests.add(DSTUtils.parseXML(modify.toString(true, true)));
        }
        List responses = sendMessage(requests);
        if(responses == null || responses.size() == 0) {
           DSTUtils.debug.error("DSTClient.getModifyResponse:null responses");
           throw new DSTException(DSTUtils.bundle.getString("nullResponse"));
        }
        List modifyResponses = new ArrayList();
        Iterator iter1 = responses.iterator();
        while(iter1.hasNext()) {
            modifyResponses.add(new DSTModifyResponse((Element)iter1.next()));
        }
        return modifyResponses;

    }

    /**
     * Sends the SOAP Message to the data service.
     * @param List of Request Objects.
     * @return List of Response Objects.
     * @exception DSTException for failure.
     */
    private List sendMessage(List requestObjects) 
    throws DSTException, InteractionRedirectException {

        DSTUtils.debug.message("DSTClient:sendMessage:Init");
        if(requestObjects == null || requestObjects.size() == 0) {
           DSTUtils.debug.message("DSTClient:sendMessage: requestobj are null");
           throw new DSTException(DSTUtils.bundle.getString("nullInputParams"));
        }

        try {
            Message msg = null;

            ProviderHeader provH = null;
            if(providerID != null) {
               provH = new ProviderHeader(providerID);
            }

            if(securityProfile == Message.X509_TOKEN) {
               if(token == null) {
                  throw new DSTException(
                  DSTUtils.bundle.getString("nullToken"));
               }
               DSTUtils.debug.message("DSTClient:sendMessage:using x509");
               msg = new Message(provH, token);
           
            } else if(securityProfile == Message.SAML_TOKEN) {
               DSTUtils.debug.message("DSTClient:sendMessage:using SAML");
               msg = new Message(provH, assertion);

            } else if(securityProfile == Message.BEARER_TOKEN) {
               DSTUtils.debug.message("DSTClient:sendMessage:using Bearer");
               msg = new Message(provH, assertion);

            } else if(securityProfile == Message.ANONYMOUS) {
               DSTUtils.debug.message("DSTClient:sendMessage:using Anonymous");
               msg = new Message(provH);

            } else {
               throw new DSTException(
               DSTUtils.bundle.getString("invalidSecurityProfile"));
            }
                
            msg.setSOAPBodies(requestObjects);
            msg.setWSFVersion(wsfVersion);

            if(clientAuthEnabled) {
               msg.setClientAuthentication(clientAuthEnabled);
            }

            if(DSTUtils.debug.messageEnabled()) {
               DSTUtils.debug.message("DSTClient:sendMessage: request:" 
                 + msg.toString());
            }

            Message response = null;

            if(httpRequest != null) {
               response = handleInteraction(msg);
            } else {
               response = Client.sendRequest(
               msg, soapURI, certAlias, soapAction);
            }

            if(DSTUtils.debug.messageEnabled()) {
               DSTUtils.debug.message("DSTClient:sendMessage:response = " +
               response.toString());
            }

            serviceInstanceUpdateHeader = 
                   response.getServiceInstanceUpdateHeader();

            return response.getBodies();

        } catch(SOAPBindingException sbe) {
            DSTUtils.debug.error("DSTClient:sendMessage:soapbindexception",sbe);
            throw new DSTException(sbe);
        } catch(SOAPFaultException sfe) {
            DSTUtils.debug.error("DSTClient:sendMessage:soapfault", sfe);
            serviceInstanceUpdateHeader =
                    sfe.getSOAPFaultMessage().getServiceInstanceUpdateHeader();
            throw new DSTException(sfe);
        }
    }

    /**
     * Handles interaction request processing.
     * When the interaction is required, it throws and InteractRedirect
     * Exception, and redirect to the caller application(servlet). 
     */
    private Message handleInteraction(Message requestMsg) 
     throws DSTException, SOAPFaultException, 
           SOAPBindingException, InteractionRedirectException {

        if(requestMsg == null || httpRequest == null ||
           httpResponse == null || soapURI == null) {
           DSTUtils.debug.error("DSTClient:handeInteraction:null values");
           throw new DSTException(
           DSTUtils.bundle.getString("nullInputParams"));
        }
        DSTUtils.debug.message("DSTClient:handleInteraction:init");
        String resend = httpRequest.getParameter(
                        InteractionManager.RESEND_MESSAGE);
        String returnURL = httpRequest.getRequestURL().toString();
        Message response;
        try {
            InteractionManager manager = InteractionManager.getInstance();
            if(resend == null) {
               //If the interaction is not required, this will send a
               // original response.
               response = manager.sendRequest(requestMsg, soapURI, certAlias,
                          soapAction, returnURL, httpRequest, httpResponse);
            } else {
               response = manager.resendRequest(returnURL, 
                                 httpRequest, httpResponse);
            }
            return response;
        } catch (InteractionRedirectException ire) {
            DSTUtils.debug.message("DSTClient:handleInteraction: Interaction" +
            "Redirection happened.");
            throw ire; 
        }  catch (InteractionException ie) {
            DSTUtils.debug.error("DSTClient:handleInteraction: Interaction" +
            " Error occured.", ie);
            throw new DSTException(ie);
        }
    }

    /**
     * Gets the <code>serviceInstanceUpdate</code> header from the SOAP
     * response message.
     * Applications can make use of this method to check if there is an
     * alternate endpoint or credential available for the service requests.
     *
     * @return <code>ServiceInstanceUpdateHeader</code> from the response
     *         message.
     */ 
    public ServiceInstanceUpdateHeader getServiceInstanceUpdateHeader() {
        return serviceInstanceUpdateHeader;
    }

    /**
     * Sets the web services version.
     *
     * @param wsfVersion the web services version that needs to be set.
     */
    public void setWSFVersion(String wsfVersion) {
        this.wsfVersion = wsfVersion;
    }
}
