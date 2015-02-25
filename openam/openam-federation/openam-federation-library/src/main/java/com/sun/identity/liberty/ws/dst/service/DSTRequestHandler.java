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
 * $Id: DSTRequestHandler.java,v 1.3 2008/06/25 05:47:14 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.dst.service;

import com.sun.identity.liberty.ws.disco.jaxb.ResourceIDType; 
import com.sun.identity.liberty.ws.disco.jaxb.EncryptedResourceIDType; 
import com.sun.identity.liberty.ws.disco.EncryptedResourceID;
import com.sun.identity.liberty.ws.dst.DSTUtils;
import com.sun.identity.liberty.ws.soapbinding.SOAPFaultException;
import com.sun.identity.liberty.ws.soapbinding.Message;
import com.sun.identity.liberty.ws.soapbinding.RequestHandler;
import com.sun.identity.liberty.ws.soapbinding.Utils;
import com.sun.identity.liberty.ws.security.SecurityTokenManager;
import com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken;
import com.sun.identity.liberty.ws.dst.DSTException;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.List;
import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * The class <code>DSTRequestHandler</code> is a handler for processing
 * Query or Modify Requests for any generic data service that are built
 * using Liberty SIS specifications. This class includes common processing
 * rules defined by Liberty DST specification, it is an extension point
 * for any Liberty DST based web services.
 *
 * @supported.all.api
 */
public abstract class DSTRequestHandler implements RequestHandler {
 
    /**
     * Default constructor
     */
     protected DSTRequestHandler() {}
  
    /**
     * Processes the request for the given personal profile service request.
     * @param msg SOAP Request message
     * @return Message SOAP Response Message.
     * @exception SOAPFaultException if the service requires an interaction.
     * @exception Exception for any generic failure.
     */
    public Message processRequest(Message msg)
    throws SOAPFaultException, Exception {

       if(DSTUtils.debug.messageEnabled()) {
          DSTUtils.debug.message("DSTRequestHandler:processRequest:" +
          "Request received: " + msg.toString());
       }

       List requestBodies = msg.getBodies();
       requestBodies = Utils.convertElementToJAXB(requestBodies);

       if(requestBodies == null || requestBodies.size() == 0) {
          DSTUtils.debug.error("DSTRequestHandler:processRequest:"+
          "SOAPBodies are null");
          throw new Exception(DSTUtils.bundle.getString("nullInputParams"));
       }

       Message response = null;
       int securityProfile = msg.getSecurityProfileType();
       if((securityProfile == Message.X509_TOKEN) ||
          (securityProfile == Message.SAML_TOKEN) || 
          (securityProfile == Message.BEARER_TOKEN)) {
          response = new Message(null, generateBinarySecurityToken(msg));
       } else {
          response = new Message();
       }

       response.setCorrelationHeader(msg.getCorrelationHeader());

       response.setWSFVersion(msg.getWSFVersion());
       List responseBodies = processSOAPBodies(requestBodies, msg, response);
       responseBodies = Utils.convertJAXBToElement(responseBodies);

       response.setSOAPBodies(responseBodies);
       if(DSTUtils.debug.messageEnabled()) {
          DSTUtils.debug.message("DSTRequestHandler:processRequest:" +
          "returned response: " + response.toString());
       }
       return response;
    }

    /**
     * Processes each SOAPBody.
     * @param requestBodies list of request bodies
     * @return List list of response bodies.
     * @exception SOAPFaultException for the interaction queries.
     * @exception DSTException for any failure.
     */
    private List processSOAPBodies(List requestBodies, 
            Message msg, Message response)
     throws SOAPFaultException, DSTException {

       DSTUtils.debug.message("DSTRequestHandler:processSOAPBodies:Init");
       List responseBodies = new ArrayList();
       int size = requestBodies.size();
       for(int i=0; i < size; i++) {
           Object request = requestBodies.get(i);
           responseBodies.add(processDSTRequest(request, msg, response));
       }
       return responseBodies;
    }

    /**
     * Generates the binary security token if the security profile is X509.
     * @param msg Request Message. 
     * @return BinarySecurityToken.
     * @exception DSTException.
     */
    private BinarySecurityToken generateBinarySecurityToken(Message msg)
    throws DSTException {
        try {
            SecurityTokenManager manager = new SecurityTokenManager(
                                 msg.getToken());
            BinarySecurityToken binaryToken = manager.getX509CertificateToken();
            binaryToken.setWSFVersion(msg.getWSFVersion());
            return binaryToken;
        } catch (Exception e) {
            DSTUtils.debug.error("DSTRequestHandler:generateBinary" +
            "SecurityToken: Error in generating binary security token.", e);
            throw new DSTException(e);
        }
    }


    /**
     * Processes query/modify request.
     * @param request query or modify object.
     * @param msg Request Message.
     * @param response response Message.
     * @return Object processed response object.
     * @exception DSTException for failure.
     * @exception SOAPFaultException for the interaction redirects
     */
    public abstract Object processDSTRequest(
        Object request, Message msg, Message response)
     throws SOAPFaultException, DSTException;

    /**
     * Gets the Resource ID given in the Query or Modify Request.
     * @param resourceIDType JAXB ResourceIDType Object.
     * @param providerID Provider ID.
     * @param serviceType Service Type.
     * @return String resource id.
     */
    protected String getResourceID(
       Object resourceIDType, 
       String providerID,
       String serviceType) {

        DSTUtils.debug.message("PPRequestHandler:getResourceID:Init");

        if(resourceIDType == null) {
           if(DSTUtils.debug.messageEnabled()) {
              DSTUtils.debug.message("PPRequestHandler:getResourceID:" +
              "ResourceIDType is null");
           }
           return null;
       }

       if(resourceIDType instanceof ResourceIDType) {
          ResourceIDType resID = (ResourceIDType)resourceIDType;
          return  resID.getValue();
       } else if( resourceIDType instanceof EncryptedResourceIDType) {
          EncryptedResourceIDType encID =
                   (EncryptedResourceIDType)resourceIDType;
          try {
              Document encDoc = XMLUtils.newDocument();
                  DiscoUtils.getDiscoMarshaller().marshal(encID, encDoc);

              if(DSTUtils.debug.messageEnabled()) {
                 DSTUtils.debug.message("PPRequestHandler.getResourceID:" +
                      "Encrypted ResourceID = " + XMLUtils.print((Node)encDoc));
              }

              EncryptedResourceID encryptedId = new EncryptedResourceID(
                  encDoc.getDocumentElement(), serviceType);
              String resIDStr = EncryptedResourceID.getDecryptedResourceID(
                   encryptedId, providerID).getResourceID();

              if(DSTUtils.debug.messageEnabled()) {
                 DSTUtils.debug.message("PPRequestHandler.getResourceID: " +
                 "ResourceID Value after decryption" +  resIDStr);
              }
              return resIDStr;
           } catch (Exception ex) {
              DSTUtils.debug.error("PPRequestHandler.getResourceID:error", ex);
              return null;
           }
       } else {
           DSTUtils.debug.error("PPRequestHandler:getResourceID:invalid" +
           "resource ID type.");
           return null;
       }

    }
  /*
   * Issue to be resolved: DST Schema does not have any name space associated
   * with it. These schemas are included in each service schema., so that they
   * would inherit the name spaces of respective processing service.
   * Hence, DST JAXBElements have to be different for each data service. This
   * issue might need to escalate to the JAXB team so that we should be able
   * to specify the name space at runtime.
   * Till then, we will have one request handler for each data service, other
   * wise resolving imports is really difficult.
   */
}
