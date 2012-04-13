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
 * $Id: PPRequestHandler.java,v 1.2 2008/06/25 05:47:14 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.idpp;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.liberty.ws.idpp.jaxb.QueryResponseElement;
import com.sun.identity.liberty.ws.idpp.jaxb.QueryElement;
import com.sun.identity.liberty.ws.idpp.jaxb.QueryType;
import com.sun.identity.liberty.ws.idpp.jaxb.QueryResponseType;
import com.sun.identity.liberty.ws.idpp.jaxb.ModifyElement;
import com.sun.identity.liberty.ws.idpp.jaxb.ModifyResponseElement;
import com.sun.identity.liberty.ws.idpp.jaxb.ModifyType;
import com.sun.identity.liberty.ws.idpp.jaxb.StatusType;
import com.sun.identity.liberty.ws.dst.DSTQueryItem;
import com.sun.identity.liberty.ws.dst.DSTModification;
import com.sun.identity.liberty.ws.dst.DSTConstants;
import com.sun.identity.liberty.ws.dst.DSTException;
import com.sun.identity.liberty.ws.dst.service.DSTRequestHandler;
import com.sun.identity.liberty.ws.idpp.common.*;
import com.sun.identity.liberty.ws.soapbinding.Message;
import com.sun.identity.liberty.ws.soapbinding.SOAPFaultException;
import com.sun.identity.liberty.ws.soapbinding.SOAPBindingException;
import com.sun.identity.liberty.ws.soapbinding.NamespacePrefixMapperImpl;
import com.sun.identity.liberty.ws.soapbinding.SOAPFault;
import com.sun.identity.liberty.ws.soapbinding.SOAPFaultDetail;
import com.sun.identity.liberty.ws.soapbinding.SOAPBindingConstants;
import com.sun.identity.liberty.ws.soapbinding.ServiceInstanceUpdateHeader;
import com.sun.identity.liberty.ws.soapbinding.ProviderHeader;
import com.sun.identity.liberty.ws.interfaces.Authorizer;
import com.sun.identity.liberty.ws.interfaces.ServiceInstanceUpdate;
import com.sun.identity.liberty.ws.common.LogUtil;
import com.sun.identity.saml.common.SAMLUtils;

import java.util.List;
import java.util.Set;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import javax.xml.bind.JAXBException;
import org.w3c.dom.Document;

//Interaction imports
import com.sun.identity.liberty.ws.interaction.JAXBObjectFactory;
import com.sun.identity.liberty.ws.interaction.InteractionSOAPFaultException;
import com.sun.identity.liberty.ws.interaction.InteractionManager;
import com.sun.identity.liberty.ws.interaction.InteractionUtils;
import com.sun.identity.liberty.ws.interaction.jaxb.InquiryElement;
import com.sun.identity.liberty.ws.interaction.jaxb.InquiryType.Confirm;
import com.sun.identity.liberty.ws.interaction.jaxb.TextElement;
import com.sun.identity.liberty.ws.interaction.jaxb.InteractionResponseElement;

/**
 * The class <code>PPRequestHandler</code> is used to process the
 * query or modify requests for a personal profile service.
 */ 

public class PPRequestHandler extends DSTRequestHandler {

    private PersonalProfile pp;
    private String logMsg = null;

    /**
     *Default constructor
     */
    public PPRequestHandler() {
       pp = new PersonalProfile();
    }

    /**
     * Processes query/modify request.
     * @param request query or modify object.
     * @param requestMsg Request Message.
     * @param responseMsg Response Message.
     * @return Object processed response object.
     * @exception SOAPFaultException for the interaction redirects 
     * @exception Exception for any failure.
     */
    public Object processDSTRequest(
        Object request, 
        Message requestMsg,
        Message responseMsg) 
    throws SOAPFaultException, DSTException {
       
        IDPPUtils.debug.message("PPRequestHandler:processRequest:Init");
        try {
            IDPPServiceManager serviceManager = 
                       IDPPServiceManager.getInstance();
            String providerID = serviceManager.getProviderID();
            ProviderHeader ph = new ProviderHeader(providerID);
            responseMsg.setProviderHeader(ph);

            if(serviceManager.isServiceInstanceUpdateEnabled()) {
               ServiceInstanceUpdateHeader siuHeader = 
                           getServiceInstanceUpdateHeader();
               responseMsg.setServiceInstanceUpdateHeader(siuHeader);
            }

            if(request instanceof QueryElement) {
               QueryElement query = (QueryElement)request;
               Document doc = IDPPUtils.getDocumentBuilder().newDocument();
               IDPPUtils.getMarshaller().setProperty(
                         "com.sun.xml.bind.namespacePrefixMapper",
                         new NamespacePrefixMapperImpl());
               IDPPUtils.getMarshaller().marshal(query, doc);
               return processQueryRequest(query, providerID, requestMsg, doc);
             } else if (request instanceof ModifyElement) {
               ModifyElement modify = (ModifyElement)request;
               Document doc = IDPPUtils.getDocumentBuilder().newDocument();
               IDPPUtils.getMarshaller().setProperty(
                         "com.sun.xml.bind.namespacePrefixMapper",
                         new NamespacePrefixMapperImpl());
               IDPPUtils.getMarshaller().marshal(modify, doc);
               return processModifyRequest(modify, providerID, requestMsg, doc);
             } else {
               IDPPUtils.debug.error("PPRequestHandler:processRequest:invalid" +
               " Request."); 
               throw new DSTException(
               IDPPUtils.bundle.getString("invalidRequest"));
             }
        } catch(IDPPException ie) {
             IDPPUtils.debug.error("PPRequestHandler:processRequest fail",ie);
             throw new DSTException(ie);
        } catch (JAXBException je) {
             IDPPUtils.debug.error("PPRequestHandler:processRequest fail",je);
             throw new DSTException(
             IDPPUtils.bundle.getString("jaxbFailure"));
        } catch (SOAPBindingException sbe) {
             IDPPUtils.debug.error("PPRequestHandler:processRequest fail",sbe);
             throw new DSTException(sbe);
        }

    }

    /**
     * Processes Query Request.
     * @param query JAXBQ QueryElement Object
     * @param request queryRequest.
     * @param providerID Provider ID.
     * @param requestMsg Request Message.
     * @return QueryResponseElement response.
     * @exception IDPPException for a failure in processing the request
     * @exception SOAPFaultException if the interaction is required
     */
    public QueryResponseElement 
    processQueryRequest(QueryElement query, 
                        String providerID,
                        Message requestMsg,
                        Document request)
     throws IDPPException, SOAPFaultException {

        IDPPUtils.debug.message("PPRequestHandler:processQueryRequest:Init");
        if(query == null || request == null) {
           IDPPUtils.debug.error("PPRequestHandler:processQueryRequest:" +
           "null values ");
           throw new IDPPException(
           IDPPUtils.bundle.getString("nullInputParams"));
        }
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("PPRequestHandler:processQueryRequest:" +
           "request received:" + XMLUtils.print(request.getDocumentElement()));
        }
        Object resObj = query.getResourceID();
        if(resObj == null) {
            resObj = query.getEncryptedResourceID();
        }
        QueryResponseElement response = getQueryResponse(query);
        String resourceID = getResourceID(resObj, providerID, 
                        IDPPConstants.XMLNS_IDPP); 
        if(resourceID == null || !pp.isResourceIDValid(resourceID)) {
           if(IDPPUtils.debug.messageEnabled()) {
              IDPPUtils.debug.message("PPRequestHandler:processQuery" +
              "Request: resource id is invalid.");
           }
           response.setStatus(setStatusType(false, DSTConstants.NO_RESOURCE, 
           IDPPUtils.bundle.getString("invalidResourceID"), null));
           return response;
        }
        if(LogUtil.isLogEnabled()) {
           logMsg = IDPPUtils.bundle.getString("messageID") + "=" +
                    requestMsg.getCorrelationHeader().getMessageID() + " "+
                    IDPPUtils.bundle.getString("providerID") + "=" +
                    providerID + " " + IDPPUtils.bundle.getString("resourceID")
                    + "=" + resourceID + " " + IDPPUtils.bundle.getString(
                    "securityMechID") + "=" + 
                    requestMsg.getAuthenticationMechanism() + " ";
        }
        List queryItems = query.getQueryItem();
        if (queryItems.size() == 0) {
            if(IDPPUtils.debug.warningEnabled()) {
               IDPPUtils.debug.warning("PPRequestHandler:processQuery" +
               "Request: The request does not have any query items.");
            }
            response.setStatus(setStatusType(false, 
            DSTConstants.UNEXPECTED_ERROR,  
            IDPPUtils.bundle.getString("nullQueryItems"), null));
            return response;
        }

        Map interactQueries = new HashMap();
        Map interactedData = new HashMap();

        List dstQueryItems = new ArrayList();
        int queryItemsSize = queryItems.size();
        for(int i= 0; i < queryItemsSize; i++) {
            boolean isQueryItemValid = true;
            QueryType.QueryItemType item =
                   (QueryType.QueryItemType)queryItems.get(i);
            String select = item.getSelect();
            String ref = item.getItemID();
            if(ref == null || ref.length() == 0) {
               ref = item.getId();
            }

            if((select == null) || (select.length() == 0)) {
                if(IDPPUtils.debug.warningEnabled()) {
                   IDPPUtils.debug.warning("PPRequestHandler:process"+
                   "QueryRequest: There is no Select in the request.");
                }
                response.setStatus(
                setStatusType(false, DSTConstants.MISSING_SELECT, 
                IDPPUtils.bundle.getString("missingSelect"), ref));
                isQueryItemValid = false;
           }

           if(!pp.isSelectDataSupported(select)) {
              if(IDPPUtils.debug.warningEnabled()) {
                 IDPPUtils.debug.warning("PPRequestHandler:process"+
                 "QueryRequest: Data not supported");
              }
              response.setStatus(setStatusType(false, 
              DSTConstants.INVALID_SELECT, 
              IDPPUtils.bundle.getString("invalidSelect"), ref));
              isQueryItemValid = false;
           }

           //Check for authorization & interaction.
           String resource = IDPPUtils.getResourceExpression(select);
           String authZAction = pp.getAuthZAction(requestMsg.getToken(),
              DSTConstants.QUERY_ACTION, resource, 
              getPolicyEnvMap(resourceID, requestMsg));

           if(authZAction == null || authZAction.equalsIgnoreCase(
              IDPPConstants.AUTHZ_DENY)) {
              response.setStatus(setStatusType(false, 
              DSTConstants.NOT_AUTHORIZED, 
              IDPPUtils.bundle.getString("notAuthorized"), ref));
              if(LogUtil.isLogEnabled()) {
                 String[] data = {resourceID};
                 LogUtil.error(Level.INFO,LogUtil.PP_QUERY_FAILURE,data);
              }
              continue;

           }

           if(authZAction.equalsIgnoreCase(
                 IDPPConstants.INTERACT_FOR_CONSENT)) {

                 if(isInteractionResponseExists(requestMsg)) {
                    if(!processInteractionConsentResponse(
                       true, requestMsg, select)) {
                       if(LogUtil.isLogEnabled()) {
                          String[] data = { resourceID };
                          LogUtil.error(Level.INFO, 
                                        LogUtil.PP_INTERACTION_FAILURE,data);
                       }
                       response.setStatus(setStatusType(false, 
                       DSTConstants.NOT_AUTHORIZED, 
                       IDPPUtils.bundle.getString("interactionFailed"),ref));
                       isQueryItemValid = false;
                    }
                 } else {
                    interactQueries.put(resource,  authZAction);
                 }
           } else if(authZAction.equalsIgnoreCase(
                     IDPPConstants.INTERACT_FOR_VALUE)) {

                 if(isInteractionResponseExists(requestMsg)) {
                    Map intrData = processInteractionValueResponse(true,
                         requestMsg, select);

                    if(intrData == null || intrData.isEmpty()) {
                       if(LogUtil.isLogEnabled()) {
                          //String[] data = { logMsg };
                          String[] data = { resourceID };
                          LogUtil.error(Level.INFO, 
                                        LogUtil.PP_INTERACTION_FAILURE,data);
                       }
                       response.setStatus(setStatusType(false, 
                       DSTConstants.NOT_AUTHORIZED, 
                       IDPPUtils.bundle.getString("interactionFailed"),ref));
                       isQueryItemValid = false;
                    } else {
                       interactedData.putAll(intrData);
                    }
                 } else {
                    interactQueries.put(resource,  authZAction);
                 }
           }

           if(isQueryItemValid) {
              Calendar changedSince = item.getChangedSince();
              Date date = null;
              if(changedSince != null) {
                 date = changedSince.getTime();
              }
              DSTQueryItem dstQueryItem = new DSTQueryItem(select, 
                                         item.isIncludeCommonAttributes(),
                                         date, null);
              dstQueryItem.setId(item.getId());
              dstQueryItem.setItemID(item.getItemID());
              dstQueryItems.add(dstQueryItem);
           }
        }

        //Check for interfaction before processing the actual response.
        if(interactQueries != null && !interactQueries.isEmpty()) {
           initInteraction(true, interactQueries, requestMsg);
        }

        Map queryResults = pp.queryData(requestMsg.getToken(), resourceID, 
                           dstQueryItems, interactedData, request);
        List data = getData(queryResults);
        if(data != null && !data.isEmpty()) {
           response.getData().addAll(data); 
        }
        if(LogUtil.isLogEnabled()) {
           String[] msgData = { resourceID };
           LogUtil.access(Level.INFO, LogUtil.PP_QUERY_SUCCESS,msgData);
        }
        return response;
     }

     /**
      * Get the data from the queried results
      * @param queryResults map of DSTQueryItems and the corresponding results.
      * @return List queried data.
      * @exception IDPPException.
      */
     private List getData(Map queryResults) throws IDPPException {
 
        IDPPUtils.debug.message("PPRequestHandler:getData:Init");
        List  dataResults = new ArrayList();
        Set queryItems = queryResults.keySet();
        Iterator iter = queryItems.iterator();
        while(iter.hasNext()) {
           QueryResponseType.DataType data = null;
           try {
               data = IDPPUtils.getIDPPFactory().
                       createQueryResponseTypeDataType();
            } catch (JAXBException je) {
                IDPPUtils.debug.error("PPRequestHandler:getData:jaxb fail", je);
                throw new IDPPException(
                IDPPUtils.bundle.getString("jaxbFailure"));
            }
            DSTQueryItem dstQueryItem = (DSTQueryItem)iter.next();
            List values = (List)queryResults.get(dstQueryItem);
            if(values.isEmpty()) {
               continue;
            }
            data.getAny().addAll(values);
            data.setItemIDRef(dstQueryItem.getItemID());
            data.setId(dstQueryItem.getId());
            dataResults.add(data);
        }
        return dataResults;
    }

    /**
     * sets the response status.
     * @param success This flag indicates whether the status is ok or failed. 
     * @param statusCode status code.
     * @param comment.
     * @param itemID Item ID for the reference.
     * @return StatusType JAXB Object.
     * @exception IDPPException for any failure
     */
    private StatusType setStatusType(boolean success, 
         String statusCode, String comment, String ref)
    throws IDPPException {
        IDPPUtils.debug.message("PPRequestHandler:setStatusType:Init");
        if(statusCode == null) {
           throw new IDPPException(
           IDPPUtils.bundle.getString("nullInputParams"));
        }
        try {
            StatusType status = IDPPUtils.getIDPPFactory().createStatusType();
            if(success) {
              QName qName = new QName(IDPPConstants.XMLNS_IDPP, statusCode);
              status.setCode(qName);
            } else {
              QName qName = 
                 new QName(IDPPConstants.XMLNS_IDPP, DSTConstants.FAILED);
              status.setCode(qName);

              StatusType secondStatus = 
                   IDPPUtils.getIDPPFactory().createStatusType();
              QName secondQ = new QName(IDPPConstants.XMLNS_IDPP, statusCode);
              secondStatus.setCode(secondQ);
              if(comment != null) {
                 secondStatus.setComment(comment);
              }
              if(ref != null) {
                 secondStatus.setRef(ref);
              }
              status.getStatus().add(secondStatus);
            }
            return status;
        } catch (JAXBException je) {
            IDPPUtils.debug.error("PPRequestHandler:setStatusType:" +
            "jaxb failure:" , je);
            throw new IDPPException(IDPPUtils.bundle.getString("jaxbFailure"));
        }
    }

    /**
     * Processes modify request.
     * @param modify ModifyElement JAXB Object
     * @param request modify request.
     * @param providerID Provider ID.
     * @param requestMsg Request Message.
     * @return ModifyResponseElement response JAXB Object
     * @exception IDPPException for failure in processing the request
     * @exception SOAPFaultException for interaction redirect
     */ 
    public ModifyResponseElement processModifyRequest(
         ModifyElement modify, 
         String providerID, 
         Message requestMsg, 
         Document request)
     throws IDPPException, SOAPFaultException {

        IDPPUtils.debug.message("PPRequestHandler:processModifyRequest:Init");
        if(modify == null) {
           IDPPUtils.debug.error("PPRequestHandler:processModify"+
           "Request. null values");
           throw new IDPPException(
           IDPPUtils.bundle.getString("nullInputParams"));
        }
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("PPRequestHandler:processModifyRequest:"+
           "request received:" + XMLUtils.print(request.getDocumentElement()));
        }

        Map interactedData = new HashMap();
        ModifyResponseElement response = getModifyResponse(modify);
        Object resObj = modify.getResourceID();
        if(resObj == null) {
            resObj = modify.getEncryptedResourceID();
        }
        String resourceID = getResourceID(resObj, providerID, 
                  IDPPConstants.XMLNS_IDPP);
        if(resourceID == null || !pp.isResourceIDValid(resourceID)) {
           if(IDPPUtils.debug.warningEnabled()) {
              IDPPUtils.debug.warning("PPRequestHandler:processModify" +
              "Request: resource id is invalid.");
           }
           response.setStatus(setStatusType(false, DSTConstants.NO_RESOURCE,
           IDPPUtils.bundle.getString("invalidResourceID"), null));
           return response;
        }
        if(LogUtil.isLogEnabled()) {
           logMsg = IDPPUtils.bundle.getString("messageID") + "=" +
                    requestMsg.getCorrelationHeader().getMessageID() + " "+
                    IDPPUtils.bundle.getString("providerID") + "=" +
                    providerID + " " + IDPPUtils.bundle.getString("resourceID")
                    + "=" + resourceID + " " + IDPPUtils.bundle.getString(
                    "securityMechID") + "=" + 
                    requestMsg.getAuthenticationMechanism() + " ";
        }
        List modificationElements = modify.getModification();
        if(modificationElements.size() == 0) {
           IDPPUtils.debug.error("PPRequestHandler:process" +
           "ModifyRequest: Modification elements are null");
           response.setStatus(setStatusType(false, 
           DSTConstants.UNEXPECTED_ERROR, 
           IDPPUtils.bundle.getString("nullModifications"),null)); 
           return response;
        }

        Map interactSelects = new HashMap();
        List dstModifications = new ArrayList();
        int size = modificationElements.size();
        for (int i=0; i < size; i++) {
             ModifyType.ModificationType modificationType =
             (ModifyType.ModificationType)modificationElements.get(i);

             String select = modificationType.getSelect();
             String ref = modificationType.getId();

             if(select == null || select.length() == 0) {
                if(IDPPUtils.debug.warningEnabled()) {
                   IDPPUtils.debug.warning("PersonalProfileService:process"+
                   "ModifyRequest: select is null");
                }
                response.setStatus(setStatusType(false, 
                DSTConstants.MISSING_SELECT, 
                IDPPUtils.bundle.getString("missingSelect"), ref));
                return response;
             }
             if(!pp.isSelectDataSupported(select)){
                if(IDPPUtils.debug.warningEnabled()) {
                   IDPPUtils.debug.warning("PersonalProfileService:process"+
                   "ModifyRequest: Data not supported");
                }
                response.setStatus(setStatusType(false,
                DSTConstants.INVALID_SELECT, 
                IDPPUtils.bundle.getString("invalidSelect"), ref));
                return response;
             }
             //Check for authorization & interaction.
             String resource = IDPPUtils.getResourceExpression(select);
             String authZAction = pp.getAuthZAction(requestMsg.getToken(),
                    DSTConstants.MODIFY_ACTION, resource, 
                    getPolicyEnvMap(resourceID, requestMsg));

             if(authZAction == null || authZAction.equalsIgnoreCase(
                IDPPConstants.AUTHZ_DENY)) {
                response.setStatus(setStatusType(false, 
                DSTConstants.NOT_AUTHORIZED,
                IDPPUtils.bundle.getString("notAuthorized"), ref));
                if(LogUtil.isLogEnabled()) {
                   String[] data = { resourceID };
                   LogUtil.error(Level.INFO,LogUtil.PP_MODIFY_FAILURE,data);
                }
                return response;
             }

             if(IDPPUtils.debug.messageEnabled()) {
                IDPPUtils.debug.message("PPRequestHandler.processModifyRequest:"
                + " Authorization action" + authZAction);
             }

             if(authZAction.equalsIgnoreCase(
                IDPPConstants.INTERACT_FOR_CONSENT)) {

                 if(isInteractionResponseExists(requestMsg)) {
                    if(!processInteractionConsentResponse(
                       false, requestMsg, select)) {
                       if(LogUtil.isLogEnabled()) {
                          String[] data = { resourceID };
                          LogUtil.error(Level.INFO, 
                                        LogUtil.PP_INTERACTION_FAILURE,data);
                       }
                       response.setStatus(setStatusType(false, 
                       DSTConstants.NOT_AUTHORIZED,
                       IDPPUtils.bundle.getString("interactionFailed"), ref));
                       return response;
                    }
                 } else {
                    interactSelects.put(resource,  authZAction);
                 }

             } else if (authZAction.equalsIgnoreCase(
                     IDPPConstants.INTERACT_FOR_VALUE)) {

                 if(isInteractionResponseExists(requestMsg)) {

                    Map intrData = processInteractionValueResponse(
                       false, requestMsg, select);

                    if(intrData == null || intrData.isEmpty()) {
                       if(LogUtil.isLogEnabled()) {
                          String[] data = { resourceID };
                          LogUtil.error(Level.INFO, 
                                        LogUtil.PP_INTERACTION_FAILURE,data);
                       }
                       response.setStatus(setStatusType(false, 
                       DSTConstants.NOT_AUTHORIZED,
                       IDPPUtils.bundle.getString("interactionFailed"), ref));
                       return response;
                    } else {
                       interactedData.putAll(intrData);
                    }
                 } else {
                    interactSelects.put(resource,  authZAction);
                 }
             }

             boolean override = modificationType.isOverrideAllowed();
             ModifyType.ModificationType.NewDataType newData =
                        modificationType.getNewData();
             DSTModification dstModification = new DSTModification();
             dstModification.setSelect(select);
             dstModification.setOverrideAllowed(override);
             dstModification.setId(modificationType.getId());
             dstModifications.add(dstModification);
             if(newData != null && newData.getAny() != null && 
                newData.getAny().size() != 0) {
                dstModification.setNewDataValue(newData.getAny());
             }
        }

        //Check for interfaction before processing the actual response.
        if(interactSelects != null && !interactSelects.isEmpty()) {
           initInteraction(false, interactSelects, requestMsg);
        }

        if(pp.modifyData(requestMsg.getToken(),resourceID, 
           dstModifications, interactedData, request)) {
           if(LogUtil.isLogEnabled()) {
              String[] data = { resourceID };
              LogUtil.access(Level.INFO, LogUtil.PP_MODIFY_SUCCESS,data);
           }
           return response;
        } else {
           response.setStatus(setStatusType(false, 
           DSTConstants.UNEXPECTED_ERROR, 
           IDPPUtils.bundle.getString("modifyFailed"), null));

           if(LogUtil.isLogEnabled()) {
                String[] data = { logMsg };
                LogUtil.error(Level.INFO,LogUtil.PP_MODIFY_FAILURE,data);
           }
           return response;
        }
    }

    /**
     * Returns Policy Environment map
     * @param resourceID resource id
     * @param requestMsg Request Message
     * @return Map Policy Environment map
     */
    private Map getPolicyEnvMap(String resourceID, Message requestMsg) {
       Map env = new HashMap();
       env.put(Authorizer.MESSAGE, requestMsg);
       env.put(Authorizer.USER_ID, pp.getUserDN(resourceID));
       env.put(Authorizer.AUTH_TYPE, requestMsg.getAuthenticationMechanism());
       return env;
    }

    /**
     * Gets the query response and set the status to OK.
     * @param query JAXB query object.
     * @return QueryResponseElement JAXB query response.
     * @exception IDPPException
     */
    public QueryResponseElement getQueryResponse(QueryElement query)
    throws IDPPException {
        if(query == null) {
           IDPPUtils.debug.error("PPRequestHandler:getQueryResponse:" +
           "Query is null.");
           throw new IDPPException(
           IDPPUtils.bundle.getString("nullInputParams"));
        }
        try {
            QueryResponseElement response =
                   IDPPUtils.getIDPPFactory().createQueryResponseElement();
            response.setStatus(setStatusType(true, DSTConstants.OK, null,null));
            response.setId(SAMLUtils.generateID());
            response.setItemIDRef(query.getItemID());
            return response;
        } catch (JAXBException je) {
            IDPPUtils.debug.error("PPRequestHandler:getQueryResponse:" +
            "JAXB failure.", je);
            throw new IDPPException(IDPPUtils.bundle.getString("jaxbFailure"));
        }

    }

    /**
     * Gets the modify response and set the status to OK.
     * @param modify JAXB modify object.
     * @return ModifyResponseElement JAXB modify response.
     * @exception IDPPException
     */
    public ModifyResponseElement getModifyResponse(ModifyElement modify)
    throws IDPPException {
        if(modify == null) { 
           IDPPUtils.debug.error("PPRequestHandler:getModifyResponse:" +
           "Modify is null.");
           throw new IDPPException(
           IDPPUtils.bundle.getString("nullInputParams"));
        }
        try {
            ModifyResponseElement response =
                   IDPPUtils.getIDPPFactory().createModifyResponseElement();
            response.setStatus(setStatusType(true, DSTConstants.OK, null,null));
            response.setId(SAMLUtils.generateID());
            response.setItemIDRef(modify.getItemID());
            return response;
        } catch (JAXBException je) {
            IDPPUtils.debug.error("PPRequestHandler:getModifyResponse:" +
            "JAXB failure.", je);
            throw new IDPPException(IDPPUtils.bundle.getString("jaxbFailure"));
        }

    }

    /**
     * Checks if the interaction response exists for this message
     * @param msg SOAP Request Message
     * @return true if exists
     */
    private boolean isInteractionResponseExists(Message msg) {
        try { 
            return (InteractionManager.getInstance().
               getInteractionResponseElement(msg) != null);
        } catch (Exception e) {
            IDPPUtils.debug.error("PPRequestHandler.isInteractionResponse" +
            "Exists: Exception while getting interaction response.", e);
            return false;
        }
    }

    /**
     * Initialize interaction for the queries that require interaction
     * @param isQuery true if this is a <code>PP</code> query request, 
     *                false if this is a <code>PP</code> modify request.
     * @param interactResourceMap map of resources that need an interaction
     *                            This map will have key as the resource
     *                            and the value as interaction type. 
     * @param msg SOAP request message
     * @exception SOAPFaultException for interaction redirection
     * @exception IDPPException for any other failure.
     */
    private void initInteraction(boolean isQuery, Map interactResourceMap, 
                                 Message msg)
     throws SOAPFaultException, IDPPException {

        IDPPUtils.debug.message("PPRequestHandler.initInteraction:Init");
        if(msg == null || interactResourceMap == null ||
                  interactResourceMap.isEmpty()) {
           IDPPUtils.debug.error("PPRequestHandler.initInteraction:Null"+
           "Input parameters");           
           throw new IDPPException(
           IDPPUtils.bundle.getString("nullInputParams"));
        } 
         
        try {
            //Create Interaction inquiry element
            InquiryElement inquiry =
              JAXBObjectFactory.getObjectFactory().createInquiryElement();
            inquiry.setTitle(IDPPUtils.bundle.getString(
            IDPPConstants.INTERACTION_TITLE));

            List selectElements = inquiry.getSelectOrConfirmOrText();
            Set inquirySelects = interactResourceMap.keySet();
            Iterator iter = inquirySelects.iterator();
            while(iter.hasNext()) {
               String resource = (String)iter.next();
               String interactionType = 
                      (String)interactResourceMap.get(resource);
               if(interactionType.equals(IDPPConstants.INTERACT_FOR_CONSENT)) {
                  selectElements.add(
                      getInteractConfirmElement(isQuery, resource, msg));
               } else if(interactionType.equals(
                  IDPPConstants.INTERACT_FOR_VALUE)) {
                  selectElements.addAll(
                      getInteractTextElements(isQuery, resource, msg));
               }
            }
            String lang = getLanguage(msg);
            if(LogUtil.isLogEnabled()) {
               String[] data = { logMsg };
               LogUtil.access(Level.INFO, LogUtil.PP_INTERACTION_SUCCESS,data);
            }
            InteractionManager.getInstance().handleInteraction(msg, 
                 inquiry, lang);
       } catch (InteractionSOAPFaultException ise) {
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("PPRequestHandler.initInteraction:"+
               "Interact redirection happened");
            }
            throw ise.getSOAPFaultException();
       } catch(Exception ex) {
            IDPPUtils.debug.error("PPRequestHandler.initInteraction:Failed");
            throw new IDPPException(ex);
       }

    }

    /**
     * Returns interaction confirm element
     * @param isQuery true if this is a <code>PP</code> query request, 
     *                false if this is a <code>PP</code> modify request.
     * @param resource resource that needs an interaction
     * @param msg Message.
     * @return Confirm Interaction JAXB Confirm Element
     * @exception IDPPException
     */
    private Confirm getInteractConfirmElement(
         boolean isQuery, String resource, Message msg) 
    throws IDPPException {

       if(resource == null) {
          IDPPUtils.debug.error("PPRequestHandler.getInteractConfirmElement:"+
          "Null input:");
          throw new IDPPException(
          IDPPUtils.bundle.getString("nullInputParams"));
       }
       resource = IDPPUtils.getExpressionContext(resource);
       if(IDPPUtils.debug.messageEnabled()) {
          IDPPUtils.debug.message("PPRequestHandler.getInteractConfirm:" +
          "Resource Context:" + resource);
       }
       
       try {
           Confirm confirmElement =
                JAXBObjectFactory.getObjectFactory().createInquiryTypeConfirm();
           PPInteractionHelper helper = 
                new PPInteractionHelper(getLanguage(msg));
           confirmElement.setName(resource);
           confirmElement.setLabel(
           helper.getInteractForConsentQuestion(isQuery, resource));
           confirmElement.setHint(
           helper.getInteractForConsentQuestion(isQuery, resource));
           return confirmElement;

       } catch(Exception e) {
           IDPPUtils.debug.error("PPRequestHandler.getInteractConfirm:"+
           "Exception while creating interact select.", e);
            throw new IDPPException(e);
       }
    }

    /**
     * Returns interaction text elements.
     * @param isQuery true if this is a <code>PP</code> query request, 
     *                false if this is a <code>PP</code> modify request.
     * @param resource resource that needs an interaction
     * @param msg Message.
     * @return List list of Interaction JAXB TextElements.
     * @exception IDPPException
     */

    private List getInteractTextElements(
        boolean isQuery, String resource, Message msg)
    throws IDPPException {

       if(resource == null) {
          IDPPUtils.debug.error("PPRequestHandler.getInteractText:"+
          "Null input:");
          throw new IDPPException(
          IDPPUtils.bundle.getString("nullInputParams"));
       }
       resource = IDPPUtils.getExpressionContext(resource);
       if(IDPPUtils.debug.messageEnabled()) {
          IDPPUtils.debug.message("PPRequestHandler.getInteractText:" +
          "Resource Context:" + resource);
       }

       try {
           List textElements = new ArrayList();
           PPInteractionHelper helper = 
                new PPInteractionHelper(getLanguage(msg));
           Map interactQueries = 
               helper.getInteractForValueQuestions(isQuery, resource);
           Iterator iter = interactQueries.keySet().iterator();
           while(iter.hasNext()) {
              String resourceKey = (String)iter.next();
              TextElement textElement =
                    JAXBObjectFactory.getObjectFactory().createTextElement();
              textElement.setName(resourceKey);
              textElement.setLabel((String)interactQueries.get(resourceKey));
              textElement.setMinChars(helper.getTextMinChars(resourceKey));
              textElement.setMaxChars(helper.getTextMaxChars(resourceKey));
              textElements.add(textElement);
            }
            return textElements;
        } catch(Exception e) {
            IDPPUtils.debug.error("PPRequestHandler.getInteractText:"+
            "Exception while creating interact text element.", e);
            throw new IDPPException(e);
        }
    }

    /**
     * Process the interaction response
     * @param isQuery true if this is a <code>PP</code> query request,
     *                  false if this is a <code>PP</code> modify request.
     * @param msg SOAP Request Message
     * @param resource interaction resource
     * @return true if the consent is allowed.
     */
    private boolean processInteractionConsentResponse(
        boolean isQuery,
        Message msg,
        String resource) {
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("PPRequestHandler.processInteraction" +
           "Response:Init");
        }

        if(msg == null || resource == null) {
           IDPPUtils.debug.error("PPRequestHandler:processInteraction" +
           "Response:null input params");
           return false;
        }

        resource = IDPPUtils.getExpressionContext(resource);
        if(IDPPUtils.debug.messageEnabled()) {
           IDPPUtils.debug.message("PPRequestHandler:processInteraction" +
           "PP Element that was trying to access:" + resource);
        }

        try {
            InteractionResponseElement ire = InteractionManager.getInstance().
                getInteractionResponseElement(msg);

            if(ire == null) {
               return false;
            }

            Map responses = InteractionUtils.getParameters(ire); 
            if(responses == null || responses.isEmpty()) {
               return false;
            }

            String value = (String)responses.get(resource);
            if(value == null || value.equals("false")) {
               if(IDPPUtils.debug.messageEnabled()) {
                  IDPPUtils.debug.message("PPRequestHandler.process" +
                  "Interaction: response is deny");
               }
               return false;
            } else {
               return true;
            }

        } catch (Exception e) {
            IDPPUtils.debug.error("PPRequestHandler.processInteraction" +
            "Response: Exception occured.", e);
            return false;
        }

    }



    /**
     * Process the interaction response for values.
     * @param isQuery true if this is a <code>PP</code> query request,
     *                false if this is a <code>PP</code> modify request.
     * @param msg SOAP Request Message
     * @param resource interaction resource
     * @return Map map of interacted data. 
     */
    private Map processInteractionValueResponse(
            boolean isQuery,
            Message msg,
            String resource) {

       if(IDPPUtils.debug.messageEnabled()) {
          IDPPUtils.debug.message("PPRequestHandler.processInteraction" +
          "Response:Init");
       }

       if(msg == null || resource == null) {
          IDPPUtils.debug.error("PPRequestHandler:processInteraction" +
          "Response:null input params");
          return null;
       }
       
       //Get the actual element
       resource = IDPPUtils.getExpressionContext(resource);
       if(IDPPUtils.debug.messageEnabled()) {
          IDPPUtils.debug.message("PPRequestHandler:processInteraction" +
          "PP Element that was trying to access:" + resource);
       }

       try {
           Map interactedData = new HashMap();
           InteractionResponseElement ire = InteractionManager.getInstance().
                       getInteractionResponseElement(msg);

           if(ire == null) {
              return null;
           }
           Map responses = InteractionUtils.getParameters(ire); 
           if(responses == null || responses.isEmpty()) {
              return null;
           }
           String lang = getLanguage(msg);
          
           PPInteractionHelper interactionHelper = 
                               new PPInteractionHelper(lang);

           Map queries = interactionHelper.getInteractForValueQuestions(
                            isQuery, resource);

           Iterator iter = queries.keySet().iterator();
           while(iter.hasNext()) {
              String query = (String)iter.next();
              String value = (String)responses.get(query);
              if(value == null || value.length() == 0) {
                 continue;
              }
              interactedData.put(
                 interactionHelper.getPPAttribute(query), value);
           }
           return interactedData;
         
       } catch (Exception e) {
           IDPPUtils.debug.error("PPRequestHandler.processInteraction" +
           "Response: Exception occured.", e);
           return null;
       }
 
    } 

    /**
     * Gets the language from the locale header.
     * @param requestMsg RequestMsg.
     * @return String Language string from the message.
     */
    private String getLanguage(Message requestMsg) {
        List langHeaders = InteractionUtils.getInteractionLangauge(requestMsg);
        if(langHeaders == null || langHeaders.isEmpty()) {
           if(IDPPUtils.debug.messageEnabled()) {
              IDPPUtils.debug.message("PPRequestHandler.setLanguage: Language" +
              "Headers are empty in the interaction message.");
           }
           return null;
        }

        String lang = (String)langHeaders.iterator().next();
        if(IDPPUtils.debug.messageEnabled()) {
          IDPPUtils.debug.message("PPRequestHandler.setLanguage:Lang:"+ lang);
        }
        return lang;
    }

    /**
     * Gets the service instance update header.
     * 
     * @exception SOAPFaultException.
     */
    private ServiceInstanceUpdateHeader getServiceInstanceUpdateHeader()
      throws SOAPFaultException {

        ServiceInstanceUpdate siu =
                 IDPPServiceManager.getInstance().getServiceInstanceUpdate();
        ServiceInstanceUpdateHeader siuHeader = 
                 siu.getServiceInstanceUpdateHeader();

        if(siu.isSOAPFaultNeeded()) {
           QName faultCodeServer =
               new QName(SOAPBindingConstants.NS_SOAP, "Server");
           SOAPFaultDetail detail = new SOAPFaultDetail(
               SOAPFaultDetail.ENDPOINT_MOVED, null, null);
           SOAPFault sf = new SOAPFault(faultCodeServer, 
                 IDPPUtils.bundle.getString("endPointMoved"), null, 
                 detail);
           Message sfmsg = new Message(sf);
           sfmsg.setServiceInstanceUpdateHeader(siuHeader);
           SOAPFaultException sfe = new SOAPFaultException(sfmsg);
           throw sfe; 
        }

        return siuHeader;
    }

}

