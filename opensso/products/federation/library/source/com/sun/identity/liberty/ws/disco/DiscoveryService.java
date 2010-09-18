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
 * $Id: DiscoveryService.java,v 1.5 2008/12/05 00:18:30 exu Exp $
 *
 */


package com.sun.identity.liberty.ws.disco;

import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Collection;
import java.util.logging.Level;

import javax.xml.bind.JAXBException;
import org.w3c.dom.*;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.liberty.ws.common.LogUtil;
import com.sun.identity.liberty.ws.common.Status;
import com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken;
import com.sun.identity.liberty.ws.security.*;
import com.sun.identity.liberty.ws.soapbinding.*;
import com.sun.identity.liberty.ws.interfaces.*;
import com.sun.identity.liberty.ws.disco.plugins.*;
import com.sun.identity.liberty.ws.disco.common.*;
import com.sun.identity.liberty.ws.disco.jaxb.*;

/**
 * Handles Liberty discovery service protocol.
 */
public final class DiscoveryService implements RequestHandler {

    /**
     * Default constructor.
     */
    public DiscoveryService() {
        DiscoUtils.debug.message("In DiscoveryService constructor.");
    }
    
    /**
     * Processes request.
     * @param request in coming request <code>Message</code>
     * @return response <code>Message</code>
     * @exception Exception if an error occurred during the process.
     */
    public Message processRequest(Message request) throws Exception {
        List bodies = request.getBodies();
        bodies = Utils.convertElementToJAXB(bodies);

        if (!(bodies.size() == 1)) {
            // log it
            DiscoUtils.debug.error("DiscoService.processRequest: SOAP message"
                        + " didn't contain one SOAP body.");
            throw new Exception(DiscoUtils.bundle.getString("oneBody"));
        }

        String authnMech = request.getAuthenticationMechanism();
        if (DiscoUtils.debug.messageEnabled()) {
            DiscoUtils.debug.message("DiscoService.processRequest: "
                + "authentication mechanism =" + authnMech);
        }
        Set authnMechs =
                DiscoServiceManager.getSupportedAuthenticationMechanisms();
        if ((authnMechs == null) || (!authnMechs.contains(authnMech))) {
            DiscoUtils.debug.error("DiscoService.processRequest: Authentication"
                + "Mechanism used is not supported by this service:"+authnMech);
            throw new Exception(DiscoUtils.bundle.getString(
                                                "authnMechNotSupported"));
        }

        Message message = null;
        ProviderHeader provH = null;
        try {
            provH = new ProviderHeader(
                        DiscoServiceManager.getDiscoProviderID());
        } catch (SOAPBindingException sbe) {
            throw new DiscoveryException(sbe.getMessage());
        }
        if (DiscoServiceManager.useResponseAuthentication() ||
            (authnMech.equals(Message.NULL_X509)) ||
            (authnMech.equals(Message.NULL_SAML)) ||
            (authnMech.equals(Message.NULL_BEARER)) ||
            (authnMech.equals(Message.TLS_X509)) ||
            (authnMech.equals(Message.TLS_SAML)) ||
            (authnMech.equals(Message.TLS_BEARER)) ||
            (authnMech.equals(Message.CLIENT_TLS_X509)) ||
            (authnMech.equals(Message.CLIENT_TLS_SAML)) ||
            (authnMech.equals(Message.CLIENT_TLS_BEARER)) ||
            (authnMech.equals(Message.NULL_X509_WSF11)) ||
            (authnMech.equals(Message.NULL_SAML_WSF11)) ||
            (authnMech.equals(Message.NULL_BEARER_WSF11)) ||
            (authnMech.equals(Message.TLS_X509_WSF11)) ||
            (authnMech.equals(Message.TLS_SAML_WSF11)) ||
            (authnMech.equals(Message.TLS_BEARER_WSF11)) ||
            (authnMech.equals(Message.CLIENT_TLS_X509_WSF11)) ||
            (authnMech.equals(Message.CLIENT_TLS_SAML_WSF11)) ||
            (authnMech.equals(Message.CLIENT_TLS_BEARER_WSF11)))
        {
            try {
                SecurityTokenManager stm =
                    new SecurityTokenManager(request.getToken());
                BinarySecurityToken binaryToken = stm.getX509CertificateToken();
                binaryToken.setWSFVersion(request.getWSFVersion());
                message = new Message(provH, binaryToken);
                message.setWSFVersion(request.getWSFVersion());
            } catch (Exception e) {
                DiscoUtils.debug.error("DiscoveryService.processRequest:"
                        + "couldn't generate Message with X509 token: ", e);
                throw new DiscoveryException(e.getMessage());
            }
        } else {
            try {
                message = new Message(provH);
            } catch (Exception e) {
                DiscoUtils.debug.error("DiscoveryService.processRequest:"
                        + "couldn't generate Message: ", e);
                throw new DiscoveryException(e.getMessage());
            }
        }

        Object body = bodies.iterator().next();
        if (body instanceof QueryType) {
            message.setSOAPBody(
                lookup((QueryType) body, request));
        } else if (body instanceof ModifyType)        {
            message.setSOAPBody(
                Utils.convertJAXBToElement(update((ModifyType) body,request)));
        } else {
            DiscoUtils.debug.error("DiscoService.processRequest: SOAPBody "
                        + "is not a Disco message.");
            throw new Exception(DiscoUtils.bundle.getString("bodyNotDisco"));
        }

        // TODO:
        //create other header if needed
        //message.setOtherHeader()
        return message;
    }

    /**
     * Finds the resource offerings requested in the query.
     * @param query The incoming Discovery Query request.
     * @param message soapbinding message that contains info regarding sending
     *          identities that can be used in access control
     * @return org.w3c.dom.Element which is the QueryResponse of this operation.
     *          Inside this QueryResponse, Credentials may be included, and
     *          ResourceID may be encrypted if required.
     */
    private org.w3c.dom.Element lookup(
                com.sun.identity.liberty.ws.disco.jaxb.QueryType query,
                com.sun.identity.liberty.ws.soapbinding.Message message)
                throws JAXBException
    {
        DiscoUtils.debug.message("in lookup.");
        Status status = new Status(DiscoConstants.DISCO_NS,
                                DiscoConstants.DISCO_NSPREFIX);
        QueryResponse resp = new QueryResponse(status);

        String providerID = DiscoServiceManager.getDiscoProviderID();
        String resourceID = null;
        ResourceIDType resID = query.getResourceID();
        if (resID == null) {
            resourceID = getResourceID(query.getEncryptedResourceID(),
                                        providerID);
        } else {
            resourceID = resID.getValue();
        }
        DiscoEntryHandler entryHandler = null;
        String userDN = null;
        boolean isB2E = false;
        if(resourceID == null ||
               resourceID.equals(DiscoConstants.IMPLIED_RESOURCE)) {
           // B2E case           
           DiscoUtils.debug.message("DiscoveryService.lookup: in B2E case");
           isB2E = true;
        }

        if(!isB2E) {
           // find the disco ResourceIDMapper from config
           ResourceIDMapper idMapper = DiscoServiceManager.getResourceIDMapper(
                                                        providerID);
           if (idMapper == null) {
               idMapper = DiscoServiceManager.getDefaultResourceIDMapper();
           }
           userDN = idMapper.getUserID(providerID, resourceID, message);
           if (userDN == null) {
               DiscoUtils.debug.error("DiscoService.lookup: couldn't find the "
                       + "user associated with the resourceID:" + resourceID);
               status.setCode(DiscoConstants.QNAME_FAILED);
               Document doc = null;
               try {
                   doc = XMLUtils.newDocument();
               } catch (Exception ex) {
                   DiscoUtils.debug.error("DiscoService.lookup:", ex);
               }
               DiscoUtils.getDiscoMarshaller().marshal(resp, doc);
               return doc.getDocumentElement();
           }
           if (DiscoUtils.debug.messageEnabled()) {
               DiscoUtils.debug.message("DiscoService.lookup: userDN="
                                        + userDN);
           }
           entryHandler = DiscoServiceManager.getDiscoEntryHandler();
        } else {
           entryHandler = DiscoServiceManager.getGlobalEntryHandler();  
        }
                
        if (entryHandler == null) {
            status.setCode(DiscoConstants.QNAME_FAILED);
            DiscoUtils.debug.message(
                "DiscoService.lookup: null DiscoEntryHandler.");
            return XMLUtils.toDOMDocument(
                resp.toString(), null).getDocumentElement();
        }

        Map discoEntriesMap = entryHandler.getDiscoEntries(userDN,
                                        query.getRequestedServiceType());
        Collection results = discoEntriesMap.values();

        Map returnMap = null;
        if (results.size() == 0) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("DiscoService.lookup: lookup "
                        + "NoResults for user:" + userDN);
            }
            status.setCode(DiscoConstants.QNAME_FAILED);    
            String[] data = { userDN };
            LogUtil.error(Level.INFO,LogUtil.DS_LOOKUP_FAILURE,data);
        } else {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("DiscoService.lookup: find " +
                    results.size() + "ResourceOfferings for userDN:" + userDN);
            }

            Authorizer authorizer = null;
            if (DiscoServiceManager.needPolicyEvalLookup()) {
                DiscoUtils.debug.message("DiscoService.lookup:needPolicyEval.");
                authorizer = DiscoServiceManager.getAuthorizer();
                if (authorizer == null) {
                    status.setCode(DiscoConstants.QNAME_FAILED);    
                    String[] data = { userDN };
                    LogUtil.error(Level.INFO,LogUtil.DS_LOOKUP_FAILURE,data);
                    return XMLUtils.toDOMDocument(
                        resp.toString(), null).getDocumentElement();
                }
            }

            returnMap = DiscoUtils.checkPolicyAndHandleDirectives(userDN,
                message, results, authorizer,null,null, message.getToken());
            List offerings = (List) returnMap.get(DiscoUtils.OFFERINGS);
            if (offerings.isEmpty()) {
                if (DiscoUtils.debug.messageEnabled()) {
                    DiscoUtils.debug.message("DiscoService.lookup: after policy"
                        + " check and directive handling, NoResults for:"
                        + userDN);
                }
                status.setCode(DiscoConstants.QNAME_FAILED);    
                String[] data = { userDN };
                LogUtil.error(Level.INFO,LogUtil.DS_LOOKUP_FAILURE,data);
            } else {
                resp.setResourceOffering(offerings);
                DiscoUtils.debug.message("after resp.getresoff.addall");
                List credentials = (List) returnMap.get(DiscoUtils.CREDENTIALS);
                if ((credentials != null) && (!credentials.isEmpty())) {
                    DiscoUtils.debug.message("DiscoService.lookup: has cred.");
                    resp.setCredentials(credentials);
                }
                status.setCode(DiscoConstants.QNAME_OK);
                String[] data = { userDN };
                LogUtil.access(Level.INFO,
                        LogUtil.DS_LOOKUP_SUCCESS,data);
            }
        }

        return XMLUtils.toDOMDocument(
            resp.toString(), null).getDocumentElement();
    }


    /**
     * Updates resource offerings.
     * @param modify The incoming Discovery Update request.
     * @param message soapbinding message that contains info regarding sending
     *          identities that can be used in access control
     * @return ModifyResponseType which includes Status of the operation.
     */
    private com.sun.identity.liberty.ws.disco.jaxb.ModifyResponseElement update(
                com.sun.identity.liberty.ws.disco.jaxb.ModifyType modify,
                com.sun.identity.liberty.ws.soapbinding.Message message)
                throws JAXBException
    {
        DiscoUtils.debug.message("in update.");
        ModifyResponseElement resp = null;
        StatusType status = null;
        try {
            resp =
                DiscoUtils.getDiscoFactory().createModifyResponseElement();
            status = DiscoUtils.getDiscoFactory().createStatusType();
            resp.setStatus(status);
        } catch (JAXBException je) {
            DiscoUtils.debug.error("DiscoService.update: couldn't form "
                + "ModifyResponse.");
            throw je;
        }

        String providerID = DiscoServiceManager.getDiscoProviderID();
        String resourceID = null;
        ResourceIDType resID = modify.getResourceID();
        if (resID == null) {
            resourceID = getResourceID(modify.getEncryptedResourceID(),
                                        providerID);
        } else {
            resourceID = resID.getValue();
        }
        
        DiscoEntryHandler entryHandler = null;
        String userDN = null;
        boolean isB2E = false;
        String logMsg = null;
        if(resourceID == null ||
               resourceID.equals(DiscoConstants.IMPLIED_RESOURCE)) {
           // B2E case           
           DiscoUtils.debug.message("DiscoveryService.lookup: in B2E case");
           isB2E = true;
        }

        if(!isB2E) {
           // find the disco ResourceIDMapper from config
           ResourceIDMapper idMapper = DiscoServiceManager.getResourceIDMapper(
                                        providerID);
           if (idMapper == null) {
               idMapper = DiscoServiceManager.getDefaultResourceIDMapper();
           }
           userDN = idMapper.getUserID(providerID, resourceID, message);

           logMsg = DiscoUtils.bundle.getString("messageID") + "="
                        + message.getCorrelationHeader().getMessageID() + "."
                        + DiscoUtils.bundle.getString("providerID") + "="
                        + providerID + "."
                        + DiscoUtils.bundle.getString("securityMechID") + "="
                        + message.getAuthenticationMechanism() + "."
                        + DiscoUtils.bundle.getString("resourceOfferingID")
                        + "=" + resourceID + "."
                        + DiscoUtils.bundle.getString("operation") + "="
                        + "Update";

           if (userDN == null) {
               DiscoUtils.debug.error("DiscoService.update: couldn't find user "
                        + "from resourceID: " + resourceID);
               status.setCode(DiscoConstants.QNAME_FAILED);
               String[] data = { resourceID };
               LogUtil.error(Level.INFO,
                        LogUtil.DS_UPDATE_FAILURE,data);
               return resp;
           }

            // find the DiscoEntryHandler from config
            entryHandler = DiscoServiceManager.getDiscoEntryHandler();
        } else {
            entryHandler = DiscoServiceManager.getGlobalEntryHandler();
        }

        // get flag if policy check for modify from config
        if (DiscoServiceManager.needPolicyEvalUpdate()) {
            DiscoUtils.debug.message("DiscoService.lookup: needPolicyEval.");
            if (!isUpdateAllowed(userDN, message, modify.getRemoveEntry(),
                        modify.getInsertEntry(), entryHandler,
                        DiscoServiceManager.getAuthorizer()))
            {
                status.setCode(DiscoConstants.QNAME_FAILED);
                String[] data = { userDN };
                LogUtil.error(Level.INFO,
                        LogUtil.DS_UPDATE_FAILURE,data);
                return resp;
            }
        }

        // now do the modify
        Map results = entryHandler.modifyDiscoEntries(userDN,
                        modify.getRemoveEntry(), modify.getInsertEntry());
        String statusCode = (String) results.get(DiscoEntryHandler.STATUS_CODE);
        if (statusCode.equals(DiscoConstants.STATUS_OK)) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("DiscoService.update: modified "
                    + "DiscoEntries through DiscoEntryHandler successfully.");
            }
            status.setCode(DiscoConstants.QNAME_OK);

            List entryIds = (List) results.get(
                                        DiscoEntryHandler.NEW_ENTRY_IDS);
            if ((entryIds != null) && (entryIds.size() != 0)) {
                resp.getNewEntryIDs().addAll(entryIds);
            }
            String[] data = { logMsg };
            LogUtil.access(Level.INFO,
                        LogUtil.DS_UPDATE_SUCCESS,data);
        } else {
            DiscoUtils.debug.error("DiscoService.update: couldn't modify "
                + "DiscoEntries through DiscoEntryHandler.");
            status.setCode(DiscoConstants.QNAME_FAILED);
            String[] data = { logMsg };
            LogUtil.error(Level.INFO,
                        LogUtil.DS_UPDATE_FAILURE,data);
        }
        return resp;
    }

    private boolean isUpdateAllowed(String userDN, Message message,
                List removes, List inserts, DiscoEntryHandler entryHandler,
                Authorizer authorizer)
    {
        DiscoUtils.debug.message("DiscoService.isUpdateAllowed.");
        if (authorizer == null) {
            return false;
        }

        Map env = null;
        // policy eval for each removes
        if ((removes != null) && (removes.size() != 0)) {
            Map entryMap = entryHandler.getDiscoEntries(userDN, null);
            Iterator i = removes.iterator();
            String entryID = null;
            while (i.hasNext()) {
                entryID = ((RemoveEntryType) i.next()).getEntryID();
                if (!entryMap.containsKey(entryID)) {
                    DiscoUtils.debug.error("DiscoveryService.isUpdateAllowed: "
                        + "remove entry not exits: " + entryID);
                    return false;
                }
                if (env == null) {
                    env = new HashMap();
                    env.put(Authorizer.USER_ID, userDN);
                    env.put(Authorizer.AUTH_TYPE,
                                message.getAuthenticationMechanism());
                    env.put(Authorizer.MESSAGE, message);
                }
                if (!authorizer.isAuthorized(message.getToken(),
                                DiscoConstants.ACTION_UPDATE,
                                ((InsertEntryType) entryMap.get(entryID)).
                                                        getResourceOffering(),
                                env))
                {
                    DiscoUtils.debug.error("DiscoveryService.isUpdateAllowed: "
                        + "WSC is not authorized to remove entry: " + entryID);
                    return false;
                }
            }
        }

        // policy eval for each inserts
        if ((inserts != null) && (inserts.size() != 0)) {
            Iterator j = inserts.iterator();
            while (j.hasNext()) {
                if (env == null) {
                    env = new HashMap();
                    env.put(Authorizer.USER_ID, userDN);
                    env.put(Authorizer.AUTH_TYPE,
                                message.getAuthenticationMechanism());
                    env.put(Authorizer.MESSAGE, message);
                }
                if (!authorizer.isAuthorized(message.getToken(),
                        DiscoConstants.ACTION_UPDATE,
                        ((InsertEntryType) j.next()).getResourceOffering(),
                        env))
                {
                    DiscoUtils.debug.error("DiscoveryService.isUpdateAllowed: "
                        + "WSC is not authorized to insert entry.");
                    return false;
                }
            }
        }

        return true;
    }

    private String getResourceID(EncryptedResourceIDType encryptResID,
                                String providerID)
    {
        if ((encryptResID == null) || (providerID == null)) {
            return null;
        }
        String result = null;
        try {
            EncryptedResourceID eri = new EncryptedResourceID(
                Utils.convertJAXBToElement(encryptResID, false));
            ResourceID ri = EncryptedResourceID.getDecryptedResourceID(eri,
                                                        providerID);
            if (ri != null) {
                result = ri.getResourceID();
            }
        } catch (Exception e) {
            DiscoUtils.debug.error("DiscoveryService.getResourceID: Exception:",
                                         e);
        }
        return result;
    }
}
